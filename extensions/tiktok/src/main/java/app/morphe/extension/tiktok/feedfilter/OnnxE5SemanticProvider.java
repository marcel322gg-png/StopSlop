/*
 * Copyright 2026 Hushfeed contributors
 * https://github.com/SysAdminDoc/hushfeed
 *
 * Built on icysymmetra/tiktok-patches-for-morphe (GPL-3.0).
 */
package app.morphe.extension.tiktok.feedfilter;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.extensions.OrtxPackage;

/** Real CPU-only multilingual-e5-small embedding provider for caption and topic text. */
final class OnnxE5SemanticProvider implements AutoCloseable {
    private static final int MAX_TOKENS = 512;
    private static final int EMBEDDING_DIMENSIONS = 384;
    private static final int MAX_TOPIC_EMBEDDINGS = 32;

    private final LinkedHashMap<String, float[]> topicEmbeddings =
            new LinkedHashMap<String, float[]>(16, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                    return size() > MAX_TOPIC_EMBEDDINGS;
                }
            };

    private OrtEnvironment environment;
    private OrtSession tokenizerSession;
    private OrtSession modelSession;
    private final StopSlopModelManager.Model selectedModel;

    OnnxE5SemanticProvider(StopSlopModelManager.Model selectedModel) {
        this.selectedModel = selectedModel;
    }

    String modelVersion() { return selectedModel.key(); }

    /** Called only by the isolated service's single serialized inference handler. */
    void initialize() throws Exception {
        File modelFile = selectedModel.modelFile();
        File tokenizerFile = selectedModel.tokenizerFile();
        if (!modelFile.isFile() || !tokenizerFile.isFile()) {
            throw new IOException("The selected model package is incomplete");
        }

        environment = OrtEnvironment.getEnvironment();
        try (OrtSession.SessionOptions tokenizerOptions = new OrtSession.SessionOptions()) {
            tokenizerOptions.setIntraOpNumThreads(1);
            tokenizerOptions.setInterOpNumThreads(1);
            tokenizerOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath());
            tokenizerSession = environment.createSession(tokenizerFile.getAbsolutePath(), tokenizerOptions);
        }
        try (OrtSession.SessionOptions modelOptions = new OrtSession.SessionOptions()) {
            modelOptions.setIntraOpNumThreads(1);
            modelOptions.setInterOpNumThreads(1);
            modelSession = environment.createSession(modelFile.getAbsolutePath(), modelOptions);
        }

        if (!tokenizerSession.getInputInfo().containsKey("inputs")
                || !tokenizerSession.getOutputInfo().containsKey("tokens")
                || !modelSession.getInputInfo().keySet().containsAll(Arrays.asList(
                        "input_ids", "attention_mask", "token_type_ids"))
                || !modelSession.getOutputInfo().containsKey("last_hidden_state")) {
            throw new IOException("The packaged embedding model has an unexpected tensor interface");
        }

        // READY means the selected profile can actually tokenize and execute, not just that two
        // session objects were created. Initialization is only called on the bounded worker.
        float[] probe = embed("query: StopSlop initialization check");
        if (probe.length != EMBEDDING_DIMENSIONS) {
            throw new IOException("Embedding initialization check returned an unexpected shape");
        }
        Arrays.fill(probe, 0f);
    }

    float classify(String text, List<String> topics) throws Exception {
        if (modelSession == null || tokenizerSession == null || text == null
                || topics == null || topics.isEmpty()) return Float.NaN;

        float[] caption = embed("passage: " + text);
        float bestSimilarity = -1f;
        for (String topic : topics) {
            if (topic == null || topic.trim().isEmpty()) continue;
            float[] topicVector = topicEmbedding(topic);
            float similarity = cosine(caption, topicVector);
            if (similarity > bestSimilarity) bestSimilarity = similarity;
        }
        return bestSimilarity < -1f ? Float.NaN : bestSimilarity;
    }

    private float[] topicEmbedding(String topic) throws Exception {
        synchronized (topicEmbeddings) {
            float[] cached = topicEmbeddings.get(topic);
            if (cached != null) return cached;
        }
        float[] encoded = embed("query: " + topic);
        synchronized (topicEmbeddings) {
            topicEmbeddings.put(topic, encoded);
        }
        return encoded;
    }

    private float[] embed(String text) throws Exception {
        int[] tokenIds = tokenize(text);
        if (tokenIds.length == 0 || tokenIds.length > MAX_TOKENS) {
            throw new IOException("Tokenizer returned an unsupported sequence length");
        }
        long[] ids = new long[tokenIds.length];
        long[] mask = new long[tokenIds.length];
        long[] tokenTypes = new long[tokenIds.length];
        for (int index = 0; index < tokenIds.length; index++) {
            ids[index] = tokenIds[index];
            mask[index] = 1L;
        }

        try (OnnxTensor inputIds = OnnxTensor.createTensor(environment,
                    LongBuffer.wrap(ids), new long[] {1L, ids.length});
             OnnxTensor attentionMask = OnnxTensor.createTensor(environment,
                    LongBuffer.wrap(mask), new long[] {1L, mask.length});
             OnnxTensor tokenTypeIds = OnnxTensor.createTensor(environment,
                    LongBuffer.wrap(tokenTypes), new long[] {1L, tokenTypes.length})) {
            Map<String, OnnxTensor> inputs = new java.util.HashMap<>(3);
            inputs.put("input_ids", inputIds);
            inputs.put("attention_mask", attentionMask);
            inputs.put("token_type_ids", tokenTypeIds);
            try (OrtSession.Result result = modelSession.run(inputs)) {
                OnnxValue value = result.get("last_hidden_state").orElse(null);
                if (!(value instanceof OnnxTensor)) throw new IOException("Embedding output is missing");
                float[] vector = meanPool(((OnnxTensor) value).getFloatBuffer(), tokenIds.length);
                normalize(vector);
                return vector;
            }
        }
    }

    private int[] tokenize(String text) throws OrtException, IOException {
        try (OnnxTensor input = OnnxTensor.createTensor(environment,
                    new String[] {text}, new long[] {1L});
             OrtSession.Result result = tokenizerSession.run(java.util.Collections.singletonMap("inputs", input))) {
            OnnxValue value = result.get("tokens").orElse(null);
            if (!(value instanceof OnnxTensor)) throw new IOException("Tokenizer output is missing");
            IntBuffer output = ((OnnxTensor) value).getIntBuffer();
            int count = output.remaining();
            if (count < 2) throw new IOException("Tokenizer output is too short");

            int[] raw = new int[count];
            output.get(raw);
            if (count <= MAX_TOKENS) return raw;

            // XLM-R uses </s> id 2. Keep it when the app-specific sequence bound truncates text.
            int[] bounded = Arrays.copyOf(raw, MAX_TOKENS);
            bounded[MAX_TOKENS - 1] = 2;
            return bounded;
        }
    }

    private static float[] meanPool(FloatBuffer hiddenStates, int sequenceLength) throws IOException {
        int expected = sequenceLength * EMBEDDING_DIMENSIONS;
        if (hiddenStates.remaining() != expected) {
            throw new IOException("Embedding output has an unexpected shape");
        }
        float[] vector = new float[EMBEDDING_DIMENSIONS];
        for (int token = 0; token < sequenceLength; token++) {
            for (int dimension = 0; dimension < EMBEDDING_DIMENSIONS; dimension++) {
                float value = hiddenStates.get();
                if (Float.isNaN(value) || Float.isInfinite(value)) {
                    throw new IOException("Embedding output contains a non-finite value");
                }
                vector[dimension] += value;
            }
        }
        for (int dimension = 0; dimension < EMBEDDING_DIMENSIONS; dimension++) {
            vector[dimension] /= sequenceLength;
        }
        return vector;
    }

    private static void normalize(float[] vector) throws IOException {
        double squaredNorm = 0d;
        for (float value : vector) {
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                throw new IOException("Embedding output contains a non-finite value");
            }
            squaredNorm += (double) value * value;
        }
        if (!(squaredNorm > 0d) || Double.isInfinite(squaredNorm) || Double.isNaN(squaredNorm)) {
            throw new IOException("Embedding output has a zero or invalid norm");
        }
        float norm = (float) Math.sqrt(squaredNorm);
        for (int index = 0; index < vector.length; index++) vector[index] /= norm;
    }

    private static float cosine(float[] left, float[] right) throws IOException {
        if (left.length != EMBEDDING_DIMENSIONS || right.length != EMBEDDING_DIMENSIONS) {
            throw new IOException("Embedding dimension does not match multilingual-e5-small");
        }
        double dot = 0d;
        for (int index = 0; index < EMBEDDING_DIMENSIONS; index++) {
            dot += (double) left[index] * right[index];
        }
        float similarity = (float) dot;
        if (Float.isNaN(similarity) || Float.isInfinite(similarity)
                || similarity < -1.001f || similarity > 1.001f) {
            throw new IOException("Cosine similarity is invalid");
        }
        return Math.max(-1f, Math.min(1f, similarity));
    }

    public boolean isReady() {
        return modelSession != null && tokenizerSession != null;
    }

    @Override
    public void close() throws Exception {
        synchronized (topicEmbeddings) {
            topicEmbeddings.clear();
        }
        OrtSession model = modelSession;
        OrtSession tokenizer = tokenizerSession;
        modelSession = null;
        tokenizerSession = null;
        if (model != null) model.close();
        if (tokenizer != null) tokenizer.close();
    }
}
