/*
 * Copyright 2026 Hushfeed contributors
 * https://github.com/SysAdminDoc/hushfeed
 *
 * Built on icysymmetra/tiktok-patches-for-morphe (GPL-3.0).
 */
package app.morphe.extension.tiktok.feedfilter;

/** Primitive IPC constants shared across the host and isolated service processes. */
final class StopSlopAiProtocol {
    static final int MSG_INITIALIZE = 1;
    static final int MSG_SET_ENABLED = 2;
    static final int MSG_CLASSIFY = 3;
    static final int MSG_SHUTDOWN = 4;
    static final int MSG_STATUS = 100;
    static final int MSG_RESULT = 101;
    static final int MSG_SHUTDOWN_ACK = 102;

    static final String KEY_REQUEST_ID = "request_id";
    static final String KEY_THRESHOLD = "threshold";
    static final String KEY_TEXT = "caption";
    static final String KEY_TOPICS = "topics";
    static final String KEY_RULES_VERSION = "rules_version";
    static final String KEY_STATUS = "status";
    static final String KEY_FAILURE = "failure";
    static final String KEY_PHASE = "phase";
    static final String KEY_MODEL = "model";
    static final String KEY_REVISION = "revision";
    static final String KEY_SELECTED_MODEL = "selected_model";
    static final String KEY_SELECTED_REVISION = "selected_revision";
    static final String KEY_SIMILARITY = "similarity";
    static final String KEY_INFERENCE_MS = "inference_ms";
    static final String KEY_PROCESS = "process";
    static final String KEY_PID = "pid";
    static final String KEY_ABI = "abi";
    static final String KEY_API = "api";
    static final String KEY_ORT = "ort";
    static final String KEY_INIT_MS = "init_ms";
    static final String KEY_LAST_INFERENCE_MS = "last_inference_ms";
    static final String KEY_PSS_KB = "pss_kb";

    private StopSlopAiProtocol() { }
}
