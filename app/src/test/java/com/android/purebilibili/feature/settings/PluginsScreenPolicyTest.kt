package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.plugin.ExternalPluginInstallDecision
import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import com.android.purebilibili.core.plugin.kotlinpkg.ExternalKotlinPluginPayloadEntry
import com.android.purebilibili.core.plugin.kotlinpkg.ExternalKotlinPluginPayloadType
import com.android.purebilibili.core.plugin.kotlinpkg.InstalledExternalPluginPackage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PluginsScreenPolicyTest {

    @Test
    fun sponsorBlockToggle_routesThroughSettingsPath() = runTest {
        var sponsorBlockToggleCount = 0
        var genericToggleCount = 0

        dispatchBuiltInPluginToggle(
            pluginId = "sponsor_block",
            enabled = true,
            onSponsorBlockToggle = { sponsorBlockToggleCount += 1 },
            onGenericPluginToggle = { _, _ -> genericToggleCount += 1 }
        )

        assertEquals(1, sponsorBlockToggleCount)
        assertEquals(0, genericToggleCount)
    }

    @Test
    fun nonSponsorBlockToggle_routesThroughGenericPluginPath() = runTest {
        var sponsorBlockToggleCount = 0
        var genericToggleCount = 0

        dispatchBuiltInPluginToggle(
            pluginId = "danmaku_enhance",
            enabled = false,
            onSponsorBlockToggle = { sponsorBlockToggleCount += 1 },
            onGenericPluginToggle = { _, _ -> genericToggleCount += 1 }
        )

        assertEquals(0, sponsorBlockToggleCount)
        assertEquals(1, genericToggleCount)
    }

    @Test
    fun capabilityModels_markNetworkAndStorageAsSensitiveAuthorizationItems() {
        val models = resolvePluginCapabilityUiModels(
            setOf(
                PluginCapability.RECOMMENDATION_CANDIDATES,
                PluginCapability.NETWORK,
                PluginCapability.PLUGIN_STORAGE,
                PluginCapability.EXTERNAL_MEDIA_PLAYBACK
            )
        )

        assertEquals(listOf("推荐候选", "网络访问", "插件存储", "外部媒体播放"), models.map { it.label })
        assertTrue(models.first { it.capability == PluginCapability.NETWORK }.requiresExplicitApproval)
        assertTrue(models.first { it.capability == PluginCapability.PLUGIN_STORAGE }.requiresExplicitApproval)
        assertTrue(models.first { it.capability == PluginCapability.EXTERNAL_MEDIA_PLAYBACK }.requiresExplicitApproval)
    }

    @Test
    fun externalInstallPreview_exposesHashSignerAndSensitiveCapabilities() {
        val manifest = PluginCapabilityManifest(
            pluginId = "dev.example.cloud",
            displayName = "云推荐",
            version = "1.0.0",
            apiVersion = 1,
            entryClassName = "dev.example.CloudPlugin",
            capabilities = setOf(PluginCapability.NETWORK, PluginCapability.LOCAL_HISTORY_READ)
        )
        val preview = buildExternalPluginInstallPreview(
            ExternalPluginInstallDecision.RequiresUserApproval(
                manifest = manifest,
                packageSha256 = "abcdef123456",
                signerTrusted = false,
                sensitiveCapabilities = setOf(PluginCapability.NETWORK, PluginCapability.LOCAL_HISTORY_READ)
            )
        )

        assertEquals("云推荐", preview.title)
        assertEquals("SHA-256: abcdef123456", preview.packageHashText)
        assertEquals("签名未信任", preview.signerText)
        assertEquals(listOf("观看历史", "网络访问"), preview.sensitiveCapabilityLabels)
    }

    @Test
    fun jsonRulePluginType_mapsToHostCapabilities() {
        assertEquals(
            setOf(PluginCapability.RECOMMENDATION_CANDIDATES),
            resolveJsonRulePluginCapabilities("feed")
        )
        assertEquals(
            setOf(PluginCapability.DANMAKU_STREAM, PluginCapability.DANMAKU_MUTATION),
            resolveJsonRulePluginCapabilities("danmaku")
        )
    }

    @Test
    fun externalPluginPayloadSummary_prefersConcretePayloadTypesOverDexOnlyText() {
        val summary = buildExternalPluginPayloadSummary(
            listOf(
                ExternalKotlinPluginPayloadEntry(
                    path = "classes.jar",
                    type = ExternalKotlinPluginPayloadType.CLASSES_JAR,
                    sizeBytes = 12
                ),
                ExternalKotlinPluginPayloadEntry(
                    path = "assets/compass.json",
                    type = ExternalKotlinPluginPayloadType.OTHER,
                    sizeBytes = 24
                )
            )
        )

        assertEquals("载荷：classes.jar、其他文件 1 个，当前不执行", summary)
    }

    @Test
    fun installedExternalPackageUi_exposesDisabledNonExecutingStateAndAuthorizationHash() {
        val installed = InstalledExternalPluginPackage(
            manifest = PluginCapabilityManifest(
                pluginId = "dev.example.cloud",
                displayName = "云推荐",
                version = "1.0.0",
                apiVersion = 1,
                entryClassName = "dev.example.CloudPlugin",
                capabilities = setOf(
                    PluginCapability.RECOMMENDATION_CANDIDATES,
                    PluginCapability.NETWORK
                )
            ),
            packageSha256 = "abcdef123456",
            signerSha256 = "trusted",
            grantedCapabilities = setOf(PluginCapability.RECOMMENDATION_CANDIDATES),
            packagePath = "/tmp/cloud.bpplugin",
            installedAtMillis = 1234L,
            enabled = false
        )

        val uiModel = buildInstalledExternalPluginUiModels(listOf(installed)).single()

        assertEquals("云推荐", uiModel.title)
        assertEquals("dev.example.cloud · v1.0.0", uiModel.subtitle)
        assertEquals("已保存，暂不运行", uiModel.stateText)
        assertEquals("SHA-256: abcdef123456", uiModel.packageHashText)
        assertEquals(listOf("推荐候选"), uiModel.grantedCapabilityLabels)
    }

}
