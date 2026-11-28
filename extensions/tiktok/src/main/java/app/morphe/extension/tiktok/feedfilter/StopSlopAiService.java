/*
 * Copyright 2026 Hushfeed contributors
 * https://github.com/SysAdminDoc/hushfeed
 *
 * Built on icysymmetra/tiktok-patches-for-morphe (GPL-3.0).
 */
package app.morphe.extension.tiktok.feedfilter;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;

import ai.onnxruntime.OrtException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ONNX lives only in the manifest-declared :stopslop_ai process. All inbound IPC is queued on
 * one low-priority handler thread; the host process only sends bounded text and receives results.
 */
public final class StopSlopAiService extends Service {
    private static final int MSG_INITIALIZE = StopSlopAiProtocol.MSG_INITIALIZE;
    private static final int MSG_SET_ENABLED = StopSlopAiProtocol.MSG_SET_ENABLED;
    private static final int MSG_CLASSIFY = StopSlopAiProtocol.MSG_CLASSIFY;
    private static final int MSG_SHUTDOWN = StopSlopAiProtocol.MSG_SHUTDOWN;
    private static final int MSG_STATUS = StopSlopAiProtocol.MSG_STATUS;
    private static final int MSG_RESULT = StopSlopAiProtocol.MSG_RESULT;
    private static final int MSG_SHUTDOWN_ACK = StopSlopAiProtocol.MSG_SHUTDOWN_ACK;

    private static final String KEY_REQUEST_ID = StopSlopAiProtocol.KEY_REQUEST_ID;
    private static final String KEY_RULES_VERSION = StopSlopAiProtocol.KEY_RULES_VERSION;
    private static final String KEY_THRESHOLD = StopSlopAiProtocol.KEY_THRESHOLD;
    private static final String KEY_TEXT = StopSlopAiProtocol.KEY_TEXT;
    private static final String KEY_TOPICS = StopSlopAiProtocol.KEY_TOPICS;
    private static final String KEY_STATUS = StopSlopAiProtocol.KEY_STATUS;
    private static final String KEY_FAILURE = StopSlopAiProtocol.KEY_FAILURE;
    private static final String KEY_PHASE = StopSlopAiProtocol.KEY_PHASE;
    private static final String KEY_MODEL = StopSlopAiProtocol.KEY_MODEL;
    private static final String KEY_REVISION = StopSlopAiProtocol.KEY_REVISION;
    private static final String KEY_SELECTED_MODEL = StopSlopAiProtocol.KEY_SELECTED_MODEL;
    private static final String KEY_SELECTED_REVISION = StopSlopAiProtocol.KEY_SELECTED_REVISION;
    private static final String KEY_SIMILARITY = StopSlopAiProtocol.KEY_SIMILARITY;
    private static final String KEY_INFERENCE_MS = StopSlopAiProtocol.KEY_INFERENCE_MS;
    private static final String KEY_PROCESS = StopSlopAiProtocol.KEY_PROCESS;
    private static final String KEY_PID = StopSlopAiProtocol.KEY_PID;
    private static final String KEY_ABI = StopSlopAiProtocol.KEY_ABI;
    private static final String KEY_API = StopSlopAiProtocol.KEY_API;
    private static final String KEY_ORT = StopSlopAiProtocol.KEY_ORT;
    private static final String KEY_INIT_MS = StopSlopAiProtocol.KEY_INIT_MS;
    private static final String KEY_LAST_INFERENCE_MS = StopSlopAiProtocol.KEY_LAST_INFERENCE_MS;
    private static final String KEY_PSS_KB = StopSlopAiProtocol.KEY_PSS_KB;

    private static final int MAX_TEXT_CHARS = 2048;
    private static final int MAX_TOPICS = 16;
    private static final int MAX_TOPIC_CHARS = 128;
    private static final int MAX_ACCEPTED_REQUESTS = 8;
    private static final String ORT_VERSION = "1.20.0";

    private HandlerThread worker;
    private Messenger messenger;
    private volatile OnnxE5SemanticProvider provider;
    private volatile String modelKey = "";
    private volatile String modelRevision = "";
    private volatile String status = "OFF";
    private volatile String failure = "";
    private volatile String stage = "idle";
    private volatile boolean semanticEnabled;
    private volatile long initDurationMs;
    private volatile long lastInferenceDurationMs;
    private int queuedRequests;

    @Override public void onCreate() {
        super.onCreate();
        worker = new HandlerThread("StopSlop-isolated-inference", Process.THREAD_PRIORITY_BACKGROUND);
        worker.start();
        messenger = new Messenger(new Handler(worker.getLooper(), this::handleMessage));
    }

    @Override public IBinder onBind(Intent intent) { return messenger.getBinder(); }

    private boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_INITIALIZE:
                Bundle selection = message.getData();
                initialize(message.arg1 != 0,
                        selection.getString(KEY_SELECTED_MODEL, ""),
                        selection.getString(KEY_SELECTED_REVISION, ""), message.replyTo);
                return true;
            case MSG_SET_ENABLED:
                semanticEnabled = message.arg1 != 0 && provider != null && provider.isReady();
                failure = semanticEnabled ? "" : failure;
                status = semanticEnabled ? "READY" : provider == null ? "OFF" : "READY";
                stage = semanticEnabled ? "active" : "prepared";
                sendStatus(message.replyTo);
                return true;
            case MSG_CLASSIFY:
                classify(message);
                return true;
            case MSG_SHUTDOWN:
                shutdown(message.replyTo);
                return true;
            default:
                return false;
        }
    }

    private void initialize(boolean enableAfterReady, String requestedKey,
            String requestedRevision, Messenger replyTo) {
        semanticEnabled = false;
        failure = "";
        status = "LOADING";
        stage = "verifying selected package";
        sendStatus(replyTo);
        long started = android.os.SystemClock.elapsedRealtime();
        OnnxE5SemanticProvider candidate = null;
        try {
            String expectedProcess = getPackageName() + ":stopslop_ai";
            if (!expectedProcess.equals(processName())) {
                failure = "AI unavailable: isolated process is not supported by this patch target";
                status = "UNAVAILABLE";
                stage = "process-boundary verification";
                return;
            }
            StopSlopModelManager.Model model = StopSlopModelManager.verifiedModel(
                    this, requestedKey, requestedRevision);
            if (model == null) {
                closeProvider();
                boolean noSelection = requestedKey == null || requestedKey.isEmpty();
                if (noSelection && !StopSlopModelManager.installed(this).isEmpty()) {
                    failure = "No model is selected";
                    status = "INSTALLED_NOT_SELECTED";
                } else {
                    failure = noSelection ? "No compatible model is installed"
                            : "Selected model files are missing or invalid";
                    status = noSelection ? "NOT_INSTALLED" : "UNAVAILABLE";
                }
                stage = "model verification";
                return;
            }
            if (provider != null && provider.isReady() && model.key().equals(modelKey)
                    && model.revision.equals(modelRevision)) {
                semanticEnabled = enableAfterReady;
                status = "READY";
                stage = enableAfterReady ? "active" : "prepared";
                return;
            }

            closeProvider();
            stage = "creating ONNX sessions and validating embedding shape";
            candidate = new OnnxE5SemanticProvider(model);
            candidate.initialize();
            if (!candidate.isReady()) throw new IOException("Embedding probe did not complete");
            provider = candidate;
            candidate = null;
            modelKey = model.key();
            modelRevision = model.revision;
            semanticEnabled = enableAfterReady;
            status = "READY";
            stage = enableAfterReady ? "active" : "prepared";
            failure = "";
        } catch (OutOfMemoryError lowMemory) {
            failure = "Low memory while creating ONNX sessions";
            status = "UNAVAILABLE";
            stage = "model initialization";
            closeCandidate(candidate);
        } catch (Exception | LinkageError unavailable) {
            failure = failureCategory(unavailable);
            status = "UNAVAILABLE";
            stage = "model initialization";
            closeCandidate(candidate);
        } finally {
            initDurationMs = Math.max(0L, android.os.SystemClock.elapsedRealtime() - started);
            sendStatus(replyTo);
        }
    }

    private void classify(Message message) {
        Messenger replyTo = message.replyTo;
        if (replyTo == null) return;
        Bundle data = message.getData();
        long requestId = data.getLong(KEY_REQUEST_ID, -1L);
        if (requestId < 0L || !semanticEnabled || provider == null || !provider.isReady()) {
            sendUnknownResult(replyTo, requestId);
            return;
        }
        if (queuedRequests >= MAX_ACCEPTED_REQUESTS) {
            sendUnknownResult(replyTo, requestId);
            return;
        }
        queuedRequests++;
        long started = android.os.SystemClock.elapsedRealtime();
        try {
            String text = data.getString(KEY_TEXT, "");
            ArrayList<String> topics = data.getStringArrayList(KEY_TOPICS);
            float threshold = data.getFloat(KEY_THRESHOLD, Float.NaN);
            if (text.isEmpty() || text.length() > MAX_TEXT_CHARS || topics == null
                    || topics.isEmpty() || topics.size() > MAX_TOPICS
                    || Float.isNaN(threshold) || Float.isInfinite(threshold)
                    || threshold < 0.50f || threshold > 0.98f) {
                sendUnknownResult(replyTo, requestId);
                return;
            }
            List<String> safeTopics = new ArrayList<>(topics.size());
            for (String topic : topics) {
                if (topic == null || topic.trim().isEmpty() || topic.length() > MAX_TOPIC_CHARS) {
                    sendUnknownResult(replyTo, requestId);
                    return;
                }
                safeTopics.add(topic);
            }
            float similarity = provider.classify(text, safeTopics);
            if (Float.isNaN(similarity) || Float.isInfinite(similarity)
                    || similarity < -1f || similarity > 1f) {
                sendUnknownResult(replyTo, requestId);
                return;
            }
            lastInferenceDurationMs = Math.max(0L, android.os.SystemClock.elapsedRealtime() - started);
            stage = "active";
            Message result = Message.obtain(null, MSG_RESULT);
            Bundle response = new Bundle();
            response.putLong(KEY_REQUEST_ID, requestId);
            response.putFloat(KEY_SIMILARITY, similarity);
            response.putString(KEY_MODEL, modelKey);
            response.putString(KEY_REVISION, modelRevision);
            response.putLong(KEY_INFERENCE_MS, lastInferenceDurationMs);
            result.setData(response);
            try { replyTo.send(result); } catch (RemoteException ignored) { }
        } catch (OutOfMemoryError lowMemory) {
            failInference("Low memory during semantic inference", replyTo);
            sendUnknownResult(replyTo, requestId);
        } catch (Exception | LinkageError unavailable) {
            failInference(failureCategory(unavailable), replyTo);
            sendUnknownResult(replyTo, requestId);
        } finally {
            queuedRequests = Math.max(0, queuedRequests - 1);
        }
    }

    private void failInference(String reason, Messenger replyTo) {
        semanticEnabled = false;
        status = "UNAVAILABLE";
        failure = reason;
        stage = "inference";
        closeProvider();
        sendStatus(replyTo);
    }

    private void sendUnknownResult(Messenger target, long requestId) {
        if (target == null || requestId < 0L) return;
        Message result = Message.obtain(null, MSG_RESULT);
        Bundle response = new Bundle();
        response.putLong(KEY_REQUEST_ID, requestId);
        response.putFloat(KEY_SIMILARITY, Float.NaN);
        response.putString(KEY_MODEL, modelKey);
        response.putString(KEY_REVISION, modelRevision);
        response.putLong(KEY_INFERENCE_MS, 0L);
        result.setData(response);
        try { target.send(result); } catch (RemoteException ignored) { }
    }

    private void sendStatus(Messenger target) {
        if (target == null) return;
        Message update = Message.obtain(null, MSG_STATUS);
        Bundle state = new Bundle();
        state.putString(KEY_STATUS, status);
        state.putString(KEY_FAILURE, failure);
        state.putString(KEY_PHASE, stage);
        state.putString(KEY_MODEL, modelKey);
        state.putString(KEY_REVISION, modelRevision);
        state.putLong(KEY_INIT_MS, initDurationMs);
        state.putLong(KEY_LAST_INFERENCE_MS, lastInferenceDurationMs);
        state.putString(KEY_PROCESS, processName());
        state.putInt(KEY_PID, Process.myPid());
        state.putString(KEY_ABI, Build.SUPPORTED_ABIS.length == 0 ? "unknown" : Build.SUPPORTED_ABIS[0]);
        state.putInt(KEY_API, Build.VERSION.SDK_INT);
        state.putString(KEY_ORT, ORT_VERSION);
        Debug.MemoryInfo memory = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memory);
        state.putInt(KEY_PSS_KB, memory.getTotalPss());
        update.setData(state);
        try { target.send(update); } catch (RemoteException ignored) { }
    }

    private void shutdown(Messenger target) {
        semanticEnabled = false;
        closeProvider();
        status = "OFF";
        failure = "";
        stage = "stopped";
        if (target != null) {
            try { target.send(Message.obtain(null, MSG_SHUTDOWN_ACK)); } catch (RemoteException ignored) { }
        }
    }

    private void closeProvider() {
        OnnxE5SemanticProvider old = provider;
        provider = null;
        modelKey = "";
        modelRevision = "";
        if (old != null) closeCandidate(old);
    }

    private static void closeCandidate(OnnxE5SemanticProvider candidate) {
        if (candidate == null) return;
        try { candidate.close(); } catch (Exception | LinkageError ignored) { }
    }

    private static String failureCategory(Throwable failure) {
        if (failure instanceof LinkageError) return "Native ONNX runtime or ABI could not load";
        if (failure instanceof IOException) return "Model files or tensor contract are invalid";
        if (failure instanceof OrtException) return "ONNX Runtime could not execute the selected model";
        if (failure instanceof SecurityException) return "Android denied access to the selected model files";
        String name = failure == null ? "" : failure.getClass().getSimpleName();
        if (name.isEmpty()) return "ONNX Runtime operation failed";
        return name.length() > 48 ? name.substring(0, 48) : name;
    }

    private static String processName() {
        try (FileInputStream input = new FileInputStream("/proc/" + Process.myPid() + "/cmdline")) {
            StringBuilder name = new StringBuilder();
            int value;
            while ((value = input.read()) > 0) name.append((char) value);
            return name.length() == 0 ? "unknown" : name.toString();
        } catch (IOException unavailable) {
            return "unknown";
        }
    }

    @Override public void onDestroy() {
        semanticEnabled = false;
        closeProvider();
        if (worker != null) worker.quitSafely();
        super.onDestroy();
    }
}
