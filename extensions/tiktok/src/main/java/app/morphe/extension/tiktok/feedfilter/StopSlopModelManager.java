/*
 * Copyright 2026 Hushfeed contributors
 * https://github.com/SysAdminDoc/hushfeed
 *
 * Built on icysymmetra/tiktok-patches-for-morphe (GPL-3.0).
 */
package app.morphe.extension.tiktok.feedfilter;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import app.morphe.extension.shared.Utils;

/** Installs data-only E5 packages into TikTok's private files; downloaded code is never loaded. */
public final class StopSlopModelManager {
    public static final String PROFILE_E5 = "e5-xlm-roberta-onnx-v1";
    public static final int PACKAGE_SCHEMA = 1;
    public static final long MAX_PACKAGE_BYTES = 160L * 1024L * 1024L;
    public static final long MAX_MODEL_BYTES = 150L * 1024L * 1024L;
    public static final long MAX_TOKENIZER_BYTES = 12L * 1024L * 1024L;
    public static final long DEFAULT_QUOTA_BYTES = 512L * 1024L * 1024L;
    public static final int MAX_INSTALLED_MODELS = 2;
    public static final String OFFICIAL_CATALOG_URL =
            "https://marcel322gg-png.github.io/StopSlop/catalog/v1/catalog.json";
    private static final String PREFS = "stop_slop_models_v1";
    private static final String CATALOG_CACHE = "official_catalog_v1";
    private static final String ACTIVE = "active_model";
    private static final String ROOT = "stopSlop/models";
    private static final String[] REQUIRED_FILES = {
            "manifest.json", "model/model.onnx", "tokenizer/tokenizer.onnx",
            "LICENSE.txt", "NOTICE.txt"
    };
    private static final Set<String> CATALOG_HOSTS = Collections.singleton("marcel322gg-png.github.io");
    private static final Set<String> RELEASE_HOSTS = Collections.unmodifiableSet(new HashSet<>(java.util.Arrays.asList(
            "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com")));
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "StopSlop-model-manager");
        thread.setDaemon(true);
        return thread;
    });

    private StopSlopModelManager() { }

    public static final class Model {
        public final String id;
        public final String version;
        public final String name;
        public final String publisher;
        public final String license;
        public final long modelBytes;
        public final long tokenizerBytes;
        public final long installedBytes;
        public final long memoryEstimateBytes;
        public final String languages;
        public final String revision;
        public final String directory;
        public final boolean builtInVerified;

        private Model(JSONObject manifest, File folder, boolean verifyHashes) throws IOException {
            try {
                id = required(manifest, "modelId");
                version = required(manifest, "modelVersion");
                name = manifest.optString("displayName", id);
                publisher = manifest.optString("publisher", "Unverified publisher");
                license = required(manifest, "licenseSpdx");
                revision = required(manifest, "sourceRevision");
                languages = manifest.optString("languages", "Not independently verified");
                memoryEstimateBytes = manifest.optLong("memoryEstimateBytes", 0L);
                JSONArray files = manifest.getJSONArray("files");
                long modelSize = -1L;
                long tokenizerSize = -1L;
                long total = 0L;
                boolean known = PROFILE_E5.equals(manifest.getString("engineProfile"));
                for (int i = 0; i < files.length(); i++) {
                    JSONObject entry = files.getJSONObject(i);
                    String path = entry.getString("path");
                    long size = entry.getLong("sizeBytes");
                    String hash = entry.getString("sha256");
                    File file = child(folder, path);
                    if (!file.isFile() || file.length() != size
                            || (verifyHashes && !hash.equals(sha256(file)))) {
                        throw new IOException("Installed model file failed verification: " + path);
                    }
                    if (Long.MAX_VALUE - total < size) throw new IOException("Model size overflow");
                    total += size;
                    if ("model/model.onnx".equals(path)) modelSize = size;
                    else if ("tokenizer/tokenizer.onnx".equals(path)) tokenizerSize = size;
                    else if (!"LICENSE.txt".equals(path) && !"NOTICE.txt".equals(path)) known = false;
                }
                if (modelSize <= 0L || tokenizerSize <= 0L) {
                    throw new IOException("The E5 package is missing model or tokenizer data");
                }
                modelBytes = modelSize;
                tokenizerBytes = tokenizerSize;
                installedBytes = total;
                directory = folder.getAbsolutePath();
                builtInVerified = known && isPinnedE5(manifest);
            } catch (JSONException | ArithmeticException malformed) {
                throw new IOException("The installed model manifest is invalid", malformed);
            }
        }

        public File modelFile() { return new File(directory, "model/model.onnx"); }
        public File tokenizerFile() { return new File(directory, "tokenizer/tokenizer.onnx"); }
        public String key() { return id + "/" + version; }
    }

    public interface Callback {
        void onProgress(long currentBytes, long totalBytes);
        void onComplete(Model model, Exception failure);
    }

    public interface CatalogCallback {
        void onComplete(List<CatalogEntry> entries, boolean fromCache, String warning);
    }

    public static final class CatalogEntry {
        public final String id;
        public final String name;
        public final String description;
        public final String packageVersion;
        public final String runtimeProfile;
        public final String publisher;
        public final String languages;
        public final String quantization;
        public final String format;
        public final String runtimeVersion;
        public final String memoryEstimateLabel;
        public final long memoryEstimateBytes;
        public final String license;
        public final String licenseUrl;
        public final String sourceRepository;
        public final String sourceRevision;
        public final String downloadUrl;
        public final String sha256;
        public final String minimumAppVersion;
        public final long packageBytes;
        public final int minimumAndroidApi;
        public final boolean compatible;
        public final String incompatibility;

        private CatalogEntry(JSONObject json) throws IOException {
            try {
                id = safeId(json.getString("id"));
                name = required(json, "name");
                description = required(json, "description");
                packageVersion = required(json, "packageVersion");
                if (!packageVersion.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) throw new IOException("Invalid catalog package version");
                runtimeProfile = required(json, "runtimeProfile");
                publisher = required(json, "publisher");
                languages = joinStrings(json.getJSONArray("languages"));
                quantization = required(json, "quantization");
                format = required(json, "format");
                runtimeVersion = required(json, "runtimeVersion");
                memoryEstimateBytes = json.getLong("memoryEstimateBytes");
                memoryEstimateLabel = required(json, "memoryEstimateLabel");
                license = required(json, "licenseSpdx");
                licenseUrl = requireHttps(json.getString("licenseUrl"));
                sourceRepository = required(json, "upstreamRepository");
                sourceRevision = required(json, "upstreamRevision");
                sha256 = json.getString("sha256");
                if (!safeDigest(sha256)) throw new IOException("Invalid catalog package checksum");
                packageBytes = json.getLong("packageSizeBytes");
                if (packageBytes <= 0L || packageBytes > MAX_PACKAGE_BYTES) throw new IOException("Catalog package size is outside the supported limit");
                minimumAppVersion = required(json, "minimumAppVersion");
                minimumAndroidApi = json.getInt("minimumAndroidApi");
                String assetName = required(json, "assetName");
                if (!assetName.matches("[A-Za-z0-9._-]{1,100}\\.ssmodel")) throw new IOException("Invalid catalog asset name");
                if (!"text-embedding".equals(json.getString("task"))
                        || !("model-v" + packageVersion).equals(json.getString("releaseTag"))) {
                    throw new IOException("Catalog model task or release tag is unsupported");
                }
                String expectedUrl = "https://github.com/marcel322gg-png/StopSlop/releases/download/model-v"
                        + packageVersion + "/" + assetName;
                downloadUrl = json.getString("downloadUrl");
                if (!expectedUrl.equals(downloadUrl)) throw new IOException("Catalog release URL is not the canonical project asset URL");

                JSONArray abis = json.getJSONArray("compatibleAbis");
                Set<String> abiSet = new HashSet<>();
                for (int i = 0; i < abis.length(); i++) abiSet.add(abis.getString(i));
                boolean e5 = "multilingual-e5-small".equals(id)
                        && PROFILE_E5.equals(runtimeProfile)
                        && PACKAGE_SCHEMA == json.getInt("packageSchemaVersion")
                        && "Xenova/multilingual-e5-small".equals(sourceRepository)
                        && "761b726dd34fb83930e26aab4e9ac3899aa1fa78".equals(sourceRevision)
                        && "INT8".equals(quantization) && "ONNX".equals(format)
                        && "ONNX Runtime Android 1.20.0; ORT Extensions 0.13.0".equals(runtimeVersion)
                        && "MIT".equals(license)
                        && memoryEstimateBytes > 0L
                        && minimumAndroidApi <= 23
                        && abiSet.contains("arm64-v8a") && abiSet.contains("armeabi-v7a")
                        && versionAtMost(minimumAppVersion, "0.84.0");
                compatible = e5;
                incompatibility = e5 ? "" : "This model profile is not supported by this StopSlop runtime.";
            } catch (JSONException | IllegalArgumentException malformed) {
                throw new IOException("Official model catalog entry is malformed", malformed);
            }
        }

        public String releaseTag() { return "model-v" + packageVersion; }
    }

    public static final class Operation {
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private volatile HttpURLConnection connection;
        public void cancel() {
            cancelled.set(true);
            HttpURLConnection active = connection;
            if (active != null) active.disconnect();
        }
    }

    public static List<Model> installed(Context context) {
        File root = modelRoot(context);
        File[] models = root.listFiles();
        if (models == null) return Collections.emptyList();
        List<Model> result = new ArrayList<>();
        for (File idFolder : models) {
            if (!idFolder.isDirectory() || !idFolder.getName().matches("[A-Za-z0-9._-]{1,64}")) continue;
            File[] versions = idFolder.listFiles();
            if (versions == null) continue;
            for (File versionFolder : versions) {
                if (!versionFolder.isDirectory()) continue;
                try {
                    result.add(readModel(versionFolder, false));
                } catch (IOException ignored) {
                    // A partially installed/corrupt directory is unavailable, not a loadable model.
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static Model active(Context context) {
        String key = preferences(context).getString(ACTIVE, "");
        if (key == null || !key.matches("[A-Za-z0-9._-]{1,64}/[A-Za-z0-9._-]{1,64}")) return null;
        String[] parts = key.split("/", 2);
        if (parts.length != 2) return null;
        try { return readModel(new File(new File(modelRoot(context), parts[0]), parts[1]), true); }
        catch (IOException unavailable) { return null; }
    }

    public static String activeKey(Context context) {
        if (context == null) return "";
        String key = preferences(context).getString(ACTIVE, "");
        return key == null ? "" : key;
    }

    /** A cheap feed-path availability check; the full SHA-256 audit runs on the model worker. */
    public static boolean hasActiveModel(Context context) {
        if (context == null) return false;
        String key = preferences(context).getString(ACTIVE, "");
        if (key == null || !key.matches("[A-Za-z0-9._-]{1,64}/[A-Za-z0-9._-]{1,64}")) return false;
        String[] parts = key.split("/", 2);
        File root = modelRoot(context);
        File folder = new File(new File(root, parts[0]), parts[1]);
        return new File(folder, "manifest.json").isFile()
                && new File(folder, "model/model.onnx").isFile()
                && new File(folder, "tokenizer/tokenizer.onnx").isFile();
    }

    public static boolean select(Context context, String key) {
        if (key == null) return false;
        Model selected = null;
        for (Model model : installed(context)) if (key.equals(model.key())) selected = model;
        if (selected == null) return false;
        if (!preferences(context).edit().putString(ACTIVE, key).commit()) return false;
        LocalContentInference.onModelChanged();
        return true;
    }

    public static boolean remove(Context context, String key) {
        if (key == null || key.equals(preferences(context).getString(ACTIVE, ""))) return false;
        for (Model model : installed(context)) {
            if (!key.equals(model.key())) continue;
            return LocalContentInference.deleteModelDirectoryWhenIdle(new File(model.directory));
        }
        return false;
    }

    public static long installedBytes(Context context) {
        long total = 0L;
        for (Model model : installed(context)) total += model.installedBytes;
        return total;
    }

    public static boolean clearActive(Context context) {
        if (!preferences(context).edit().remove(ACTIVE).commit()) return false;
        LocalContentInference.onModelChanged();
        return true;
    }

    public static Operation importPackage(Context context, Uri uri, Callback callback) {
        Operation operation = new Operation();
        IO.execute(() -> {
            Model model = null;
            Exception failure = null;
            File archive = new File(context.getCacheDir(), "stop_slop_model_import_" + System.nanoTime() + ".ssmodel.part");
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                if (input == null) throw new IOException("The selected package cannot be opened");
                copyBounded(input, archive, MAX_PACKAGE_BYTES, operation, callback, -1L);
                model = installArchive(context, archive, operation, callback, -1L);
            } catch (Exception error) {
                failure = error;
            } finally {
                archive.delete();
            }
            complete(callback, model, failure);
        });
        return operation;
    }

    /** Loads the fixed public catalog, falling back to the last validated response while offline. */
    public static void fetchCatalog(Context context, CatalogCallback callback) {
        Context app = context.getApplicationContext();
        Context owner = app == null ? context : app;
        IO.execute(() -> {
            List<CatalogEntry> entries = null;
            String warning = null;
            boolean fromCache = false;
            Operation operation = new Operation();
            try {
                URL url = checkedUrl(OFFICIAL_CATALOG_URL);
                HttpURLConnection connection = openFollowingHttps(
                        url, operation, CATALOG_HOSTS, "application/json");
                try {
                    int responseCode = connection.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        throw new IOException("Official catalog returned HTTP " + responseCode);
                    }
                    byte[] response;
                    try (InputStream input = connection.getInputStream()) {
                        response = readBounded(input, 64 * 1024);
                    }
                    String json = new String(response, StandardCharsets.UTF_8);
                    entries = parseCatalog(json);
                    preferences(owner).edit().putString(CATALOG_CACHE, json).apply();
                } finally {
                    connection.disconnect();
                }
            } catch (Exception networkFailure) {
                warning = networkFailure.getMessage() == null ? "Catalog refresh failed" : networkFailure.getMessage();
                String cached = preferences(owner).getString(CATALOG_CACHE, "");
                if (cached != null && !cached.isEmpty()) {
                    try {
                        entries = parseCatalog(cached);
                        fromCache = true;
                    } catch (IOException invalidCache) {
                        warning = "Cached model catalog is invalid";
                    }
                }
            } finally {
                HttpURLConnection connection = operation.connection;
                if (connection != null) connection.disconnect();
            }
            callback.onComplete(entries == null ? Collections.emptyList() : entries, fromCache, warning);
        });
    }

    private static List<CatalogEntry> parseCatalog(String json) throws IOException {
        if (json == null || json.getBytes(StandardCharsets.UTF_8).length > 64 * 1024) {
            throw new IOException("Model catalog exceeds its size limit");
        }
        try {
            JSONObject catalog = new JSONObject(json);
            String catalogVersion = catalog.optString("catalogVersion", "");
            if (catalog.getInt("schemaVersion") != 1 || !catalogVersion.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) {
                throw new IOException("Unsupported model catalog schema");
            }
            JSONArray models = catalog.getJSONArray("models");
            if (models.length() > 16) throw new IOException("Model catalog contains too many entries");
            List<CatalogEntry> entries = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < models.length(); i++) {
                CatalogEntry entry = new CatalogEntry(models.getJSONObject(i));
                if (!ids.add(entry.id + "/" + entry.packageVersion)) throw new IOException("Duplicate model catalog entry");
                entries.add(entry);
            }
            return Collections.unmodifiableList(entries);
        } catch (JSONException malformed) {
            throw new IOException("Official model catalog is not valid JSON", malformed);
        }
    }

    /** Explicitly initiated HTTPS download of a data-only .ssmodel package. */
    public static Operation downloadPackage(Context context, String sourceUrl, Callback callback) {
        return downloadPackage(context, sourceUrl, -1L, null, null, callback);
    }

    public static Operation downloadCatalogModel(Context context, CatalogEntry entry, Callback callback) {
        if (entry == null || !entry.compatible) throw new IllegalArgumentException("Catalog model is not compatible");
        return downloadPackage(context, entry.downloadUrl, entry.packageBytes, entry.sha256, RELEASE_HOSTS, callback);
    }

    private static Operation downloadPackage(Context context, String sourceUrl, long expectedBytes,
            String expectedSha256, Set<String> allowedHosts, Callback callback) {
        Operation operation = new Operation();
        IO.execute(() -> {
            Model model = null;
            Exception failure = null;
            File archive = new File(context.getCacheDir(), "stop_slop_model_download_" + System.nanoTime() + ".ssmodel.part");
            try {
                URL url = checkedUrl(sourceUrl);
                HttpURLConnection connection = openFollowingHttps(url, operation, allowedHosts,
                        "application/zip, application/octet-stream;q=0.9");
                operation.connection = connection;
                int response = connection.getResponseCode();
                if (response < 200 || response >= 300) throw new IOException("Model source returned HTTP " + response);
                long declared;
                try { declared = Long.parseLong(connection.getHeaderField("Content-Length")); }
                catch (RuntimeException missingLength) { declared = -1L; }
                if (declared <= 0L || declared > MAX_PACKAGE_BYTES) {
                    throw new IOException("The model package size is missing or exceeds the download limit");
                }
                if (expectedBytes > 0L && declared != expectedBytes) throw new IOException("Catalog package size does not match the release asset");
                if (context.getFilesDir().getUsableSpace() < declared + MAX_MODEL_BYTES + MAX_TOKENIZER_BYTES) {
                    throw new IOException("There is not enough free space to stage and install this model");
                }
                try (InputStream input = connection.getInputStream()) {
                    copyBounded(input, archive, MAX_PACKAGE_BYTES, operation, callback, declared);
                } finally {
                    connection.disconnect();
                    operation.connection = null;
                }
                if (archive.length() != declared) throw new IOException("Downloaded package size does not match the server declaration");
                if (expectedSha256 != null && !expectedSha256.equals(sha256(archive))) {
                    throw new IOException("Downloaded model package failed the catalog SHA-256 check");
                }
                model = installArchive(context, archive, operation, callback, declared);
            } catch (Exception error) {
                failure = error;
            } finally {
                HttpURLConnection active = operation.connection;
                if (active != null) active.disconnect();
                archive.delete();
            }
            complete(callback, model, failure);
        });
        return operation;
    }

    private static Model installArchive(Context context, File archive, Operation operation,
            Callback callback, long totalBytes) throws Exception {
        if (operation.cancelled.get()) throw new IOException("Model operation cancelled");
        if (!archive.isFile() || archive.length() <= 0L || archive.length() > MAX_PACKAGE_BYTES) {
            throw new IOException("The selected file is not a supported .ssmodel package");
        }
        File root = modelRoot(context);
        if (!root.isDirectory() && !root.mkdirs()) throw new IOException("Could not create private model storage");
        long stagingBytes = archive.length();
        if (root.getUsableSpace() < MAX_MODEL_BYTES + MAX_TOKENIZER_BYTES + 16L * 1024L * 1024L) {
            throw new IOException("There is not enough free private storage to install this model");
        }
        File staging = new File(root, ".install-" + System.nanoTime());
        if (!staging.mkdir()) throw new IOException("Could not create model staging directory");
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive)) {
            Set<String> seen = new HashSet<>();
            Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !safePath(name) || !seen.add(name)) {
                    throw new IOException("The package contains an unsafe or duplicate path");
                }
                if (!contains(REQUIRED_FILES, name)) throw new IOException("Unexpected package entry: " + name);
            }
            if (seen.size() != REQUIRED_FILES.length || !seen.contains("manifest.json")) {
                throw new IOException("The package is missing required files");
            }
            JSONObject manifest = readManifest(zip.getInputStream(zip.getEntry("manifest.json")));
            validateManifest(manifest);
            JSONArray files = manifest.getJSONArray("files");
            for (int i = 0; i < files.length(); i++) {
                JSONObject file = files.getJSONObject(i);
                String path = file.getString("path");
                long expectedSize = file.getLong("sizeBytes");
                String expectedHash = file.getString("sha256").toLowerCase(Locale.ROOT);
                if (!safeDigest(expectedHash) || expectedSize <= 0L || expectedSize > fileLimit(path)) {
                    throw new IOException("Invalid size or digest in model manifest");
                }
                java.util.zip.ZipEntry entry = zip.getEntry(path);
                if (entry == null || entry.getSize() != expectedSize) {
                    throw new IOException("Model entry size does not match the manifest: " + path);
                }
                File output = child(staging, path);
                File parent = output.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Could not create model staging path");
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                long copied = 0L;
                try (InputStream input = new BufferedInputStream(zip.getInputStream(entry));
                     OutputStream stream = new BufferedOutputStream(new FileOutputStream(output))) {
                    byte[] buffer = new byte[64 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (operation.cancelled.get()) throw new IOException("Model operation cancelled");
                        copied += count;
                        if (copied > expectedSize) throw new IOException("Model entry exceeds its declared size");
                        digest.update(buffer, 0, count);
                        stream.write(buffer, 0, count);
                    }
                }
                String actual = hex(digest.digest());
                if (copied != expectedSize || !actual.equals(expectedHash)) {
                    throw new IOException("Model entry failed its SHA-256 check: " + path);
                }
            }
            try (InputStream input = new BufferedInputStream(zip.getInputStream(zip.getEntry("manifest.json")));
                 OutputStream output = new BufferedOutputStream(new FileOutputStream(new File(staging, "manifest.json")))) {
                copyBounded(input, output, 64 * 1024L, operation);
            }

            String id = manifest.getString("modelId");
            String version = manifest.getString("modelVersion");
            long packageBytes = 0L;
            for (int i = 0; i < files.length(); i++) {
                long size = files.getJSONObject(i).getLong("sizeBytes");
                if (Long.MAX_VALUE - packageBytes < size) throw new IOException("Model package size overflow");
                packageBytes += size;
            }
            Model existing = null;
            String existingKey = id + "/" + version;
            for (Model model : installed(context)) {
                if (model.key().equals(existingKey)) existing = model;
            }
            if (existing != null) {
                deleteRecursively(staging);
                return existing;
            }
            if (installed(context).size() >= MAX_INSTALLED_MODELS) {
                throw new IOException("Delete an installed model before adding another (maximum two)");
            }
            if (installedBytes(context) + packageBytes > DEFAULT_QUOTA_BYTES) {
                throw new IOException("The installed models would exceed the 512 MiB storage limit");
            }
            File modelParent = new File(root, id);
            if (!modelParent.isDirectory() && !modelParent.mkdir()) throw new IOException("Could not create model directory");
            File destination = new File(modelParent, version);
            if (destination.exists()) throw new IOException("That model version is already present but could not be verified");
            if (!staging.renameTo(destination)) throw new IOException("Could not atomically install the verified model");
            return readModel(destination, true);
        } catch (Exception failure) {
            deleteRecursively(staging);
            throw failure;
        }
    }

    private static JSONObject readManifest(InputStream input) throws IOException {
        if (input == null) throw new IOException("Model manifest is missing");
        try (InputStream source = input; java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = source.read(buffer)) != -1) {
                if (output.size() + count > 64 * 1024) throw new IOException("Model manifest is too large");
                output.write(buffer, 0, count);
            }
            return new JSONObject(new String(output.toByteArray(), "UTF-8"));
        } catch (JSONException malformed) {
            throw new IOException("Model manifest is not valid JSON", malformed);
        }
    }

    private static void validateManifest(JSONObject manifest) throws IOException {
        try {
            if (manifest.getInt("schemaVersion") != PACKAGE_SCHEMA
                    || !PROFILE_E5.equals(manifest.getString("engineProfile"))
                    || !"text-embedding".equals(manifest.getString("task"))) {
                throw new IOException("This model package uses an unsupported profile or task");
            }
            safeId(manifest.getString("modelId"));
            safeId(manifest.getString("modelVersion"));
            String license = required(manifest, "licenseSpdx");
            required(manifest, "licenseUrl");
            required(manifest, "sourceRepository");
            required(manifest, "sourceRevision");
            if (manifest.getInt("minimumAndroidApi") > 23
                    || manifest.optLong("maximumPackageBytes", -1L) <= 0L
                    || manifest.optLong("maximumPackageBytes", Long.MAX_VALUE) > MAX_PACKAGE_BYTES
                    || manifest.optLong("memoryEstimateBytes", -1L) <= 0L
                    || manifest.optInt("embeddingDimensions", -1) != 384
                    || !"XLM-Roberta tokenizer via ONNX Runtime Extensions".equals(manifest.getString("tokenizerType"))
                    || manifest.getInt("maximumSequenceLength") != 512
                    || !"query: ".equals(manifest.getString("queryPrefix"))
                    || !"passage: ".equals(manifest.getString("passagePrefix"))
                    || !"mean".equals(manifest.getString("pooling"))
                    || !manifest.getBoolean("l2Normalize")) {
                throw new IOException("The package preprocessing contract is incompatible with this runtime");
            }
            if (license.isEmpty()) throw new IOException("Model license metadata is missing");
            JSONArray files = manifest.getJSONArray("files");
            if (files.length() != 4) throw new IOException("The package must contain exactly four model data files");
            Set<String> paths = new HashSet<>();
            for (int i = 0; i < files.length(); i++) {
                JSONObject file = files.getJSONObject(i);
                String path = file.getString("path");
                if (!contains(REQUIRED_FILES, path) || "manifest.json".equals(path) || !paths.add(path)) {
                    throw new IOException("The manifest contains an unsupported or duplicate file");
                }
                String url = file.optString("sourceUrl", "");
                if (!url.isEmpty()) checkedUrl(url);
            }
            for (String path : REQUIRED_FILES) {
                if (!"manifest.json".equals(path) && !paths.contains(path)) throw new IOException("The manifest is incomplete");
            }
            JSONObject tensors = manifest.getJSONObject("tensorContract");
            if (!"inputs".equals(tensors.getJSONObject("tokenizerInput").getString("name"))
                    || !"string".equals(tensors.getJSONObject("tokenizerInput").getString("dtype"))
                    || !"tokens".equals(tensors.getJSONObject("tokenizerOutput").getString("name"))
                    || !"int64".equals(tensors.getJSONObject("tokenizerOutput").getString("dtype"))
                    || !"last_hidden_state".equals(tensors.getJSONObject("modelOutput").getString("name"))
                    || !"float32".equals(tensors.getJSONObject("modelOutput").getString("dtype"))) {
                throw new IOException("The manifest tensor contract is unsupported");
            }
            JSONArray modelInputs = tensors.getJSONArray("modelInputs");
            Set<String> inputNames = new HashSet<>();
            for (int i = 0; i < modelInputs.length(); i++) {
                JSONObject input = modelInputs.getJSONObject(i);
                if (!"int64".equals(input.getString("dtype"))) throw new IOException("Unsupported model input type");
                inputNames.add(input.getString("name"));
            }
            if (!inputNames.equals(new HashSet<>(java.util.Arrays.asList("input_ids", "attention_mask", "token_type_ids")))) {
                throw new IOException("The manifest model inputs are unsupported");
            }
        } catch (JSONException | IllegalArgumentException malformed) {
            throw new IOException("Model manifest is missing required profile fields", malformed);
        }
    }

    private static boolean isPinnedE5(JSONObject manifest) {
        try {
            if (!"Xenova/multilingual-e5-small".equals(manifest.getString("sourceRepository"))
                    || !"761b726dd34fb83930e26aab4e9ac3899aa1fa78".equals(manifest.getString("sourceRevision"))) return false;
            JSONArray files = manifest.getJSONArray("files");
            boolean model = false;
            boolean tokenizer = false;
            for (int i = 0; i < files.length(); i++) {
                JSONObject entry = files.getJSONObject(i);
                if ("model/model.onnx".equals(entry.getString("path"))) {
                    model = entry.getLong("sizeBytes") == 118_308_185L
                            && "f80102d3f2a1229f387d3c81909990d8945513e347b0eab049f7de3c6f98c193".equals(entry.getString("sha256"));
                }
                if ("tokenizer/tokenizer.onnx".equals(entry.getString("path"))) {
                    tokenizer = entry.getLong("sizeBytes") == 5_069_559L
                            && "d74a9c6240a3a337fabba6acf450707658041036b490b83a219ff77e844a1d45".equals(entry.getString("sha256"));
                }
            }
            return model && tokenizer;
        } catch (JSONException malformed) {
            return false;
        }
    }

    private static Model readModel(File folder, boolean verifyHashes) throws IOException {
        File manifestFile = new File(folder, "manifest.json");
        if (!manifestFile.isFile() || manifestFile.length() > 64 * 1024) throw new IOException("Model manifest is unavailable");
        try (InputStream input = new FileInputStream(manifestFile)) {
            JSONObject manifest = readManifest(input);
            validateManifest(manifest);
            return new Model(manifest, folder, verifyHashes);
        }
    }

    private static File modelRoot(Context context) {
        Context app = context.getApplicationContext();
        return new File(app == null ? context.getFilesDir() : app.getFilesDir(), ROOT);
    }

    private static SharedPreferences preferences(Context context) {
        Context app = context.getApplicationContext();
        return (app == null ? context : app).getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static File child(File root, String relative) throws IOException {
        if (!safePath(relative)) throw new IOException("Unsafe model path");
        File file = new File(root, relative);
        String base = root.getCanonicalPath() + File.separator;
        if (!file.getCanonicalPath().startsWith(base)) throw new IOException("Model path escapes its package directory");
        return file;
    }

    private static String required(JSONObject object, String key) throws IOException {
        String value = object.optString(key, "").trim();
        if (value.isEmpty() || "null".equals(value)) throw new IOException("Model manifest field is missing: " + key);
        return value;
    }

    private static String safeId(String value) throws IOException {
        if (value == null || !value.matches("[A-Za-z0-9._-]{1,64}") || value.equals(".") || value.equals("..")) {
            throw new IOException("Model ID or version is invalid");
        }
        return value;
    }

    private static boolean safePath(String path) {
        return path != null && path.length() <= 128 && !path.startsWith("/")
                && !path.contains("\\") && !path.contains(":")
                && !path.contains("..") && path.matches("[A-Za-z0-9._/-]+");
    }

    private static long fileLimit(String path) {
        if ("model/model.onnx".equals(path)) return MAX_MODEL_BYTES;
        if ("tokenizer/tokenizer.onnx".equals(path)) return MAX_TOKENIZER_BYTES;
        return 256L * 1024L;
    }

    private static boolean contains(String[] values, String target) {
        for (String value : values) if (value.equals(target)) return true;
        return false;
    }

    private static boolean safeDigest(String value) { return value != null && value.matches("[0-9a-f]{64}"); }

    private static String sha256(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            return hex(digest.digest());
        } catch (java.security.GeneralSecurityException unavailable) {
            throw new IOException("SHA-256 is unavailable", unavailable);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder output = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) output.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return output.toString();
    }

    private static URL checkedUrl(String text) throws IOException {
        try {
            URL url = new URL(text);
            if (!"https".equalsIgnoreCase(url.getProtocol()) || url.getHost() == null
                    || url.getHost().isEmpty() || url.getUserInfo() != null || url.getRef() != null) {
                throw new IOException("Only credential-free HTTPS model sources are allowed");
            }
            return url;
        } catch (java.net.MalformedURLException malformed) {
            throw new IOException("Model source URL is invalid", malformed);
        }
    }

    private static String requireHttps(String text) throws IOException {
        URL url = checkedUrl(text);
        return url.toString();
    }

    private static HttpURLConnection openFollowingHttps(URL initial, Operation operation,
            Set<String> allowedHosts, String acceptHeader) throws IOException {
        URL next = initial;
        for (int redirects = 0; redirects <= 5; redirects++) {
            if (allowedHosts != null && !allowedHosts.contains(next.getHost().toLowerCase(Locale.ROOT))) {
                throw new IOException("Catalog or official release redirected to an unapproved HTTPS host");
            }
            HttpURLConnection connection = (HttpURLConnection) next.openConnection();
            operation.connection = connection;
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setConnectTimeout(20_000);
            connection.setReadTimeout(30_000);
            connection.setRequestProperty("Accept", acceptHeader);
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP
                    || code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null) throw new IOException("Model source returned a redirect without a destination");
                next = checkedUrl(new URL(next, location).toString());
                continue;
            }
            return connection;
        }
        throw new IOException("Model source redirected too many times");
    }

    private static byte[] readBounded(InputStream input, int maxBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (output.size() + count > maxBytes) throw new IOException("Catalog response exceeds its size limit");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static String joinStrings(JSONArray values) throws JSONException, IOException {
        if (values.length() == 0 || values.length() > 32) throw new IOException("Catalog language metadata is invalid");
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < values.length(); i++) {
            String value = values.getString(i).trim();
            if (value.isEmpty() || value.length() > 80) throw new IOException("Catalog language entry is invalid");
            if (joined.length() > 0) joined.append(", ");
            joined.append(value);
        }
        return joined.toString();
    }

    private static boolean versionAtMost(String candidate, String supported) {
        if (!candidate.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) return false;
        String[] left = candidate.split("\\.");
        String[] right = supported.split("\\.");
        for (int i = 0; i < 3; i++) {
            int comparison = Integer.compare(Integer.parseInt(left[i]), Integer.parseInt(right[i]));
            if (comparison != 0) return comparison < 0;
        }
        return true;
    }

    private static void copyBounded(InputStream input, File destination, long maxBytes,
            Operation operation, Callback callback, long total) throws IOException {
        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(destination))) {
            copyBounded(input, output, maxBytes, operation, callback, total);
        }
    }

    private static void copyBounded(InputStream input, OutputStream output, long maxBytes,
            Operation operation) throws IOException {
        copyBounded(input, output, maxBytes, operation, null, -1L);
    }

    private static void copyBounded(InputStream input, OutputStream output, long maxBytes,
            Operation operation, Callback callback, long total) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long copied = 0L;
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (operation.cancelled.get()) throw new IOException("Model operation cancelled");
            copied += count;
            if (copied > maxBytes) throw new IOException("Model package exceeds the download limit");
            output.write(buffer, 0, count);
            if (callback != null && (copied % (1024L * 1024L) < count)) callback.onProgress(copied, total);
        }
    }

    private static boolean deleteRecursively(File file) {
        if (file == null || !file.exists()) return true;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) if (!deleteRecursively(child)) return false;
        return file.delete();
    }

    private static void complete(Callback callback, Model model, Exception failure) {
        if (callback != null) callback.onComplete(model, failure);
    }
}
