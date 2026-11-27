/*
 * Copyright 2026 Hushfeed contributors
 * https://github.com/SysAdminDoc/hushfeed
 *
 * Built on icysymmetra/tiktok-patches-for-morphe (GPL-3.0).
 */
package app.morphe.patches.tiktok.misc.settings

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import java.io.File
import java.io.IOException
import java.security.MessageDigest

private data class StopSlopPackagedFile(
    val resourcePath: String,
    val apkPath: String,
    val size: Long,
    val sha256: String,
)

private object StopSlopModelAssetResources {
    private val files = listOf(
        StopSlopPackagedFile(
            "stop_slop/native/arm64-v8a/libonnxruntime.so",
            "lib/arm64-v8a/libonnxruntime.so",
            17_571_160L,
            "52329b7c8e5fcd5c1aa88b01093215faaef362e3f3d0e374cb8b10d1e4704677",
        ),
        StopSlopPackagedFile(
            "stop_slop/native/arm64-v8a/libonnxruntime4j_jni.so",
            "lib/arm64-v8a/libonnxruntime4j_jni.so",
            915_968L,
            "ac94a0df9d6f2e4eb2a18723807e4cb2e38b5a78bceb9c367bcbf84633171649",
        ),
        StopSlopPackagedFile(
            "stop_slop/native/arm64-v8a/libonnxruntime_extensions4j_jni.so",
            "lib/arm64-v8a/libonnxruntime_extensions4j_jni.so",
            3_762_192L,
            "ca456c2a270fb1ead267a6e3cb7f68d46c30847c34445147c65b1d531b6ee22b",
        ),
        StopSlopPackagedFile(
            "stop_slop/native/arm64-v8a/libortextensions.so",
            "lib/arm64-v8a/libortextensions.so",
            2_373_256L,
            "f38ca9e042624ed0d44e6e537e75ef6e2604574e56435d84655162f52f1c3be7",
        ),
        StopSlopPackagedFile(
            "stop_slop/native/armeabi-v7a/libonnxruntime.so",
            "lib/armeabi-v7a/libonnxruntime.so",
            12_294_888L,
            "634fc9868e3ac188f78feb75c9fd8bfe6cb5f52bda84df0863fbaebe083e6350",
        ),
        StopSlopPackagedFile(
            "stop_slop/native/armeabi-v7a/libonnxruntime4j_jni.so",
            "lib/armeabi-v7a/libonnxruntime4j_jni.so",
            778_216L,
            "bd95ce75c1e0ec91b563c9953b52b595a2edfc5fb4ca6e5f0ca30f0935824838",
        ),
        StopSlopPackagedFile(
            "stop_slop/native/armeabi-v7a/libonnxruntime_extensions4j_jni.so",
            "lib/armeabi-v7a/libonnxruntime_extensions4j_jni.so",
            2_603_328L,
            "a128accb1d7ae544e30627731ee9434f6ab9b968e49f72ee5718757fe1fd9525",
        ),
        StopSlopPackagedFile(
            "stop_slop/native/armeabi-v7a/libortextensions.so",
            "lib/armeabi-v7a/libortextensions.so",
            1_554_004L,
            "6da6e72cd1c4b12685c59db243897491fe8089947b21800683a730bc16fe3d53",
        ),
    )

    fun copyIntoApk(apkPath: String, destination: File, expectedSize: Long, expectedHash: String) {
        val file = files.singleOrNull { it.apkPath == apkPath }
            ?: throw PatchException("Unknown StopSlop model resource path: $apkPath")
        if (file.size != expectedSize || file.sha256 != expectedHash) {
            throw PatchException("StopSlop model resource metadata changed without review")
        }
        val input = javaClass.classLoader.getResourceAsStream(file.resourcePath)
            ?: throw PatchException("StopSlop model resource is missing: ${file.resourcePath}")
        val digest = MessageDigest.getInstance("SHA-256")
        var copied = 0L
        try {
            input.use { source ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        copied += read
                        if (copied > file.size) throw IOException("Packaged file exceeds its pinned size")
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            }
        } catch (failure: IOException) {
            destination.delete()
            throw PatchException("Could not copy StopSlop model resource: ${file.resourcePath}", failure)
        }
        val actualHash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        if (copied != file.size || actualHash != file.sha256) {
            destination.delete()
            throw PatchException("StopSlop model resource failed its size or SHA-256 check: ${file.resourcePath}")
        }
    }
}

/** Adds only the pinned native runtime; user-selected model data stays outside the APK. */
val stopSlopModelAssetsPatch = rawResourcePatch(
    name = "StopSlop local semantic model assets",
    description = "Packages only the ONNX CPU runtime for optional, privately installed E5 models.",
    default = false,
) {
    execute {
        val entries = listOf(
            "lib/arm64-v8a/libonnxruntime.so",
            "lib/arm64-v8a/libonnxruntime4j_jni.so",
            "lib/arm64-v8a/libonnxruntime_extensions4j_jni.so",
            "lib/arm64-v8a/libortextensions.so",
            "lib/armeabi-v7a/libonnxruntime.so",
            "lib/armeabi-v7a/libonnxruntime4j_jni.so",
            "lib/armeabi-v7a/libonnxruntime_extensions4j_jni.so",
            "lib/armeabi-v7a/libortextensions.so",
        )
        for (apkPath in entries) {
            val expected = when (apkPath) {
                "lib/arm64-v8a/libonnxruntime.so" -> 17_571_160L to
                    "52329b7c8e5fcd5c1aa88b01093215faaef362e3f3d0e374cb8b10d1e4704677"
                "lib/arm64-v8a/libonnxruntime4j_jni.so" -> 915_968L to
                    "ac94a0df9d6f2e4eb2a18723807e4cb2e38b5a78bceb9c367bcbf84633171649"
                "lib/arm64-v8a/libonnxruntime_extensions4j_jni.so" -> 3_762_192L to
                    "ca456c2a270fb1ead267a6e3cb7f68d46c30847c34445147c65b1d531b6ee22b"
                "lib/arm64-v8a/libortextensions.so" -> 2_373_256L to
                    "f38ca9e042624ed0d44e6e537e75ef6e2604574e56435d84655162f52f1c3be7"
                "lib/armeabi-v7a/libonnxruntime.so" -> 12_294_888L to
                    "634fc9868e3ac188f78feb75c9fd8bfe6cb5f52bda84df0863fbaebe083e6350"
                "lib/armeabi-v7a/libonnxruntime4j_jni.so" -> 778_216L to
                    "bd95ce75c1e0ec91b563c9953b52b595a2edfc5fb4ca6e5f0ca30f0935824838"
                "lib/armeabi-v7a/libonnxruntime_extensions4j_jni.so" -> 2_603_328L to
                    "a128accb1d7ae544e30627731ee9434f6ab9b968e49f72ee5718757fe1fd9525"
                "lib/armeabi-v7a/libortextensions.so" -> 1_554_004L to
                    "6da6e72cd1c4b12685c59db243897491fe8089947b21800683a730bc16fe3d53"
                else -> throw PatchException("Unknown StopSlop runtime library: $apkPath")
            }
            val destination = get(apkPath, false)
            StopSlopModelAssetResources.copyIntoApk(apkPath, destination, expected.first, expected.second)
        }
    }
}
