/*
 * Copyright 2026 Hushfeed contributors
 * https://github.com/SysAdminDoc/hushfeed
 *
 * Built on icysymmetra/tiktok-patches-for-morphe (GPL-3.0).
 */
package app.morphe.extension.tiktok.feedfilter;

import com.ss.android.ugc.aweme.feed.model.Aweme;
import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import app.morphe.extension.tiktok.blockauthor.Reflect;
import app.morphe.extension.shared.Utils;
import ai.onnxruntime.OrtException;

/**
 * Bounded asynchronous local semantic filtering. Feed callers can only consume a completed cached
 * result; initialization, tokenization, model loading and inference stay on its single worker.
 */
public final class LocalContentInference {
    private static final int MAX_TEXT_CHARS = 2048;
    private static final int MAX_CACHE_ENTRIES = 128;
    private static final int MAX_QUEUE = 32;
    private static volatile ProviderBinding provider;
    private static volatile boolean enabled;
    private static volatile boolean prepareSelectedModel;
    private static volatile Status status = Status.OFF;
    private static volatile String failureReason = "";
    private static final Object PROVIDER_LOCK = new Object();
    private static boolean initializationQueued;
    private static ProviderBinding closeWhenIdle;
    private static final ResultCache CACHE = new ResultCache();
    private static final CurrentRequests CURRENT_REQUESTS = new CurrentRequests();
    private static final AtomicLong CONFIGURATION_GENERATION = new AtomicLong();
    private static final ConcurrentHashMap<String, Object> PENDING = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<WeakReference<StatusListener>> STATUS_LISTENERS =
            new CopyOnWriteArrayList<>();
    private static String currentRulesVersion;
    private static int currentThresholdBits;
    private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(MAX_QUEUE), task -> {
        Thread thread = new Thread(task, "StopSlop-local-inference");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    }, new ThreadPoolExecutor.AbortPolicy());

    private LocalContentInference() { }

    /** Only fields confirmed on the supported Aweme stub are read; media is never opened. */
    public static Features extract(Aweme item) {
        if (item == null) return null;
        try {
            String id = item.getAid();
            String text = Reflect.string(item, "getDesc", "desc");
            if (id == null || id.isEmpty() || text == null || text.trim().isEmpty()) return null;
            if (text.length() > MAX_TEXT_CHARS) text = text.substring(0, MAX_TEXT_CHARS);
            String fingerprint = digest(text);
            return fingerprint == null ? null : new Features(id, text, fingerprint);
        } catch (RuntimeException | LinkageError unavailableMetadata) {
            return null;
        }
    }

    public static void setEnabled(boolean value) {
        if (enabled == value) {
            if (value && provider == null && status != Status.UNAVAILABLE
                    && status != Status.NOT_INSTALLED) startProviderInitialization();
            return;
        }
        enabled = value;
        CONFIGURATION_GENERATION.incrementAndGet();
        CACHE.clear();
        CURRENT_REQUESTS.clear();
        PENDING.clear();
        if (value) {
            prepareSelectedModel = false;
            failureReason = "";
            if (isReady()) setStatus(Status.READY);
            else {
                setStatus(Status.LOADING);
                startProviderInitialization();
            }
        } else {
            prepareSelectedModel = false;
            ProviderBinding previous = provider;
            provider = null;
            setStatus(Status.OFF);
            if (previous != null) closeOnWorker(previous);
        }
    }

    /** Model replacement invalidates old results and closes the old session on the worker. */
    public static void onModelChanged() {
        CONFIGURATION_GENERATION.incrementAndGet();
        CACHE.clear();
        CURRENT_REQUESTS.clear();
        PENDING.clear();
        ProviderBinding previous = provider;
        provider = null;
        if (previous != null) closeOnWorker(previous);
        if (!enabled) {
            prepareSelectedModel = false;
            failureReason = "";
            setStatus(StopSlopModelManager.activeKey(Utils.getContext()).isEmpty()
                    ? Status.NOT_INSTALLED : Status.OFF);
            return;
        }
        failureReason = "";
        setStatus(Status.LOADING);
        startProviderInitialization();
    }

    public static boolean isReady() {
        ProviderBinding current = provider;
        return current != null && current.provider.isReady();
    }

    /** Initializes an explicitly selected model without enabling semantic filtering. */
    public static void prepareSelectedModel() {
        if (enabled) return;
        Context context = Utils.getContext();
        if (StopSlopModelManager.activeKey(context).isEmpty()) {
            failureReason = "No model is selected";
            setStatus(Status.NOT_INSTALLED);
            return;
        }
        prepareSelectedModel = true;
        failureReason = "";
        if (isReady()) {
            setStatus(Status.READY);
            return;
        }
        CONFIGURATION_GENERATION.incrementAndGet();
        setStatus(Status.LOADING);
        startProviderInitialization();
    }

    public static Status getStatus() { return status; }
    public static String getFailureReason() { return failureReason; }

    /** Explicit retry exposed by the model manager; a failed provider is never retried per item. */
    public static void retryInitialization() {
        failureReason = "";
        if (!enabled) {
            prepareSelectedModel();
            return;
        }
        CONFIGURATION_GENERATION.incrementAndGet();
        setStatus(Status.LOADING);
        startProviderInitialization();
    }

    public static void addStatusListener(StatusListener listener) {
        if (listener != null) STATUS_LISTENERS.add(new WeakReference<>(listener));
    }

    /** Invalidates old semantic jobs/results when the configured topics or threshold change. */
    public static void updateTopicConfiguration(String rulesVersion, float threshold) {
        String version = rulesVersion == null ? "" : rulesVersion;
        int thresholdBits = Float.floatToIntBits(threshold);
        synchronized (PROVIDER_LOCK) {
            if (version.equals(currentRulesVersion) && thresholdBits == currentThresholdBits) return;
            currentRulesVersion = version;
            currentThresholdBits = thresholdBits;
        }
        CONFIGURATION_GENERATION.incrementAndGet();
        CACHE.clear();
        CURRENT_REQUESTS.clear();
        PENDING.clear();
    }

    private static void startProviderInitialization() {
        synchronized (PROVIDER_LOCK) {
            if ((!enabled && !prepareSelectedModel) || provider != null || initializationQueued
                    || status == Status.NOT_INSTALLED) return;
            initializationQueued = true;
        }
        long generation = CONFIGURATION_GENERATION.get();
        try {
            WORKER.execute(() -> {
                OnnxE5SemanticProvider candidate = null;
                try {
                    Context context = Utils.getContext();
                    StopSlopModelManager.Model model = context == null
                            ? null : StopSlopModelManager.active(context);
                    if (model == null) {
                        if ((enabled || prepareSelectedModel)
                                && generation == CONFIGURATION_GENERATION.get()) {
                            boolean noSelection = StopSlopModelManager.activeKey(context).isEmpty();
                            failureReason = noSelection ? "No model is selected" : "Selected model files are missing or invalid";
                            setStatus(noSelection ? Status.NOT_INSTALLED : Status.UNAVAILABLE);
                        }
                        return;
                    }
                    candidate = new OnnxE5SemanticProvider(model);
                    candidate.initialize();
                    if (!candidate.isReady()) throw new IOException("The ONNX sessions are not ready");
                    ProviderBinding binding = new ProviderBinding(candidate,
                            candidate.modelVersion());
                    if ((enabled || prepareSelectedModel)
                            && generation == CONFIGURATION_GENERATION.get()) {
                        provider = binding;
                        failureReason = "";
                        setStatus(Status.READY);
                    } else {
                        candidate.close();
                    }
                } catch (Exception | LinkageError unavailable) {
                    if (candidate != null) try { candidate.close(); } catch (Exception ignored) { }
                    if ((enabled || prepareSelectedModel)
                            && generation == CONFIGURATION_GENERATION.get()) {
                        failureReason = failureCategory(unavailable);
                        setStatus(Status.UNAVAILABLE);
                    }
                } catch (OutOfMemoryError lowMemory) {
                    if (candidate != null) try { candidate.close(); } catch (Exception | LinkageError ignored) { }
                    if ((enabled || prepareSelectedModel)
                            && generation == CONFIGURATION_GENERATION.get()) {
                        failureReason = "Low memory while creating ONNX sessions";
                        setStatus(Status.UNAVAILABLE);
                    }
                } finally {
                    synchronized (PROVIDER_LOCK) { initializationQueued = false; }
                    closeIfDisabled();
                    if ((enabled || prepareSelectedModel) && provider == null
                            && generation != CONFIGURATION_GENERATION.get()) {
                        setStatus(StopSlopModelManager.hasActiveModel(Utils.getContext())
                                ? Status.LOADING : Status.NOT_INSTALLED);
                        startProviderInitialization();
                    }
                }
            });
        } catch (RuntimeException queueFull) {
            synchronized (PROVIDER_LOCK) { initializationQueued = false; }
            failureReason = "Inference worker queue is busy";
            setStatus(Status.UNAVAILABLE);
        }
    }

    private static void closeOnWorker(ProviderBinding binding) {
        try {
            WORKER.execute(() -> {
                try { binding.provider.close(); } catch (Exception ignored) { }
                closeIfDisabled();
            });
        } catch (RuntimeException queueFull) {
            synchronized (PROVIDER_LOCK) { closeWhenIdle = binding; }
        }
    }

    private static void closeIfDisabled() {
        ProviderBinding pendingClose = closeWhenIdle;
        if (pendingClose == null) return;
        synchronized (PROVIDER_LOCK) {
            pendingClose = closeWhenIdle;
            closeWhenIdle = null;
        }
        if (pendingClose != null) {
            try { pendingClose.provider.close(); } catch (Exception ignored) { }
        }
    }

    /** Deleting an inactive package is serialized behind inference and session closure. */
    public static boolean deleteModelDirectoryWhenIdle(File directory) {
        if (directory == null) return false;
        try {
            WORKER.execute(() -> {
                closeIfDisabled();
                deleteTree(directory);
            });
            return true;
        } catch (RuntimeException queueFull) {
            return false;
        }
    }

    private static boolean deleteTree(File file) {
        if (file == null || !file.exists()) return true;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) if (!deleteTree(child)) return false;
        return file.delete();
    }

    private static void setStatus(Status value) {
        if (status == value) return;
        status = value;
        for (WeakReference<StatusListener> reference : STATUS_LISTENERS) {
            StatusListener listener = reference.get();
            if (listener == null) STATUS_LISTENERS.remove(reference);
            else {
                try { listener.onStatusChanged(value); } catch (RuntimeException ignored) { }
            }
        }
    }

    /** Returns immediately; a miss is queued and always fails open for this feed evaluation. */
    public static FilterDecision request(Features features, List<String> topics,
            String rulesVersion, float threshold, CompletionListener completion) {
        if (features == null || topics == null || topics.isEmpty() || completion == null || !enabled) {
            return FilterDecision.unknown();
        }
        List<String> topicSnapshot = boundedTopics(topics);
        if (topicSnapshot.isEmpty() || Float.isNaN(threshold) || Float.isInfinite(threshold)
                || threshold < 0.50f || threshold > 0.98f) return FilterDecision.unknown();
        ProviderBinding current = provider;
        String selectedModelVersion = current == null
                ? StopSlopModelManager.activeKey(Utils.getContext()) : current.modelVersion;
        final String modelVersion = selectedModelVersion == null || selectedModelVersion.isEmpty()
                ? "no-model" : selectedModelVersion;
        long generation = CONFIGURATION_GENERATION.get();
        String key = features.videoId + ':' + features.inputFingerprint + ':'
                + (rulesVersion == null ? "" : rulesVersion) + ':'
                + Float.floatToIntBits(threshold) + ':' + modelVersion;
        CURRENT_REQUESTS.remember(features.videoId, key);
        ClassificationResult cached = CACHE.get(key);
        if (cached != null && current != null && enabled && provider == current
                && generation == CONFIGURATION_GENERATION.get()
                && CURRENT_REQUESTS.isCurrent(features.videoId, key)) {
            return FilterDecision.from(cached, threshold);
        }
        if (current == null) startProviderInitialization();
        Object requestToken = new Object();
        if (PENDING.putIfAbsent(key, requestToken) != null) return FilterDecision.unknown();
        try {
            WORKER.execute(() -> {
                ProviderBinding active = provider;
                try {
                    if (!isCurrent(generation, active, features.videoId, key)
                            || !active.provider.isReady()) return;
                    ClassificationResult result = active.provider.classify(features, topicSnapshot);
                    if (!isCurrent(generation, active, features.videoId, key)
                            || result == null || !result.isUsable(modelVersion)) return;
                    CACHE.put(key, result);
                    if (isCurrent(generation, active, features.videoId, key)) {
                        completion.onComplete(new CompletedDecision(features.videoId,
                                features.inputFingerprint, FilterDecision.from(result, threshold)));
                    }
                } catch (Exception | LinkageError unavailable) {
                    markUnavailable(active, failureCategory(unavailable));
                } catch (OutOfMemoryError lowMemory) {
                    markUnavailable(active, "Low memory during semantic inference");
                } finally {
                    removePending(key, requestToken);
                    closeIfDisabled();
                }
            });
        } catch (RuntimeException queueFull) {
            removePending(key, requestToken);
        }
        return FilterDecision.unknown();
    }

    private static boolean isCurrent(long generation, ProviderBinding binding,
            String videoId, String requestKey) {
        return enabled && binding != null && provider == binding
                && CONFIGURATION_GENERATION.get() == generation
                && CURRENT_REQUESTS.isCurrent(videoId, requestKey);
    }

    private static void markUnavailable(ProviderBinding failed, String reason) {
        if (failed == null || provider != failed) return;
        provider = null;
        CONFIGURATION_GENERATION.incrementAndGet();
        CACHE.clear();
        CURRENT_REQUESTS.clear();
        PENDING.clear();
        failureReason = reason;
        setStatus(enabled ? Status.UNAVAILABLE : Status.OFF);
        closeOnWorker(failed);
    }

    private static String failureCategory(Throwable failure) {
        if (failure instanceof LinkageError) return "Native ONNX runtime or ABI could not load";
        if (failure instanceof IOException) return "Model files or tensor contract are invalid";
        if (failure instanceof OrtException) return "ONNX Runtime could not execute the selected model";
        if (failure instanceof SecurityException) return "Android denied access to the selected model files";
        String name = failure == null ? "" : failure.getClass().getSimpleName();
        if (name.isEmpty()) return "ONNX initialization failed";
        return name.length() > 48 ? name.substring(0, 48) : name;
    }

    private static List<String> boundedTopics(List<String> topics) {
        List<String> bounded = new ArrayList<>(Math.min(16, topics.size()));
        for (String topic : topics) {
            if (topic == null) continue;
            String value = topic.trim();
            if (value.isEmpty() || value.length() > 128 || bounded.contains(value)) continue;
            bounded.add(value);
            if (bounded.size() == 16) break;
        }
        return Collections.unmodifiableList(bounded);
    }

    public interface InferenceProvider {
        boolean isReady();
        ClassificationResult classify(Features features, List<String> topics) throws Exception;
        default void close() throws Exception { }
    }

    private static final class ProviderBinding {
        final InferenceProvider provider;
        final String modelVersion;
        ProviderBinding(InferenceProvider provider, String modelVersion) {
            this.provider = provider;
            this.modelVersion = modelVersion;
        }
    }

    public interface CompletionListener { void onComplete(CompletedDecision result); }
    public interface StatusListener { void onStatusChanged(Status status); }

    public static final class Features {
        public final String videoId;
        /** Bounded in-memory caption text passed to the provider; never persisted by this class. */
        public final String text;
        public final String inputFingerprint;
        private Features(String id, String text, String fingerprint) {
            this.videoId = id; this.text = text; this.inputFingerprint = fingerprint;
        }
    }

    public static final class ClassificationResult {
        /** Maximum cosine similarity between the caption and configured topic embeddings. */
        public final float similarity;
        public final String modelVersion;
        public ClassificationResult(float similarity, String modelVersion) {
            this.similarity = similarity; this.modelVersion = modelVersion;
        }
        boolean isUsable(String expectedVersion) {
            return modelVersion != null && modelVersion.equals(expectedVersion)
                    && !Float.isNaN(similarity) && !Float.isInfinite(similarity)
                    && similarity >= -1f && similarity <= 1f;
        }
    }

    public enum Status { OFF, NOT_INSTALLED, LOADING, READY, UNAVAILABLE }
    public enum Outcome { KEEP, BLOCK, UNKNOWN }
    public static final class FilterDecision {
        public final Outcome outcome;
        public final float similarity;
        public final String modelVersion;
        private FilterDecision(Outcome outcome, float similarity, String version) {
            this.outcome=outcome; this.similarity=similarity; this.modelVersion=version;
        }
        public static FilterDecision unknown() { return new FilterDecision(Outcome.UNKNOWN, 0f, null); }
        static FilterDecision from(ClassificationResult result, float threshold) {
            if (result == null || !result.isUsable(result.modelVersion)) return unknown();
            return new FilterDecision(result.similarity >= threshold ? Outcome.BLOCK : Outcome.KEEP,
                    result.similarity, result.modelVersion);
        }
    }

    public static final class CompletedDecision {
        public final String videoId, inputFingerprint;
        public final FilterDecision decision;
        private CompletedDecision(String id, String fingerprint, FilterDecision decision) {
            this.videoId=id; this.inputFingerprint=fingerprint; this.decision=decision;
        }
    }

    private static final class ResultCache {
        private final LinkedHashMap<String, ClassificationResult> entries =
                new LinkedHashMap<String, ClassificationResult>(16, 0.75f, true) {
                    @Override protected boolean removeEldestEntry(Map.Entry<String, ClassificationResult> eldest) {
                        return size() > MAX_CACHE_ENTRIES;
                    }
                };
        synchronized ClassificationResult get(String key) { return entries.get(key); }
        synchronized void put(String key, ClassificationResult value) { entries.put(key, value); }
        synchronized void clear() { entries.clear(); }
    }

    private static void removePending(String key, Object requestToken) {
        synchronized (PENDING) {
            if (PENDING.get(key) == requestToken) PENDING.remove(key);
        }
    }

    /** Bounded request guard; old captions, rules, models, and settings never reach a consumer. */
    private static final class CurrentRequests {
        private final LinkedHashMap<String, String> entries =
                new LinkedHashMap<String, String>(16, 0.75f, true) {
                    @Override protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                        return size() > MAX_CACHE_ENTRIES;
                    }
                };
        synchronized void remember(String id, String requestKey) { entries.put(id, requestKey); }
        synchronized boolean isCurrent(String id, String requestKey) {
            return requestKey.equals(entries.get(id));
        }
        synchronized void clear() { entries.clear(); }
    }

    private static String digest(String text) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            return result.toString();
        } catch (Exception unavailableDigest) {
            return null;
        }
    }
}
