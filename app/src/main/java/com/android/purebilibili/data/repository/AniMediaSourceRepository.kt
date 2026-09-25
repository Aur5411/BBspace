// 文件路径: data/repository/AniMediaSourceRepository.kt
//
// 「换源」解析器。
//
// 职责: 给定「要找哪一集」(AniMediaQuery), 到各已启用数据源里找候选资源。
// 参考 animeko 的媒体源选择思路, 但保持极简: 只做请求拼装 + 结果解析 + 排序,
// 不实现 BT 下载协议 (种子/磁力交给外部 BT 客户端或系统处理)。
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.animeko.AnimekoNetwork
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.animeko.AniWebSourceCatalog
import com.android.purebilibili.data.model.animeko.AniBuiltinSources
import com.android.purebilibili.data.model.animeko.AniMediaCandidate
import com.android.purebilibili.data.model.animeko.AniMediaQuery
import com.android.purebilibili.data.model.animeko.AniMediaSource
import com.android.purebilibili.data.model.animeko.AniMediaSourceKind
import com.android.purebilibili.data.model.animeko.AniOnlineCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URLEncoder

private const val TAG = "AniMediaSourceRepo"

/** 换源结果。 */
data class AniMediaSearchResult(
    val candidates: List<AniMediaCandidate> = emptyList(),
    /** 每个源的执行状态, 便于 UI 提示「哪个源没响应」。 */
    val sourceStatus: List<AniSourceStatus> = emptyList(),
) {
    val isEmpty: Boolean get() = candidates.isEmpty()
}

data class AniSourceStatus(
    val sourceId: String,
    val sourceName: String,
    val ok: Boolean,
    val count: Int,
    val message: String = "",
)

object AniMediaSourceRepository {

    /**
     * 按查询条件到所有启用的源里找候选资源。
     *
     * @param query 查询条件 (含已配置的数据源)
     * @param enabledIds 用户启用的源 id; 为空表示全部启用
     * @param kinds 只检索这些类别的源; null 表示不限。
     *              ★ 播放(换源)只传 [AniMediaSourceKind.ONLINE] —— 在线看
     *              不该被一堆播不了的种子刷屏; 下载面板才传 BT/CUSTOM。
     */
    suspend fun search(
        query: AniMediaQuery,
        enabledIds: Set<String> = AniBuiltinSources.defaultEnabledIds,
        kinds: Set<AniMediaSourceKind>? = null,
    ): AniMediaSearchResult = withContext(Dispatchers.IO) {
        val sources = (query.sources.ifEmpty { AniBuiltinSources.builtins })
            .filter { it.enabled && it.id in enabledIds }
            .filter { kinds == null || it.kindEnum in kinds }

        val candidates = mutableListOf<AniMediaCandidate>()
        val statuses = mutableListOf<AniSourceStatus>()

        // ★ 测速优先: 在线源先按站点延迟排序, 快的先请求、结果也排前面。
        //   实测各站延迟差距可达 3~7 倍, 这一步直接决定「换源」列表的观感。
        var latencyMap: Map<String, Long> = emptyMap()
        val onlineOrdered = sources.filter { it.kindEnum == AniMediaSourceKind.ONLINE }
            .let { onlineList ->
                val webCfgs = onlineList.mapNotNull { AniWebSourceCatalog.byId[it.id] }
                if (webCfgs.isEmpty()) {
                    onlineList.map { it to -1L }
                } else {
                    val latencies = AniWebSourceFetcher.probeLatencies(webCfgs)
                    latencyMap = latencies
                    onlineList.map { src ->
                        src to (latencies[src.id] ?: -1L)
                    }.sortedWith(compareBy { (_, lat) -> if (lat < 0) Long.MAX_VALUE else lat })
                }
            }
        val orderedSources = onlineOrdered.map { it.first } +
            sources.filter { it.kindEnum != AniMediaSourceKind.ONLINE }

        // ★ 全部源并发检索: web 源一趟要 3 跳(搜索/详情/播放), 串行 18 个源会到分钟级。
        //   快的源先返回先上屏(候选最终按测速延迟排序)。
        val results = coroutineScope {
            orderedSources.map { source ->
                async(Dispatchers.IO) {
                    val fetch: suspend () -> List<AniMediaCandidate> = {
                        when (source.kindEnum) {
                            AniMediaSourceKind.ONLINE -> fetchOnlineCandidates(source, query)

                            // BT / 自定义源：解析 RSS，得到种子/磁力或自定义规则给出的地址。
                            AniMediaSourceKind.BT, AniMediaSourceKind.CUSTOM ->
                                fetchRssCandidates(source, query)

                            AniMediaSourceKind.LOCAL -> emptyList()
                        }
                    }
                    source to (runCatching { fetch() }
                        .onFailure { e -> Logger.w(TAG, "源 ${source.name} 失败: ${e.message}") }
                        .getOrDefault(emptyList()))
                }
            }.awaitAll()
        }

        for ((source, list) in results) {
            candidates += list
            statuses += if (list.isNotEmpty() || source.kindEnum != AniMediaSourceKind.ONLINE) {
                AniSourceStatus(source.id, source.name, ok = true, count = list.size)
            } else {
                // web 源没搜到目标集时给个明确状态, UI 才能解释「为什么这个站不在列表里」
                AniSourceStatus(
                    source.id, source.name, ok = false, count = 0,
                    message = "该站没有这一集",
                )
            }
        }

        // 本地缓存源只登记状态(Undo: 候选由 ViewModel 注入)
        if (sources.any { it.kindEnum == AniMediaSourceKind.LOCAL }) {
            statuses += AniSourceStatus(
                sourceId = AniBuiltinSources.ID_LOCAL_CACHE,
                sourceName = "本地缓存",
                ok = true,
                count = 0,
                message = "由本地缓存列表提供",
            )
        }

        AniMediaSearchResult(
            candidates = candidates.sortedWith(
                // ★ 源测速延迟小的排最前; 同源内再按做种数/清晰度/体积
                compareBy<AniMediaCandidate> { c -> latencyMap[c.sourceId] ?: Long.MAX_VALUE }
                    .thenByDescending { it.seeders }
                    .thenByDescending { qualityRank(it.quality) }
                    .thenByDescending { it.sizeBytes }
            ),
            sourceStatus = statuses,
        )
    }

    /**
     * 在线源分发: web 站点源走三跳抓取, maccms 采集源走 JSON 接口。
     */
    private suspend fun fetchOnlineCandidates(
        source: AniMediaSource,
        query: AniMediaQuery,
    ): List<AniMediaCandidate> {
        val webCfg = AniWebSourceCatalog.byId[source.id]
        return if (webCfg != null) {
            val c = AniWebSourceFetcher.fetchEpisode(
                cfg = webCfg,
                subjectName = query.subjectName,
                episodeSort = query.episodeSort,
            )
            listOfNotNull(c)
        } else {
            fetchOnlineCatalogCandidates(source, query)
        }
    }

    /**
     * 在线源（maccms 采集接口）检索。
     *
     * 流程：按番剧名搜索 → 在候选条目里挑最像「正片」的那条 → 拆出每集地址 →
     * 取出目标集对应的 m3u8。每个在线源只贡献**当前这一集**的候选，
     * 这样「换源」面板展示的就是「同一集的不同源」，语义正确。
     *
     * 返回的地址是 m3u8 直链，media3 可直接播放。
     */
    private fun fetchOnlineCatalogCandidates(
        source: AniMediaSource,
        query: AniMediaQuery,
    ): List<AniMediaCandidate> {
        // 直接按站点根地址请求，不走 urlTemplate（避免占位符被编码的历史坑）。
        val baseUrl = AniOnlineCatalog.byId[source.id]?.baseUrl
            ?: source.homepage.takeIf { it.isNotBlank() }
            ?: return emptyList()

        // 番剧名可能带「！」「(TV)」「第 2 季」等，采集站搜不到；
        // 依次用「原文 → 简化名 → 首个词」试，命中即停。
        val keywords = AniOnlineCatalog.searchKeywords(query.subjectName)
        if (keywords.isEmpty()) return emptyList()

        var items: List<AniOnlineCatalog.CatalogItem> = emptyList()
        var lastError: String? = null
        for (keyword in keywords) {
            val request = Request.Builder()
                .url(AniOnlineCatalog.detailUrl(baseUrl, keyword))
                .header("User-Agent", MEDIA_SOURCE_USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .build()
            val body = try {
                AnimekoNetwork.okHttpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    resp.body?.string().orEmpty()
                }
            } catch (e: Exception) {
                lastError = e.message ?: "请求失败"
                continue
            }
            items = AniOnlineCatalog.parseDetail(body)
            if (items.isNotEmpty()) break
        }
        if (items.isEmpty()) {
            // 三次都没结果：把原因抛出去，让「换源」面板能显示具体是哪一步失败
            error(lastError ?: "未搜到该番剧（已尝试 ${keywords.size} 个关键字）")
        }

        val wantEpisode = AniOnlineCatalog.parseEpisodeNumber(query.episodeSort)
        val best = AniOnlineCatalog.pickBestItem(items, query.subjectName, wantEpisode)
            ?: return emptyList()
        val entries = AniOnlineCatalog.splitPlayUrls(best.playUrl)
        // 该源没有目标集时不产出候选（宁缺勿错，避免播成别的集）
        val picked = AniOnlineCatalog.pickEpisode(entries, query.episodeSort, query.episodeName)
            ?: return emptyList()

        val groupName = AniOnlineCatalog.firstPlayGroupName(best.playFrom)
        return listOf(
            AniMediaCandidate(
                sourceId = source.id,
                sourceName = source.name,
                kind = AniMediaSourceKind.ONLINE.name,
                url = picked.url,
                title = buildString {
                    append(best.name)
                    if (picked.label.isNotBlank()) append(" · ").append(picked.label)
                },
                quality = groupName.ifBlank { "在线" },
                subtitleGroup = source.name,
            )
        )
    }

    /**
     * 按 URL 模板请求并解析 RSS / 文本列表。
     *
     * 蜜柑的 RSS 是标准 RSS 2.0, 每个 <item> 含 title / link / enclosure(url,length)。
     * 这里用手写正则解析, 避免为一个源引入完整 XML 依赖。
     */
    private fun fetchRssCandidates(
        source: AniMediaSource,
        query: AniMediaQuery,
    ): List<AniMediaCandidate> {
        val url = buildUrl(source, query) ?: return emptyList()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", MEDIA_SOURCE_USER_AGENT)
            .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
            .build()

        val body = AnimekoNetwork.okHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            resp.body?.string().orEmpty()
        }
        if (body.isBlank()) return emptyList()

        // RSS 2.0 (<item>) 与 Atom (<entry>) 两种格式都支持:
        // nyaa.land 是标准 RSS; AnimeGarden 的 feed.xml 是 Atom。
        val items = if (body.contains("<entry", ignoreCase = true)) {
            parseAtomEntries(body)
        } else {
            parseRssItems(body)
        }
        return items
            .let { items -> filterByEpisode(items, query) }
            .map { item ->
                AniMediaCandidate(
                    sourceId = source.id,
                    sourceName = source.name,
                    kind = source.kind,
                    url = item.enclosureUrl.ifBlank { item.link },
                    title = item.title,
                    quality = extractQuality(item.title),
                    subtitleGroup = extractGroup(item.title),
                    sizeBytes = item.length,
                    seeders = item.seeders,
                )
            }
    }

    /** 把模板里的占位符替换成真实值。 */
    private fun buildUrl(source: AniMediaSource, query: AniMediaQuery): String? {
        val template = source.urlTemplate
        if (template.isBlank()) return null
        // BT 源优先用蜜柑 id, 没有则退回标题搜索。
        // 域名用镜像域 mikanime.tv: 主域 mikanani.me 在部分网络下不可达。
        if (source.id == AniBuiltinSources.ID_BT_MIKAN && query.mikanId != null) {
            return "https://mikanime.tv/RSS/Bangumi?bangumiId=${query.mikanId}"
        }
        return template
            .replace("{bangumiId}", query.bangumiId.toString())
            .replace("{episodeSort}", query.episodeSort)
            .replace("{title}", urlEncode(query.subjectName))
    }

    // ---------------------------------------------------------------
    // 极简 RSS 解析
    // ---------------------------------------------------------------

    private data class RssItem(
        val title: String,
        val link: String,
        val enclosureUrl: String,
        val length: Long,
        val seeders: Int,
    )

    private val ITEM_REGEX = Regex("<item[\\s>][\\s\\S]*?</item>", RegexOption.IGNORE_CASE)
    private val TITLE_REGEX = Regex("<title>(?:<!\\[CDATA\\[)?([\\s\\S]*?)(?:\\]\\]>)?</title>", RegexOption.IGNORE_CASE)
    private val LINK_REGEX = Regex("<link>(?:<!\\[CDATA\\[)?([\\s\\S]*?)(?:\\]\\]>)?</link>", RegexOption.IGNORE_CASE)
    private val ENCLOSURE_REGEX = Regex("<enclosure[^>]*url=\"([^\"]+)\"[^>]*(?:length=\"(\\d+)\")?", RegexOption.IGNORE_CASE)
    private val SEEDERS_REGEX = Regex("(?:seeders|seeds)=\"(\\d+)\"", RegexOption.IGNORE_CASE)

    private fun parseRssItems(xml: String): List<RssItem> {
        return ITEM_REGEX.findAll(xml).mapNotNull { m ->
            val block = m.value
            val title = TITLE_REGEX.find(block)?.groupValues?.get(1)?.trim().orEmpty()
            if (title.isBlank()) return@mapNotNull null
            val link = LINK_REGEX.find(block)?.groupValues?.get(1)?.trim().orEmpty()
            val encMatch = ENCLOSURE_REGEX.find(block)
            RssItem(
                title = decodeEntities(title),
                link = link,
                enclosureUrl = encMatch?.groupValues?.get(1).orEmpty(),
                length = encMatch?.groupValues?.get(2)?.toLongOrNull() ?: 0L,
                seeders = SEEDERS_REGEX.find(block)?.groupValues?.get(1)?.toIntOrNull() ?: -1,
            )
        }.toList()
    }

    private val ENTRY_REGEX = Regex("<entry[\\s>][\\s\\S]*?</entry>", RegexOption.IGNORE_CASE)
    private val ATOM_LINK_REGEX =
        Regex("<link[^>]*href=\"([^\"]+)\"[^>]*>", RegexOption.IGNORE_CASE)

    /** Atom feed 最小解析(AnimeGarden): title + 优先 magnet/torrent 的 link。 */
    private fun parseAtomEntries(xml: String): List<RssItem> {
        return ENTRY_REGEX.findAll(xml).mapNotNull { m ->
            val block = m.value
            val title = TITLE_REGEX.find(block)?.groupValues?.get(1)?.trim().orEmpty()
            if (title.isBlank()) return@mapNotNull null
            val links = ATOM_LINK_REGEX.findAll(block).map { it.groupValues[1] }.toList()
            val best = links.firstOrNull { it.startsWith("magnet") || it.contains(".torrent") }
                ?: links.firstOrNull().orEmpty()
            RssItem(
                title = decodeEntities(title),
                link = best,
                enclosureUrl = best,
                length = 0L,
                seeders = -1,
            )
        }.toList()
    }

    /** 只保留与目标集数匹配的条目; 若一条都没匹配上, 则原样返回 (让用户自己挑)。 */
    private fun filterByEpisode(items: List<RssItem>, query: AniMediaQuery): List<RssItem> {
        val sort = query.episodeSort.trim()
        if (sort.isBlank() || items.isEmpty()) return items
        val num = sort.substringBefore('.').toIntOrNull() ?: return items
        // 常见命名: " - 01 "、"[01]"、"第01话"、"E01"
        val patterns = listOf(
            Regex("""[-–—]\s*0*$num(?:\D|$)"""),
            Regex("""\[\s*0*$num\s*]"""),
            Regex("""第\s*0*$num\s*[话話集]"""),
            Regex("""[Ee]\s*0*$num(?:\D|$)"""),
            Regex("""\s0*$num\s*\["""),
        )
        val matched = items.filter { item -> patterns.any { it.containsMatchIn(item.title) } }
        return matched.ifEmpty { items }
    }

    /** 从标题里抠出清晰度, 如 1080p / 4K / 720p。 */
    private fun extractQuality(title: String): String {
        val m = Regex("""(?i)(2160p|1440p|1080p|1080i|720p|480p|4K|8K)""").find(title)
        return m?.value?.uppercase().orEmpty()
    }

    /** 从标题里抠出字幕组, 通常是开头的 [xxx] 或 【xxx】。 */
    private fun extractGroup(title: String): String {
        val m = Regex("""^\s*[\[【]([^\]】]{1,24})[\]】]""").find(title)
        return m?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun qualityRank(quality: String): Int = when (quality.uppercase()) {
        "8K" -> 8000
        "4K", "2160P" -> 2160
        "1440P" -> 1440
        "1080P" -> 1080
        "1080I" -> 1080
        "720P" -> 720
        "480P" -> 480
        else -> 0
    }

    private fun urlEncode(value: String): String =
        runCatching { URLEncoder.encode(value, "UTF-8") }.getOrDefault(value)

    private fun decodeEntities(s: String): String = s
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")

    private const val MEDIA_SOURCE_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
}
