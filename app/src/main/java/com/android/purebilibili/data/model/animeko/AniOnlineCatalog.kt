// 文件路径: data/model/animeko/AniOnlineCatalog.kt
//
// 「在线源」实现：对接公开的 maccms(苹果CMS) 采集接口，直接拿到 **m3u8 直链**。
//
// ★ 为什么需要它:
//   蜜柑计划只提供 BT/磁力资源，ExoPlayer 无法直接播放；而 animeko 的 api.animeko.org
//   只提供条目元数据与弹幕，不提供视频流。所以此前「换源」永远拿不到可播地址。
//   maccms 采集接口是公开的标准接口 (ac=detail&wd=)，返回 JSON 里带
//   vod_play_url = "第01集$https://x/index.m3u8#第02集$https://y/index.m3u8"。
//
// ★ 本文件只做「请求拼装 + 纯函数解析」，网络请求在 AniMediaSourceRepository 里做。
//   所有解析函数都是纯函数，便于单测。
package com.android.purebilibili.data.model.animeko

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder

/** 一个 maccms 采集源。接口固定为 {baseUrl}/api.php/provide/vod/。 */
data class AniOnlineCatalogSource(
    val id: String,
    val name: String,
    val baseUrl: String,
)

/**
 * 内置在线源清单。
 *
 * 这三个都实测可用（2026-09 验证：`ac=detail&wd=<番剧名>` 均返回含 m3u8 的
 * vod_play_url，且直链无需 Referer 即可拉取播放列表）。
 */
object AniOnlineCatalog {

    const val ID_LZIAPI = "online_lziapi"
    const val ID_FFZY = "online_ffzy"
    const val ID_FFZY5 = "online_ffzy5"
    const val ID_360ZY = "online_360zy"
    const val ID_BFZY = "online_bfzy"
    const val ID_HNNIU = "online_hongniu"
    const val ID_HNNIU3 = "online_hongniu3"
    const val ID_ZUIDA = "online_zuida"
    const val ID_HUYA = "online_huya"
    const val ID_UNITY = "online_unity"
    // ---- 2026-10 实测新增(见 README「番剧源实测」) ----
    const val ID_JSZY = "online_jszy"
    const val ID_P2100 = "online_p2100"
    const val ID_JINYING = "online_jinying"
    const val ID_HHZY = "online_hhzy"
    const val ID_MDZY = "online_mdzy"
    const val ID_IQIYIZY = "online_iqiyizy"

    val sources: List<AniOnlineCatalogSource> = listOf(
        AniOnlineCatalogSource(ID_LZIAPI, "量子资源", "https://cj.lziapi.com"),
        AniOnlineCatalogSource(ID_FFZY, "非凡资源", "https://api.ffzyapi.com"),
        AniOnlineCatalogSource(ID_360ZY, "360资源", "https://360zy.com"),
        // ---- 2026-09 实测新增(200 OK 且含 m3u8 播放串, 见 _probe_maccms.py) ----
        AniOnlineCatalogSource(ID_BFZY, "暴风资源", "https://bfzyapi.com"),
        AniOnlineCatalogSource(ID_HNNIU, "红牛资源", "https://www.hongniuzy2.com"),
        AniOnlineCatalogSource(ID_ZUIDA, "最大资源", "https://api.zuidapi.com"),
        AniOnlineCatalogSource(ID_HUYA, "虎牙资源", "https://www.huyaapi.com"),
        AniOnlineCatalogSource(ID_UNITY, "Unity资源", "https://api.ukuapi88.com"),
        // ---- 2026-10 直连实测新增镜像(主域在某些网络下慢/被墙, 镜像另走一条线路) ----
        AniOnlineCatalogSource(ID_FFZY5, "非凡资源·镜像", "https://ffzy5.tv"),
        AniOnlineCatalogSource(ID_HNNIU3, "红牛资源·镜像", "https://www.hongniuzy3.com"),
        // ---- 2026-10 「孤独摇滚」专项实测新增(ac=detail&wd= 均直出 m3u8, 延迟 <2.5s) ----
        AniOnlineCatalogSource(ID_JSZY, "极速资源", "https://jszyapi.com"),
        AniOnlineCatalogSource(ID_P2100, "飘零资源", "https://p2100.net"),
        AniOnlineCatalogSource(ID_JINYING, "金鹰资源", "https://jinyingzy.com"),
        AniOnlineCatalogSource(ID_HHZY, "豪华资源", "https://hhzyapi.com"),
        AniOnlineCatalogSource(ID_MDZY, "魔都资源", "https://www.mdzyapi.com"),
        AniOnlineCatalogSource(ID_IQIYIZY, "爱奇艺资源", "https://iqiyizyapi.com"),
        // ★ 2026-10 实测淘汰(不再保留):
        //   - 天堂资源 api.vodtv.cc / 天空资源 api.tiankongapi.com / 快云 kuaiyun-api.com
        //     / 樱花资源 api.yhzyapi.com / 奶香 naixiangapi.com / ikun ikunzyapi.com
        //     → URLError(域名不解析或连接被拒)
        //   - 索尼 suoniapi.com / 无尽 api.wujinapi.me / 新浪 api.xinlangapi.com
        //     → HTTP 403(反爬)
        //   - 快看 kuaikan-api.com → HTTP 404(接口路径已变)
        //   - 天涯 tyyszyapi.com / 卧龙 collect.wolongzyw.com / 闪电 sdzyapi.com
        //     / 茅台 caiji.maotaizy.cc → 返回非 JSON(接口不可用于换源)
    )

    val byId: Map<String, AniOnlineCatalogSource> = sources.associateBy { it.id }

    /** 拼搜索接口。注意关键字必须 URL 编码。 */
    fun detailUrl(baseUrl: String, keyword: String): String {
        val q = runCatching { URLEncoder.encode(keyword, "UTF-8") }.getOrDefault(keyword)
        return detailUrlTemplate(baseUrl).replace("{title}", q)
    }

    /**
     * 不编码关键字的模板。
     *
     * ★ 内置源的 `urlTemplate` 必须用这个：`detailUrl(base, "{title}")` 会把占位符
     * 编码成 `%7Btitle%7D`，之后 `buildUrl` 的 `.replace("{title}", ...)` 就替换不中，
     * 请求会变成「查询字面量 {title}」→ 每个源都 0 条结果。
     */
    fun detailUrlTemplate(baseUrl: String): String =
        "${baseUrl.trimEnd('/')}/api.php/provide/vod/?ac=detail&wd={title}"

    /**
     * 生成搜索关键字候选（按优先级）。
     *
     * 采集站的搜索偏精确：番剧名带「！」「(TV)」「第 2 季」时用原文常常搜不到东西，
     * 所以依次尝试「原文 → 去括号/季数后缀 → 首个词」，命中即停。
     */
    fun searchKeywords(subjectName: String): List<String> {
        val raw = subjectName.trim()
        if (raw.isEmpty()) return emptyList()
        val simplified = raw
            .replace(Regex("[（(【\\[].*?[）)】\\]]"), " ")
            .replace(
                Regex(
                    "(?i)\\s*(第\\s*[0-9一二三四五六七八九十]+\\s*[季期部]|season\\s*\\d+" +
                        "|tv|剧场版|总集篇|篇)\\s*"
                ),
                " "
            )
            .replace(Regex("[！？。·・：:；;，,、—\\-]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
        val firstWord = simplified.split(" ").firstOrNull().orEmpty()
        return listOf(raw, simplified, firstWord)
            .filter { it.isNotBlank() }
            .distinct()
    }

    // =============================================================
    // 响应模型（只声明用得到的字段，其余由 ignoreUnknownKeys 忽略）
    // =============================================================

    @Serializable
    data class CatalogResponse(
        val code: Int = 0,
        val list: List<CatalogItem> = emptyList(),
    )

    @Serializable
    data class CatalogItem(
        @SerialName("vod_name") val name: String = "",
        /** 多个播放组用 $$$ 分隔，如 "feifan$$$ffm3u8"。 */
        @SerialName("vod_play_from") val playFrom: String = "",
        /** 播放地址串。 */
        @SerialName("vod_play_url") val playUrl: String = "",
        @SerialName("type_name") val typeName: String = "",
        @SerialName("vod_remarks") val remarks: String = "",
    )

    /** 一集的可播条目。 */
    data class EpisodeEntry(
        /** 集名，如「第01集」「HD中字」。 */
        val label: String,
        /** 可播放地址（http/https）。 */
        val url: String,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    /** 解析详情接口的 JSON。解析失败返回空列表（调用方按「该源无结果」处理）。 */
    fun parseDetail(body: String): List<CatalogItem> {
        if (body.isBlank()) return emptyList()
        return runCatching { json.decodeFromString(CatalogResponse.serializer(), body).list }
            .getOrDefault(emptyList())
    }

    /**
     * 拆分 `vod_play_url`。
     *
     * 标准格式：`第01集$https://x/index.m3u8#第02集$https://y/index.m3u8`
     * 兼容变体：段内不区分「名称/地址」的先后顺序 —— 取看起来像 URL 的那一段当地址，
     * 另一段当集名。这样名称里有 `$`、或顺序颠倒的站也能正确解析。
     */
    fun splitPlayUrls(playUrl: String): List<EpisodeEntry> {
        if (playUrl.isBlank()) return emptyList()
        return playUrl.split('#').mapNotNull { segment ->
            val parts = segment.split('$').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.isEmpty()) {
                null
            } else {
                val url = parts.firstOrNull { it.startsWith("http", ignoreCase = true) }
                    ?: return@mapNotNull null
                EpisodeEntry(label = parts.firstOrNull { it != url } ?: "", url = url)
            }
        }
    }

    /** 从「第12集」「12」「EP12」「12话」里取出集数。 */
    fun parseEpisodeNumber(text: String): Int? =
        Regex("\\d{1,4}").find(text.trim())?.value?.toIntOrNull()

    /**
     * 在候选条目里挑出目标集。
     *
     * 规则：
     * 1. 目标集数能解析时，优先取集数完全相等的那条；
     * 2. 找不到且**只有一条**（剧场版 / 单集）时允许回退，避免多集番剧播错集；
     * 3. 目标集数解析不出来时，退化为按集名包含匹配，最后取第一条。
     */
    fun pickEpisode(
        entries: List<EpisodeEntry>,
        episodeSort: String,
        episodeName: String = "",
    ): EpisodeEntry? {
        if (entries.isEmpty()) return null
        val wanted = parseEpisodeNumber(episodeSort)
        if (wanted != null) {
            entries.firstOrNull { parseEpisodeNumber(it.label) == wanted }?.let { return it }
            return entries.singleOrNull()
        }
        if (episodeName.isNotBlank()) {
            entries.firstOrNull { it.label.contains(episodeName, ignoreCase = true) }?.let { return it }
        }
        return entries.firstOrNull()
    }

    /** 归一化番剧名，用于打分比较：去空白/标点、统一小写。 */
    fun normalizeName(raw: String): String = raw.trim().lowercase()
        .replace(Regex("[\\s\\p{Punct}！？。·・：:；;，,、（）()\\[\\]【】『』「」]"), "")

    /**
     * ★ 2026-10: 校验「源站返回的条目名」是否与目标番剧名匹配, 防止播错片。
     *
     * 规则(由宽到严兜底, 避免误杀):
     * 1. 归一化后完全相等 -> 匹配;
     * 2. 互相包含(含「第N季」这类后缀) -> 匹配;
     * 3. 目标名去掉「第N季/部/期」核心词后, 与候选名互相包含 -> 匹配;
     * 否则视为不匹配(可能命中了同名/不同版本/另一部作品)。
     */
    fun isNameMatched(candidateName: String, subjectName: String): Boolean {
        val c = normalizeName(candidateName)
        val s = normalizeName(subjectName)
        if (c.isBlank() || s.isBlank()) return false
        if (c == s) return true
        if (c.contains(s) || s.contains(c)) return true
        val sCore = s.replace(Regex("第[0-9零一二三四五六七八九十百]+[季期部]"), "").trim()
        if (sCore.isNotBlank() && (c.contains(sCore) || sCore.contains(c))) return true
        return false
    }

    /**
     * 搜索结果里多条同名/近名条目时，挑最可能是「正片」的那条。
     *
     * 打分：名称完全一致 > 互相包含 > 集数更多；剧场版/总集篇降权。
     */
    fun pickBestItem(
        items: List<CatalogItem>,
        subjectName: String,
        wantEpisode: Int?,
    ): CatalogItem? {
        if (items.isEmpty()) return null
        val target = normalizeName(subjectName)
        return items.maxByOrNull { item ->
            val name = normalizeName(item.name)
            val entries = splitPlayUrls(item.playUrl)
            var score = 0
            when {
                target.isNotEmpty() && name == target -> score += 100
                target.isNotEmpty() && (name.contains(target) || target.contains(name)) -> score += 60
            }
            if (name.contains("剧场") || name.contains("总集篇")) score -= 40
            if (wantEpisode != null && entries.any { parseEpisodeNumber(it.label) == wantEpisode }) {
                score += 30
            }
            score + minOf(entries.size, 30)
        }
    }

    /** 从播放组名里取第一个可读名（去掉 $$$ 分组）。 */
    fun firstPlayGroupName(playFrom: String): String =
        playFrom.split("$$$").firstOrNull { it.isNotBlank() }.orEmpty()
}
