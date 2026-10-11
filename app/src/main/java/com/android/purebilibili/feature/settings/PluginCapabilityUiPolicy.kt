package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.plugin.ExternalPluginInstallDecision
import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.kotlinpkg.ExternalKotlinPluginPayloadEntry
import com.android.purebilibili.core.plugin.kotlinpkg.ExternalKotlinPluginPayloadType
import com.android.purebilibili.core.plugin.kotlinpkg.InstalledExternalPluginPackage

data class PluginCapabilityUiModel(
    val capability: PluginCapability,
    val label: String,
    val description: String,
    val requiresExplicitApproval: Boolean
)

data class ExternalPluginInstallPreviewUiModel(
    val title: String,
    val subtitle: String,
    val packageHashText: String,
    val signerText: String,
    val sensitiveCapabilityLabels: List<String>
)

data class InstalledExternalPluginPackageUiModel(
    val title: String,
    val subtitle: String,
    val stateText: String,
    val packageHashText: String,
    val signerText: String,
    val grantedCapabilityLabels: List<String>
)

private val capabilityOrder = listOf(
    PluginCapability.PLAYER_STATE,
    PluginCapability.PLAYER_CONTROL,
    PluginCapability.PLAYBACK_CDN,
    PluginCapability.DANMAKU_STREAM,
    PluginCapability.DANMAKU_MUTATION,
    PluginCapability.RECOMMENDATION_CANDIDATES,
    PluginCapability.LOCAL_HISTORY_READ,
    PluginCapability.LOCAL_FEEDBACK_READ,
    PluginCapability.NETWORK,
    PluginCapability.PLUGIN_STORAGE,
    PluginCapability.EXTERNAL_MEDIA_PLAYBACK
)

private val explicitApprovalCapabilities = setOf(
    PluginCapability.PLAYER_CONTROL,
    PluginCapability.PLAYBACK_CDN,
    PluginCapability.DANMAKU_MUTATION,
    PluginCapability.LOCAL_HISTORY_READ,
    PluginCapability.LOCAL_FEEDBACK_READ,
    PluginCapability.NETWORK,
    PluginCapability.PLUGIN_STORAGE,
    PluginCapability.EXTERNAL_MEDIA_PLAYBACK
)

fun resolvePluginCapabilityUiModels(
    capabilities: Set<PluginCapability>
): List<PluginCapabilityUiModel> {
    return capabilities
        .sortedBy { capabilityOrder.indexOf(it).let { index -> if (index >= 0) index else Int.MAX_VALUE } }
        .map { capability ->
            PluginCapabilityUiModel(
                capability = capability,
                label = capability.label,
                description = capability.description,
                requiresExplicitApproval = capability in explicitApprovalCapabilities
            )
        }
}

fun resolveJsonRulePluginCapabilities(type: String): Set<PluginCapability> {
    return when (type) {
        "feed" -> setOf(PluginCapability.RECOMMENDATION_CANDIDATES)
        "danmaku" -> setOf(
            PluginCapability.DANMAKU_STREAM,
            PluginCapability.DANMAKU_MUTATION
        )
        else -> emptySet()
    }
}

fun buildExternalPluginInstallPreview(
    decision: ExternalPluginInstallDecision
): ExternalPluginInstallPreviewUiModel {
    return when (decision) {
        is ExternalPluginInstallDecision.RequiresUserApproval -> ExternalPluginInstallPreviewUiModel(
            title = decision.manifest.displayName,
            subtitle = "${decision.manifest.pluginId} · v${decision.manifest.version}",
            packageHashText = "SHA-256: ${decision.packageSha256}",
            signerText = if (decision.signerTrusted) "签名可信" else "签名未信任",
            sensitiveCapabilityLabels = resolvePluginCapabilityUiModels(decision.sensitiveCapabilities)
                .map { it.label }
        )
        is ExternalPluginInstallDecision.Rejected -> ExternalPluginInstallPreviewUiModel(
            title = "插件不可安装",
            subtitle = decision.reason,
            packageHashText = "",
            signerText = "已拒绝",
            sensitiveCapabilityLabels = emptyList()
        )
    }
}

fun buildExternalPluginPayloadSummary(
    payloadEntries: List<ExternalKotlinPluginPayloadEntry>
): String {
    if (payloadEntries.isEmpty()) return "载荷：无，当前不执行"

    val labels = buildList {
        if (payloadEntries.any { it.type == ExternalKotlinPluginPayloadType.CLASSES_JAR }) {
            add("classes.jar")
        }
        if (payloadEntries.any { it.type == ExternalKotlinPluginPayloadType.CLASSES_DEX }) {
            add("classes.dex")
        }
        val otherCount = payloadEntries.count { it.type == ExternalKotlinPluginPayloadType.OTHER }
        if (otherCount > 0) {
            add("其他文件 $otherCount 个")
        }
    }
    return "载荷：${labels.joinToString("、")}，当前不执行"
}

fun buildInstalledExternalPluginUiModels(
    installedPackages: List<InstalledExternalPluginPackage>
): List<InstalledExternalPluginPackageUiModel> {
    return installedPackages
        .sortedBy { it.manifest.displayName }
        .map { installed ->
            InstalledExternalPluginPackageUiModel(
                title = installed.manifest.displayName,
                subtitle = "${installed.manifest.pluginId} · v${installed.manifest.version}",
                stateText = if (installed.enabled) "已启用" else "已保存，暂不运行",
                packageHashText = "SHA-256: ${installed.packageSha256}",
                signerText = if (installed.signerSha256.isNullOrBlank()) "签名未信任" else "签名可信",
                grantedCapabilityLabels = resolvePluginCapabilityUiModels(installed.grantedCapabilities)
                    .map { it.label }
            )
        }
}

private val PluginCapability.label: String
    get() = when (this) {
        PluginCapability.PLAYER_STATE -> "播放器状态"
        PluginCapability.PLAYER_CONTROL -> "播放器控制"
        PluginCapability.PLAYBACK_CDN -> "播放 CDN"
        PluginCapability.DANMAKU_STREAM -> "弹幕流"
        PluginCapability.DANMAKU_MUTATION -> "弹幕改写"
        PluginCapability.RECOMMENDATION_CANDIDATES -> "推荐候选"
        PluginCapability.LOCAL_HISTORY_READ -> "观看历史"
        PluginCapability.LOCAL_FEEDBACK_READ -> "本地反馈"
        PluginCapability.NETWORK -> "网络访问"
        PluginCapability.PLUGIN_STORAGE -> "插件存储"
        PluginCapability.EXTERNAL_MEDIA_PLAYBACK -> "外部媒体播放"
        PluginCapability.FEED_SOURCE -> "订阅源"
    }

private val PluginCapability.description: String
    get() = when (this) {
        PluginCapability.PLAYER_STATE -> "读取当前播放位置、视频标识与播放状态"
        PluginCapability.PLAYER_CONTROL -> "控制跳转、跳过片段或调整播放行为"
        PluginCapability.PLAYBACK_CDN -> "读取和调整播放地址候选线路"
        PluginCapability.DANMAKU_STREAM -> "读取当前视频弹幕内容"
        PluginCapability.DANMAKU_MUTATION -> "过滤、高亮或改写弹幕显示"
        PluginCapability.RECOMMENDATION_CANDIDATES -> "读取首页候选内容用于排序或筛选"
        PluginCapability.LOCAL_HISTORY_READ -> "读取本地历史摘要和偏好画像"
        PluginCapability.LOCAL_FEEDBACK_READ -> "读取不感兴趣等本地反馈信号"
        PluginCapability.NETWORK -> "访问网络获取远程数据或服务"
        PluginCapability.PLUGIN_STORAGE -> "读写插件自己的本地配置或缓存"
        PluginCapability.EXTERNAL_MEDIA_PLAYBACK -> "播放插件返回的外部媒体链接"
        PluginCapability.FEED_SOURCE -> "声明 RSS 或 Atom 订阅地址，由应用解析并在首页展示"
    }
