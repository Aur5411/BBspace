// 文件路径: data/model/animeko/AniMediaSource.kt
//
// 「换源」模块的数据源抽象。
//
// 参考 animeko 的 MediaSource / MediaSourceSelector 设计, 但做了大幅精简:
// animeko 的多源体系包含 BT(种子) / 在线(web) / Jellyfin / Emby / 自定义 JS 源,
// 本项目保留其中对追番最有价值的三类:
//   1. 在线源  : 直接给出可播放的 m3u8 / mp4 地址 (最常见的「换源」诉求)
//   2. BT 源   : 通过蜜柑计划(Mikan) 等站点按番剧/集数定位种子 (真机可达)
//   3. 自定义源: 用户填写 URL 模板 / JSON 规则, 无限扩展
//
// 所有源只描述「怎么拿到资源」, 不参与播放器实现。
package com.android.purebilibili.data.model.animeko

import kotlinx.serialization.Serializable

/** 数据源类别, 决定 UI 上分组与能力提示。 */
enum class AniMediaSourceKind(val label: String) {
    /** 在线直链源: 给出 m3u8 / mp4 等可直接播放的地址。 */
    ONLINE("在线源"),

    /** BT / 种子源 (走蜜柑等站点)。 */
    BT("BT 源"),

    /** 用户自定义源。 */
    CUSTOM("自定义源"),

    /** 本地缓存 (已下载到本机)。 */
    LOCAL("本地缓存"),
}

/** 单个剧集的候选资源。 */
@Serializable
data class AniMediaCandidate(
    val sourceId: String = "",
    val sourceName: String = "",
    val kind: String = AniMediaSourceKind.ONLINE.name,
    /** 播放地址 (在线源为直链; BT 源为磁力/种子地址)。 */
    val url: String = "",
    /** 展示用标题, 如 "[Lilith-Raws] 孤独摇滚 - 01 [1080p]"。 */
    val title: String = "",
    /** 清晰度/分辨率描述, 如 "1080p"、"Baha 1080P"。 */
    val quality: String = "",
    /** 字幕组/发布组。 */
    val subtitleGroup: String = "",
    /** 文件大小(字节), 0 表示未知。 */
    val sizeBytes: Long = 0L,
    /** 做种数/热度, 用于排序; -1 表示未知。 */
    val seeders: Int = -1,
) {
    val sizeLabel: String
        get() = when {
            sizeBytes <= 0L -> ""
            sizeBytes >= 1024L * 1024L * 1024L ->
                String.format("%.2f GB", sizeBytes / 1024.0 / 1024.0 / 1024.0)
            sizeBytes >= 1024L * 1024L -> String.format("%.0f MB", sizeBytes / 1024.0 / 1024.0)
            else -> String.format("%.0f KB", sizeBytes / 1024.0)
        }

    /**
     * 这个候选能否交给播放器直接播放。
     *
     * 在线源给的是 m3u8/mp4 直链，可以；BT 源给的是 `.torrent` 或磁力链接，
     * ExoPlayer 无法直接播放，必须交给外部 BT 客户端。
     * 自动播放与手动换源都以此为准，避免把种子地址塞给播放器后一直黑屏。
     */
    val playableDirectly: Boolean
        get() {
            val k = runCatching { AniMediaSourceKind.valueOf(kind) }
                .getOrDefault(AniMediaSourceKind.ONLINE)
            // 本地缓存: file:// 或 content://(MediaStore, 公共下载目录) 直链, ExoPlayer 均可直接播
            if (k == AniMediaSourceKind.LOCAL) {
                return url.startsWith("file://") || url.startsWith("content://")
            }
            if (!url.startsWith("http", ignoreCase = true)) return false
            if (url.lowercase().contains(".torrent")) return false
            return k == AniMediaSourceKind.ONLINE
        }
}

/** URL 是否指向真正的媒体文件（m3u8 播放列表 / mp4 / flv 直链）。 */
val AniMediaCandidate.isDirectMediaUrl: Boolean
    get() {
        val path = url.substringBefore('?').lowercase()
        return path.endsWith(".m3u8") || path.endsWith(".mp4") || path.endsWith(".flv")
    }

/** URL 是否疑似网页/跳转地址（下载应避开——下回去是几 KB 的 HTML）。 */
val AniMediaCandidate.isProbablyWebPage: Boolean
    get() {
        if (isDirectMediaUrl) return false
        val path = url.substringBefore('?').lowercase()
        return path.endsWith(".html") || path.endsWith(".htm") ||
            path.endsWith(".php") || path.endsWith(".shtml") ||
            path.endsWith(".torrent")
    }

/**
 * 一个可用的数据源 (在「换源」面板里作为一行展示)。
 */
@Serializable
data class AniMediaSource(
    val id: String,
    val name: String,
    val kind: String = AniMediaSourceKind.ONLINE.name,
    /** 是否默认启用。 */
    val enabled: Boolean = true,
    /** 是否是用户自建源 (可删除/编辑)。 */
    val custom: Boolean = false,
    /** 站点主页, 用于「打开来源」与署名。 */
    val homepage: String = "",
    /**
     * URL 模板, 支持占位符:
     *   {bangumiId} 条目 id
     *   {episodeSort} 集数, 如 "1"、"12.5"
     *   {title} 条目标题 (已 URL 编码)
     * 目前用于自定义源与 BT 源。
     */
    val urlTemplate: String = "",
    /** 备注 / 使用提示。 */
    val note: String = "",
) {
    val kindEnum: AniMediaSourceKind
        get() = runCatching { AniMediaSourceKind.valueOf(kind) }.getOrDefault(AniMediaSourceKind.ONLINE)
}

/**
 * 「换源」的判定依据 —— 描述「要找哪一集」, 与具体源解耦。
 */
data class AniMediaQuery(
    val bangumiId: Long,
    val subjectName: String,
    val episodeSort: String,
    val episodeName: String = "",
    val episodeId: Long = 0L,
    /** 蜜柑番剧 id (来自季度番表), 有则 BT 源定位更准。 */
    val mikanId: Int? = null,
    /** 已配置的数据源列表。 */
    val sources: List<AniMediaSource> = emptyList(),
)

/**
 * 内置数据源清单。
 *
 * 说明: 这些站点的可达性取决于用户所在网络环境; 本模块只负责「按规则拼请求 / 解析结果」,
 * 不做站点鉴权绕过, 也内置了自定义源以满足用户自行扩展。
 */
object AniBuiltinSources {

    const val ID_BT_MIKAN = "bt_mikan"
    const val ID_LOCAL_CACHE = "local_cache"

    val builtins: List<AniMediaSource> =
        // ---- 在线源：maccms 采集接口，直接给 m3u8 直链，可被播放器直接播放 ----
        AniOnlineCatalog.sources.map { catalog ->
            AniMediaSource(
                id = catalog.id,
                name = "${catalog.name}(在线)",
                kind = AniMediaSourceKind.ONLINE.name,
                homepage = catalog.baseUrl,
                // 必须用不编码的模板，否则 {title} 会被编码掉导致替换失败
                urlTemplate = AniOnlineCatalog.detailUrlTemplate(catalog.baseUrl),
                note = "按番剧名检索在线播放地址，直接返回 m3u8 直链",
            )
        } +
            // ---- 在线源：web 站点(来自 animeko 社区 css1.json), 三跳抓取直链 ----
            AniWebSourceCatalog.sources.map { web ->
                AniMediaSource(
                    id = web.id,
                    name = "${web.name}(网页)",
                    kind = AniMediaSourceKind.ONLINE.name,
                    homepage = web.homepage,
                    urlTemplate = web.searchUrl,
                    note = "网页源: 搜索页→详情→播放页三跳抓直链; 播放前会先测速, 快的站优先",
                )
            } + listOf(
                // ---- BT 源：只有种子/磁力，需要外部 BT 客户端，播放器不能直链播放 ----
                AniMediaSource(
                    id = ID_BT_MIKAN,
                    name = "蜜柑计划(BT)",
                    kind = AniMediaSourceKind.BT.name,
                    homepage = "https://mikanime.tv",
                    // ★ 蜜柑主域 mikanani.me 在部分网络下不可达，改用同站镜像域。
                    urlTemplate = "https://mikanime.tv/RSS/Search?searchstr={title}",
                    note = "按番剧名搜索种子；BT 资源需交给外部 BT 客户端下载后播放",
                ),
                AniMediaSource(
                    id = ID_LOCAL_CACHE,
                    name = "本地缓存",
                    kind = AniMediaSourceKind.LOCAL.name,
                    homepage = "",
                    note = "已缓存的集数优先本地播放, 无需联网",
                ),
            ) +
                // ---- BT 源: animeko 社区 bt1.json 的聚合站(nyaa / AnimeGarden) ----
                AniWebSourceCatalog.btSources

    /** 默认启用的源 id 集合（BT 源默认开启，会作为「需外部客户端」的一类展示）。 */
    val defaultEnabledIds: Set<String> = builtins.map { it.id }.toSet()
}
