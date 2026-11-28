package app.morphe.patches.tiktok.misc.settings

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import org.w3c.dom.Element

internal const val STOP_SLOP_AI_SERVICE =
    "app.morphe.extension.tiktok.feedfilter.StopSlopAiService"

/** Adds only the private process endpoint; it starts on explicit binding, never from app startup. */
internal val stopSlopAiServiceManifestPatch = patch {
    execute {
        document("AndroidManifest.xml").use { xml ->
            val application = xml.getElementsByTagName("application").item(0) as? Element
                ?: throw PatchException("StopSlop AI: the manifest has no application")
            val services = xml.getElementsByTagName("service")
            val existing = (0 until services.length)
                .map { services.item(it) as Element }
                .firstOrNull { it.getAttribute("android:name") == STOP_SLOP_AI_SERVICE }
            if (existing != null) {
                if (existing.getAttribute("android:process") != ":stopslop_ai"
                    || existing.getAttribute("android:exported") != "false") {
                    throw PatchException("StopSlop AI: an existing service declaration is not isolated")
                }
                return@use
            }

            val service = xml.createElement("service")
            service.setAttribute("android:name", STOP_SLOP_AI_SERVICE)
            service.setAttribute("android:process", ":stopslop_ai")
            service.setAttribute("android:exported", "false")
            application.appendChild(service)
        }
    }
}
