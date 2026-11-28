/*
 * Copyright 2026 Hushfeed contributors
 * https://github.com/SysAdminDoc/hushfeed
 *
 * Built on icysymmetra/tiktok-patches-for-morphe (GPL-3.0).
 */
package app.morphe.extension.tiktok.feedfilter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.ss.android.ugc.aweme.feed.model.Aweme;

import app.morphe.extension.shared.Utils;
import app.morphe.extension.tiktok.blockauthor.Reflect;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Main-process, fail-open Messenger client. This class has no ONNX references and never loads
 * model files. Binding is explicit from the model/settings UI; feed callbacks only read a bounded
 * result cache and enqueue one-way Messenger messages.
 */
public final class LocalContentInference {
    private static final int MAX_TEXT_CHARS = 2048;
    private static final int MAX_TOPICS = 16;
    private static final int MAX_TOPIC_CHARS = 128;
    private static final int MAX_CACHE_ENTRIES = 128;
    private static final int MAX_PENDING = 8;
    private static final long SERVICE_TIMEOUT_MS = 60_000L;
    private static final long REQUEST_TIMEOUT_MS = 20_000L;
    private static final String SERVICE_CLASS =
            "app.morphe.extension.tiktok.feedfilter.StopSlopAiService";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();
    private static final AtomicLong NEXT_REQUEST_ID = new AtomicLong();
    private static final ResultCache CACHE = new ResultCache();
    private static final CurrentRequests CURRENT_REQUESTS = new CurrentRequests();
    private static final Map<String, PendingRequest> PENDING_BY_KEY = new LinkedHashMap<>();
    private static final Map<Long, PendingRequest> PENDING_BY_ID = new LinkedHashMap<>();
    private static final List<File> PENDING_DELETES = new ArrayList<>();
    private static final CopyOnWriteArrayList<WeakReference<StatusListener>> STATUS_LISTENERS =
            new CopyOnWriteArrayList<>();
    private static final ThreadPoolExecutor FILE_IO = new ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(4), task -> {
        Thread thread = new Thread(task, "StopSlop-model-file-io");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    }, new ThreadPoolExecutor.AbortPolicy());

    private static volatile boolean enabled;
    private static volatile boolean preparing;
    private static volatile Status status = Status.OFF;
    private static volatile String failureReason = "";
    private static volatile String selectedModelKey = "";
    private static volatile String selectedModelRevision = "";
    private static volatile String serviceProcess = "not started";
    private static volatile String serviceModelKey = "";
    private static volatile String serviceRevision = "";
    private static volatile String serviceStage = "idle";
    private static volatile String serviceAbi = "unknown";
    private static volatile String serviceOrt = "1.20.0";
    private static volatile int servicePid;
    private static volatile int serviceApi;
    private static volatile int servicePssKb;
    private static volatile long initializationDurationMs;
    private static volatile long lastInferenceDurationMs;
    private static String currentRulesVersion = "";
    private static int currentThresholdBits;
    private static long configurationGeneration;
    private static long connectionGeneration;
    private static boolean binding;
    private static boolean bound;
    private static boolean shutdownPending;
    private static boolean requestAfterShutdown;
    private static boolean deleteDrainQueued;
    private static Messenger remote;
    private static IBinder remoteBinder;
    private static IBinder.DeathRecipient deathRecipient;

    private static final Messenger CLIENT = new Messenger(new Handler(Looper.getMainLooper(), message -> {
        handleServiceMessage(message);
        return true;
    }));

    private static final ServiceConnection CONNECTION = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            synchronized (LOCK) {
                binding = false;
                bound = true;
                remoteBinder = binder;
                remote = new Messenger(binder);
                deathRecipient = () -> MAIN.post(() -> serviceDied("Isolated AI process stopped unexpectedly"));
                try {
                    binder.linkToDeath(deathRecipient, 0);
                } catch (RemoteException dead) {
                    serviceDied("Isolated AI process stopped unexpectedly");
                    return;
                }
                sendInitializeLocked();
            }
        }

        @Override public void onServiceDisconnected(ComponentName name) {
            serviceDied("Isolated AI process stopped unexpectedly");
        }

        @Override public void onBindingDied(ComponentName name) {
            serviceDied("AI unavailable: isolated process is not supported by this patch target");
        }

        @Override public void onNullBinding(ComponentName name) {
            serviceDied("AI unavailable: isolated process is not supported by this patch target");
        }
    };

    private LocalContentInference() { }

    /** Metadata extraction and hashing only; no media is opened or sent outside the app. */
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

    /** Called only by the explicit settings action; never binds from feed evaluation. */
    public static void prepareSelectedModel() {
        Context context = Utils.getContext();
        StopSlopModelManager.Model selected = StopSlopModelManager.selectedModel(context);
        String activeKey = safeActiveKey(context);
        synchronized (LOCK) {
            enabled = false;
            preparing = true;
            selectedModelKey = selected == null ? activeKey : selected.key();
            selectedModelRevision = selected == null ? "" : selected.revision;
            CACHE.clear();
            CURRENT_REQUESTS.clear();
            clearPendingLocked();
            if (selected == null) {
                preparing = false;
                Status unselected = activeKey.isEmpty()
                        ? statusWithoutSelection(context) : Status.UNAVAILABLE;
                failureReason = failureForUnselected(unselected);
                setStatus(unselected);
                return;
            }
            failureReason = "";
            setStatus(Status.STARTING);
            bindLocked(context);
        }
    }

    /** Called only after the user turns the semantic switch on with an isolated provider ready. */
    public static void activateForFiltering() {
        Context context = Utils.getContext();
        StopSlopModelManager.Model selected = StopSlopModelManager.selectedModel(context);
        String activeKey = safeActiveKey(context);
        synchronized (LOCK) {
            selectedModelKey = selected == null ? activeKey : selected.key();
            selectedModelRevision = selected == null ? "" : selected.revision;
            if (selected == null) {
                enabled = false;
                preparing = false;
                Status unselected = activeKey.isEmpty()
                        ? statusWithoutSelection(context) : Status.UNAVAILABLE;
                failureReason = failureForUnselected(unselected);
                setStatus(unselected);
                return;
            }
            preparing = false;
            enabled = true;
            CACHE.clear();
            CURRENT_REQUESTS.clear();
            clearPendingLocked();
            failureReason = "";
            setStatus(Status.STARTING);
            if (remote != null) sendEnabledLocked(true);
            else bindLocked(context);
        }
    }

    /** Disabling closes the remote provider and unbinds; it does not touch the saved preference. */
    public static void setEnabled(boolean value) {
        if (value) {
            activateForFiltering();
            return;
        }
        synchronized (LOCK) {
            enabled = false;
            preparing = false;
            CACHE.clear();
            CURRENT_REQUESTS.clear();
            clearPendingLocked();
            failureReason = "";
            setStatus(Status.OFF);
            shutdownRemoteLocked();
        }
    }

    /** Model selection is not activation; close any old remote session without starting another. */
    public static void onModelChanged() {
        Context context = Utils.getContext();
        String activeKey = safeActiveKey(context);
        StopSlopModelManager.Model selected = StopSlopModelManager.selectedModel(context);
        synchronized (LOCK) {
            enabled = false;
            preparing = false;
            selectedModelKey = selected == null ? activeKey : selected.key();
            selectedModelRevision = selected == null ? "" : selected.revision;
            CACHE.clear();
            CURRENT_REQUESTS.clear();
            clearPendingLocked();
            failureReason = "";
            Status next = activeKey.isEmpty() ? statusWithoutSelection(context)
                    : selected == null ? Status.UNAVAILABLE : Status.OFF;
            failureReason = failureForUnselected(next);
            setStatus(next);
            shutdownRemoteLocked();
        }
    }

    public static void retryInitialization() {
        synchronized (LOCK) {
            failureReason = "";
            if (enabled) {
                preparing = false;
                setStatus(Status.STARTING);
                if (remote != null) sendInitializeLocked();
                else bindLocked(Utils.getContext());
                return;
            }
        }
        prepareSelectedModel();
    }

    public static boolean isReady() { return status == Status.READY; }
    public static boolean isEnabled() { return enabled; }
    public static Status getStatus() { return status; }
    public static String getFailureReason() { return failureReason; }

    public static String diagnosticsSummary() {
        String state = status.name();
        String model = serviceModelKey.isEmpty() ? selectedModelKey : serviceModelKey;
        String revision = serviceRevision.isEmpty() ? "unknown" : serviceRevision;
        return "StopSlop isolated AI: status=" + state
                + "; process=" + safeDiagnostic(serviceProcess)
                + "; pid=" + servicePid
                + "; model=" + safeDiagnostic(model)
                + "; revision=" + safeDiagnostic(revision)
                + "; stage=" + safeDiagnostic(serviceStage)
                + "; abi=" + safeDiagnostic(serviceAbi)
                + "; api=" + serviceApi
                + "; ort=" + safeDiagnostic(serviceOrt)
                + "; init_ms=" + initializationDurationMs
                + "; inference_ms=" + lastInferenceDurationMs
                + "; pss_kb=" + servicePssKb
                + "; failure=" + safeDiagnostic(failureReason);
    }

    public static void addStatusListener(StatusListener listener) {
        if (listener != null) STATUS_LISTENERS.add(new WeakReference<>(listener));
    }

    /** Invalidates local semantic results. This never sends topic rules unless a request is made. */
    public static void updateTopicConfiguration(String rulesVersion, float threshold) {
        String version = rulesVersion == null ? "" : rulesVersion;
        int thresholdBits = Float.floatToIntBits(threshold);
        synchronized (LOCK) {
            if (version.equals(currentRulesVersion) && thresholdBits == currentThresholdBits) return;
            currentRulesVersion = version;
            currentThresholdBits = thresholdBits;
            configurationGeneration++;
            CACHE.clear();
            CURRENT_REQUESTS.clear();
            clearPendingLocked();
        }
    }

    /** Returns a cached decision or queues one bounded asynchronous IPC request; all misses KEEP. */
    public static FilterDecision request(Features features, List<String> topics,
            String rulesVersion, float threshold, CompletionListener completion) {
        if (features == null || topics == null || topics.isEmpty() || completion == null || !enabled
                || status != Status.READY || remote == null) return FilterDecision.unknown();
        List<String> bounded = boundedTopics(topics);
        if (bounded.isEmpty() || Float.isNaN(threshold) || Float.isInfinite(threshold)
                || threshold < 0.50f || threshold > 0.98f) return FilterDecision.unknown();

        String key = features.videoId + ':' + features.inputFingerprint + ':'
                + (rulesVersion == null ? "" : rulesVersion) + ':'
                + Float.floatToIntBits(threshold) + ':' + selectedModelKey + '@' + selectedModelRevision;
        synchronized (LOCK) {
            if (!enabled || status != Status.READY || remote == null) return FilterDecision.unknown();
            CURRENT_REQUESTS.remember(features.videoId, key);
            ClassificationResult cached = CACHE.get(key);
            if (cached != null && CURRENT_REQUESTS.isCurrent(features.videoId, key)) {
                return FilterDecision.from(cached, threshold);
            }
            if (PENDING_BY_KEY.containsKey(key) || PENDING_BY_ID.size() >= MAX_PENDING) {
                return FilterDecision.unknown();
            }

            long requestId = NEXT_REQUEST_ID.incrementAndGet();
            PendingRequest pending = new PendingRequest(requestId, key, features,
                    rulesVersion == null ? "" : rulesVersion, threshold, bounded,
                    configurationGeneration, completion);
            PENDING_BY_KEY.put(key, pending);
            PENDING_BY_ID.put(requestId, pending);
            pending.timeout = () -> requestTimedOut(requestId);
            MAIN.postDelayed(pending.timeout, REQUEST_TIMEOUT_MS);
            try {
                Message request = Message.obtain(null, StopSlopAiProtocol.MSG_CLASSIFY);
                Bundle data = new Bundle();
                data.putLong(StopSlopAiProtocol.KEY_REQUEST_ID, requestId);
                data.putFloat(StopSlopAiProtocol.KEY_THRESHOLD, threshold);
                data.putString(StopSlopAiProtocol.KEY_TEXT, features.text);
                data.putString(StopSlopAiProtocol.KEY_RULES_VERSION, pending.rulesVersion);
                data.putStringArrayList(StopSlopAiProtocol.KEY_TOPICS, new ArrayList<>(bounded));
                request.setData(data);
                request.replyTo = CLIENT;
                remote.send(request); // Messenger is one-way; no inference occurs on this caller.
            } catch (RemoteException dead) {
                clearPendingLocked();
                MAIN.post(() -> serviceDied("Isolated AI process stopped unexpectedly"));
            }
        }
        return FilterDecision.unknown();
    }

    /** Closes the remote session before the file worker removes a model directory. */
    public static boolean deleteModelDirectoryWhenIdle(File directory) {
        if (directory == null) return false;
        synchronized (LOCK) {
            if (PENDING_DELETES.size() >= 4) return false;
            PENDING_DELETES.add(directory);
            enabled = false;
            preparing = false;
            CACHE.clear();
            CURRENT_REQUESTS.clear();
            clearPendingLocked();
            shutdownRemoteLocked();
            if (remote == null && !bound && !binding) queueDeleteDrainLocked();
            return true;
        }
    }

    private static void bindLocked(Context context) {
        if (context == null) {
            failLocked("Isolated AI service context is unavailable");
            return;
        }
        try {
            ComponentName serviceName = new ComponentName(context.getPackageName(), SERVICE_CLASS);
            ServiceInfo declared = context.getPackageManager().getServiceInfo(serviceName, 0);
            if (declared.exported || !declared.enabled
                    || !(context.getPackageName() + ":stopslop_ai").equals(declared.processName)) {
                failLocked("AI unavailable: isolated process is not supported by this patch target");
                return;
            }
        } catch (PackageManager.NameNotFoundException | RuntimeException unavailable) {
            failLocked("AI unavailable: isolated process is not supported by this patch target");
            return;
        }
        if (remote != null) {
            if (shutdownPending) requestAfterShutdown = true;
            else sendInitializeLocked();
            return;
        }
        if (binding || bound || status == Status.UNAVAILABLE) return;
        long generation = ++connectionGeneration;
        Intent intent = new Intent().setClassName(context.getPackageName(), SERVICE_CLASS);
        binding = true;
        try {
            if (!context.bindService(intent, CONNECTION, Context.BIND_AUTO_CREATE)) {
                binding = false;
                failLocked("AI unavailable: isolated process is not supported by this patch target");
                return;
            }
            setStatus(Status.STARTING);
            MAIN.postDelayed(() -> {
                synchronized (LOCK) {
                    if (generation == connectionGeneration
                            && (status == Status.STARTING || status == Status.LOADING)) {
                        failLocked("Isolated AI service did not respond; tap Retry initialization");
                    }
                }
            }, SERVICE_TIMEOUT_MS);
        } catch (RuntimeException unavailable) {
            binding = false;
            failLocked("AI unavailable: isolated process is not supported by this patch target");
        }
    }

    private static void sendInitializeLocked() {
        if (remote == null) return;
        if (shutdownPending) {
            requestAfterShutdown = true;
            return;
        }
        if (!enabled && !preparing) {
            shutdownRemoteLocked();
            return;
        }
        Message initialize = Message.obtain(null, StopSlopAiProtocol.MSG_INITIALIZE,
                enabled ? 1 : 0, 0);
        Bundle selection = new Bundle();
        selection.putString(StopSlopAiProtocol.KEY_SELECTED_MODEL, selectedModelKey);
        selection.putString(StopSlopAiProtocol.KEY_SELECTED_REVISION, selectedModelRevision);
        initialize.setData(selection);
        initialize.replyTo = CLIENT;
        sendLocked(initialize);
        setStatus(Status.LOADING);
    }

    private static void sendEnabledLocked(boolean value) {
        if (shutdownPending && value) {
            requestAfterShutdown = true;
            return;
        }
        if (remote == null) {
            if (value) bindLocked(Utils.getContext());
            return;
        }
        Message message = Message.obtain(null, StopSlopAiProtocol.MSG_SET_ENABLED, value ? 1 : 0, 0);
        message.replyTo = CLIENT;
        sendLocked(message);
    }

    private static void shutdownRemoteLocked() {
        if (remote != null) {
            if (shutdownPending) return;
            shutdownPending = true;
            Message shutdown = Message.obtain(null, StopSlopAiProtocol.MSG_SHUTDOWN);
            shutdown.replyTo = CLIENT;
            sendLocked(shutdown);
        } else if (!binding && !bound) {
            queueDeleteDrainLocked();
        }
    }

    private static void sendLocked(Message message) {
        try { remote.send(message); }
        catch (RemoteException dead) {
            MAIN.post(() -> serviceDied("Isolated AI process stopped unexpectedly"));
        }
    }

    private static void handleServiceMessage(Message message) {
        if (message.what == StopSlopAiProtocol.MSG_STATUS) {
            Bundle data = message.getData();
            String newStatus = data.getString(StopSlopAiProtocol.KEY_STATUS, "UNAVAILABLE");
            String reportedModel = boundedDiagnostic(data.getString(StopSlopAiProtocol.KEY_MODEL, ""), 128);
            String reportedRevision = boundedDiagnostic(
                    data.getString(StopSlopAiProtocol.KEY_REVISION, ""), 96);
            if (!reportedModel.isEmpty() && !selectedModelKey.isEmpty()
                    && (!reportedModel.equals(selectedModelKey)
                    || !reportedRevision.equalsIgnoreCase(selectedModelRevision))) return;
            serviceProcess = boundedDiagnostic(data.getString(StopSlopAiProtocol.KEY_PROCESS, "unknown"), 96);
            servicePid = Math.max(0, data.getInt(StopSlopAiProtocol.KEY_PID, 0));
            serviceModelKey = reportedModel;
            serviceRevision = reportedRevision;
            serviceStage = boundedDiagnostic(data.getString(StopSlopAiProtocol.KEY_PHASE, "unknown"), 96);
            serviceAbi = boundedDiagnostic(data.getString(StopSlopAiProtocol.KEY_ABI, "unknown"), 48);
            serviceApi = Math.max(0, data.getInt(StopSlopAiProtocol.KEY_API, 0));
            serviceOrt = boundedDiagnostic(data.getString(StopSlopAiProtocol.KEY_ORT, "unknown"), 32);
            initializationDurationMs = Math.max(0L, data.getLong(StopSlopAiProtocol.KEY_INIT_MS, 0L));
            lastInferenceDurationMs = Math.max(0L, data.getLong(StopSlopAiProtocol.KEY_LAST_INFERENCE_MS, 0L));
            servicePssKb = Math.max(0, data.getInt(StopSlopAiProtocol.KEY_PSS_KB, 0));
            failureReason = boundedDiagnostic(data.getString(StopSlopAiProtocol.KEY_FAILURE, ""), 96);
            Status parsed = parseStatus(newStatus);
            if (parsed == Status.UNAVAILABLE || parsed == Status.NOT_INSTALLED
                    || parsed == Status.INSTALLED_NOT_SELECTED) {
                enabled = false;
                clearPending();
            }
            setStatus(parsed);
            return;
        }
        if (message.what == StopSlopAiProtocol.MSG_RESULT) {
            Bundle data = message.getData();
            long requestId = data.getLong(StopSlopAiProtocol.KEY_REQUEST_ID, -1L);
            PendingRequest pending;
            synchronized (LOCK) {
                pending = PENDING_BY_ID.remove(requestId);
                if (pending == null) return;
                PENDING_BY_KEY.remove(pending.key);
                if (pending.timeout != null) MAIN.removeCallbacks(pending.timeout);
                float similarity = data.getFloat(StopSlopAiProtocol.KEY_SIMILARITY, Float.NaN);
                String model = data.getString(StopSlopAiProtocol.KEY_MODEL, "");
                String revision = data.getString(StopSlopAiProtocol.KEY_REVISION, "");
                String identity = model + "@" + revision;
                if (pending.generation == configurationGeneration && enabled
                        && model.equals(selectedModelKey)
                        && revision.equalsIgnoreCase(selectedModelRevision)
                        && validSimilarity(similarity)
                        && CURRENT_REQUESTS.isCurrent(pending.features.videoId, pending.key)) {
                    ClassificationResult result = new ClassificationResult(similarity, identity);
                    CACHE.put(pending.key, result);
                    long inferenceMs = Math.max(0L,
                            data.getLong(StopSlopAiProtocol.KEY_INFERENCE_MS, 0L));
                    if (inferenceMs > 0L) lastInferenceDurationMs = inferenceMs;
                    try {
                        pending.completion.onComplete(new CompletedDecision(pending.features.videoId,
                                pending.features.inputFingerprint,
                                FilterDecision.from(result, pending.threshold)));
                    } catch (RuntimeException ignored) { }
                }
            }
            return;
        }
        if (message.what == StopSlopAiProtocol.MSG_SHUTDOWN_ACK) {
            synchronized (LOCK) {
                shutdownPending = false;
                if (requestAfterShutdown && (enabled || preparing)) {
                    requestAfterShutdown = false;
                    sendInitializeLocked();
                } else {
                    requestAfterShutdown = false;
                    setStatus(selectedModelKey.isEmpty()
                            ? statusWithoutSelection(Utils.getContext()) : Status.OFF);
                    unbindLocked();
                    queueDeleteDrainLocked();
                }
            }
        }
    }

    private static void requestTimedOut(long requestId) {
        synchronized (LOCK) {
            PendingRequest pending = PENDING_BY_ID.get(requestId);
            if (pending == null) return;
            failLocked("Isolated AI inference timed out; semantic filtering is unavailable");
        }
    }

    private static void serviceDied(String reason) {
        synchronized (LOCK) {
            failLocked(reason);
        }
    }

    private static void failLocked(String reason) {
        enabled = false;
        preparing = false;
        failureReason = boundedDiagnostic(reason, 96);
        serviceStage = "service unavailable";
        CACHE.clear();
        CURRENT_REQUESTS.clear();
        clearPendingLocked();
        setStatus(Status.UNAVAILABLE);
        unbindLocked();
        queueDeleteDrainLocked();
    }

    private static void unbindLocked() {
        Messenger previous = remote;
        remote = null;
        if (remoteBinder != null && deathRecipient != null) {
            try { remoteBinder.unlinkToDeath(deathRecipient, 0); } catch (RuntimeException ignored) { }
        }
        remoteBinder = null;
        deathRecipient = null;
        shutdownPending = false;
        requestAfterShutdown = false;
        if (bound || binding) {
            Context context = Utils.getContext();
            try { if (context != null) context.unbindService(CONNECTION); } catch (RuntimeException ignored) { }
        }
        bound = false;
        binding = false;
        connectionGeneration++;
        if (previous != null) queueDeleteDrainLocked();
    }

    private static void clearPending() {
        synchronized (LOCK) { clearPendingLocked(); }
    }

    private static void clearPendingLocked() {
        for (PendingRequest pending : PENDING_BY_ID.values()) {
            if (pending.timeout != null) MAIN.removeCallbacks(pending.timeout);
        }
        PENDING_BY_ID.clear();
        PENDING_BY_KEY.clear();
    }

    private static void queueDeleteDrainLocked() {
        if (deleteDrainQueued || PENDING_DELETES.isEmpty() || remote != null || bound || binding) return;
        deleteDrainQueued = true;
        try {
            FILE_IO.execute(() -> {
                while (true) {
                    File directory;
                    synchronized (LOCK) {
                        if (PENDING_DELETES.isEmpty()) {
                            deleteDrainQueued = false;
                            return;
                        }
                        directory = PENDING_DELETES.remove(0);
                    }
                    deleteTree(directory);
                }
            });
        } catch (RuntimeException full) {
            deleteDrainQueued = false;
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

    private static Status parseStatus(String value) {
        try { return Status.valueOf(value); } catch (RuntimeException invalid) { return Status.UNAVAILABLE; }
    }

    private static String safeActiveKey(Context context) {
        try { return StopSlopModelManager.activeKey(context); } catch (RuntimeException unavailable) { return ""; }
    }

    private static Status statusWithoutSelection(Context context) {
        if (context == null) return Status.NOT_INSTALLED;
        try {
            if (!StopSlopModelManager.installed(context).isEmpty()) return Status.INSTALLED_NOT_SELECTED;
            return StopSlopModelManager.hasModelFiles(context) ? Status.UNAVAILABLE : Status.NOT_INSTALLED;
        } catch (RuntimeException unavailable) {
            return Status.UNAVAILABLE;
        }
    }

    private static String failureForUnselected(Status status) {
        if (status == Status.INSTALLED_NOT_SELECTED) return "No model is selected";
        if (status == Status.UNAVAILABLE) return "Model files exist but selection is invalid or incomplete";
        return "No compatible model is installed";
    }

    private static String boundedDiagnostic(String value, int limit) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder safe = new StringBuilder(Math.min(limit, value.length()));
        for (int i = 0; i < value.length() && safe.length() < limit; i++) {
            char ch = value.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '.' || ch == ':' || ch == '/' || ch == ' ') {
                safe.append(ch);
            } else safe.append('_');
        }
        return safe.toString();
    }

    private static String safeDiagnostic(String value) { return boundedDiagnostic(value, 128); }

    private static boolean validSimilarity(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value) && value >= -1f && value <= 1f;
    }

    private static List<String> boundedTopics(List<String> topics) {
        List<String> bounded = new ArrayList<>(Math.min(MAX_TOPICS, topics.size()));
        for (String topic : topics) {
            if (topic == null) continue;
            String value = topic.trim();
            if (value.isEmpty() || value.length() > MAX_TOPIC_CHARS || bounded.contains(value)) continue;
            bounded.add(value);
            if (bounded.size() == MAX_TOPICS) break;
        }
        return Collections.unmodifiableList(bounded);
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

    private static final class PendingRequest {
        final long requestId;
        final String key;
        final Features features;
        final String rulesVersion;
        final float threshold;
        final List<String> topics;
        final long generation;
        final CompletionListener completion;
        Runnable timeout;
        PendingRequest(long requestId, String key, Features features, String rulesVersion,
                float threshold, List<String> topics, long generation, CompletionListener completion) {
            this.requestId = requestId;
            this.key = key;
            this.features = features;
            this.rulesVersion = rulesVersion;
            this.threshold = threshold;
            this.topics = topics;
            this.generation = generation;
            this.completion = completion;
        }
    }

    public interface CompletionListener { void onComplete(CompletedDecision result); }
    public interface StatusListener { void onStatusChanged(Status status); }

    public static final class Features {
        public final String videoId;
        /** Bounded, transient caption text; never persisted or logged. */
        public final String text;
        public final String inputFingerprint;
        private Features(String id, String text, String fingerprint) {
            this.videoId = id; this.text = text; this.inputFingerprint = fingerprint;
        }
    }

    public static final class ClassificationResult {
        public final float similarity;
        public final String modelVersion;
        ClassificationResult(float similarity, String modelVersion) {
            this.similarity = similarity; this.modelVersion = modelVersion;
        }
        boolean isUsable(String expectedVersion) {
            return modelVersion != null && modelVersion.equals(expectedVersion) && validSimilarity(similarity);
        }
    }

    public enum Status { OFF, NOT_INSTALLED, INSTALLED_NOT_SELECTED, STARTING, LOADING, READY, UNAVAILABLE }
    public enum Outcome { KEEP, BLOCK, UNKNOWN }

    public static final class FilterDecision {
        public final Outcome outcome;
        public final float similarity;
        public final String modelVersion;
        private FilterDecision(Outcome outcome, float similarity, String version) {
            this.outcome = outcome; this.similarity = similarity; this.modelVersion = version;
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
            this.videoId = id; this.inputFingerprint = fingerprint; this.decision = decision;
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

    private static final class CurrentRequests {
        private final LinkedHashMap<String, String> entries =
                new LinkedHashMap<String, String>(16, 0.75f, true) {
                    @Override protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                        return size() > MAX_CACHE_ENTRIES;
                    }
                };
        synchronized void remember(String id, String requestKey) { entries.put(id, requestKey); }
        synchronized boolean isCurrent(String id, String requestKey) { return requestKey.equals(entries.get(id)); }
        synchronized void clear() { entries.clear(); }
    }
}
