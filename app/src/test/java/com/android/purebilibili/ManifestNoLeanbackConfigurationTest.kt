package com.android.purebilibili

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManifestNoLeanbackConfigurationTest {

    @Test
    fun manifest_does_not_declare_leanback_feature() {
        val manifest = loadManifestText()

        assertFalse(
            manifest.contains("""android.software.leanback"""),
            "AndroidManifest should not declare android.software.leanback in mobile/tablet repo"
        )
        assertTrue(
            manifest.contains("""android.hardware.touchscreen""") &&
                manifest.contains("""android:required="false""""),
            "AndroidManifest should keep touchscreen optional for broad device compatibility"
        )
    }

    @Test
    fun manifest_does_not_expose_leanback_launcher_or_banner() {
        val manifest = loadManifestText()

        assertFalse(
            manifest.contains("""android.intent.category.LEANBACK_LAUNCHER"""),
            "AndroidManifest should not expose LEANBACK_LAUNCHER entry in mobile/tablet repo"
        )
        assertFalse(
            manifest.contains("""android:banner="""),
            "AndroidManifest application should not define TV banner in mobile/tablet repo"
        )
    }

    private fun loadManifestText(): String {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        )
        val manifestFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate AndroidManifest.xml from ${File(".").absolutePath}")
        return manifestFile.readText()
    }
}
