// 文件路径: data/repository/AniWebSourceFetcher.kt
//
// Web 站点源的「搜索 → 详情 → 播放页 → 直链」三跳抓取 + 站点测速。
//
// ★ 解析全部用正则而不是 jsoup: 这些站点(MacCMS 系)结构高度雷同,
//   核心就是 <a href> + Base64 播放数据, 正则足够且免去一个依赖。
//   链路已在真实站点验证(E-ACG / 去看吧 全链路 200 OK 拿到 m3u8)。
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.animeko.AnimekoNetwork
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.animeko.AniMediaCandidate
import com.android.purebilibili.data.model.animeko.AniWebSourceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private const val TAG = "AniWebSourceFetcher"

object AniWebSourceFetcher {

    private val client = AnimekoNetwork.okHttpClient.newBuilder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val http get() = AnimekoNetwork.okHttpClient

    // ---------------------------------------------------------------
    // 站点测速(用户要求: 优先用最快的站)
    // ---------------------------------------------------------------

    /** 各站延迟缓存(host 毫秒); -1 = 不可达。TTL 10 分钟。 */
    private val latencyCache = ConcurrentHashMap<String, Pair<Long, Long>>()
    private const val LATENCY_TTL_MS = 10 * 60_000L

    /**
     * 并发测所有 web 源的首页延迟。
     *
     * @return sourceId -> 延迟毫秒(-1 = 不可达)。结果带 10 分钟缓存,
     *   换集/重进播放器不会反复全量测速。
     */
    suspend fun probeLatencies(sources: List<AniWebSourceConfig>): Map<String, Long> =
        coroutineScope {
            val now = System.currentTimeMillis()
            sources.map { cfg ->
                async(Dispatchers.IO) {
                    val cached = latencyCache[cfg.homepage]
                    if (cached != null && now - cached.second < LATENCY_TTL_MS) {
                        cfg.id to cached.first
                    } else {
                        cfg.id to measureLatency(cfg)
                    }
                }
            }.awaitAll().toMap()
        }

    private suspend fun measureLatency(cfg: AniWebSourceConfig): Long =
        withContext(Dispatchers.IO) {
            val t0 = System.currentTimeMillis()
            try {
                val request = Request.Builder()
                    .url(cfg.homepage)
                    .header("User-Agent", WEB_UA)
                    .head()
                    .build()
                client.newCall(request).execute().use { resp ->
                    val ms = System.currentTimeMillis() - t0
                    latencyCache[cfg.homepage] = (if (resp.isSuccessful) ms else -1L) to System.currentTimeMillis()
                    return@withContext if (resp.isSuccessful) ms else -1L
                }
            } catch (e: Exception) {
                // HEAD 被拒时退化成 GET
                try {
                    val req2 = Request.Builder().url(cfg.homepage).header("User-Agent", WEB_UA).build()
                    client.newCall(req2).execute().use { resp ->
                        val ms = System.currentTimeMillis() - t0
                        latencyCache[cfg.homepage] = (if (resp.isSuccessful) ms else -1L) to System.currentTimeMillis()
                        return@withContext if (resp.isSuccessful) ms else -1L
                    }
                } catch (e2: Exception) {
                    Logger.w(TAG, "测速失败 ${cfg.name}: ${e2.message}")
                    latencyCache[cfg.homepage] = -1L to System.currentTimeMillis()
                    -1L
                }
            }
        }

    /** 按延迟给源排序(不可达的排最后并置 -1)。 */
    fun sortByLatency(
        sources: List<AniWebSourceConfig>,
        latencies: Map<String, Long>,
    ): List<Pair<AniWebSourceConfig, Long>> =
        sources.map { it to (latencies[it.id] ?: -1L) }
            .sortedWith(compareBy<Pair<AniWebSourceConfig, Long>> { p -> if (p.second < 0) Long.MAX_VALUE else p.second })

    // ---------------------------------------------------------------
    // 三跳抓取
    // ---------------------------------------------------------------

    /**
     * 在一个 web 源里搜目标番剧的目标集。
     *
     * @return 找到时给一条候选; 找不到/任何一步失败给 null(调用方按「该源无结果」处理)。
     */
    suspend fun fetchEpisode(
        cfg: AniWebSourceConfig,
        subjectName: String,
        episodeSort: String,
        latencyMs: Long = -1L,
    ): AniMediaCandidate? = withContext(Dispatchers.IO) {
        val keywords = buildSearchKeywords(subjectName, cfg.useOnlyFirstWord)
        for (keyword in keywords) {
            try {
                val candidate = tryKeyword(cfg, keyword, subjectName, episodeSort, latencyMs)
                if (candidate != null) return@withContext candidate
            } catch (e: Exception) {
                Logger.w(TAG, "${cfg.name} 搜「$keyword」失败: ${e.message}")
            }
        }
        null
    }

    private suspend fun tryKeyword(
        cfg: AniWebSourceConfig,
        keyword: String,
        subjectName: String,
        episodeSort: String,
        latencyMs: Long,
    ): AniMediaCandidate? {
        // 1. 搜索页 → 找名字匹配的条目链接
        val searchHtml = httpGet(cfg.searchUrl.replace("{keyword}", urlEncode(keyword))) ?: return null
        val hits = extractLinks(searchHtml)
            .filter { (_, text) -> nameMatches(text, subjectName) }
            // preferShorterName: 名字最短的最可能是正片条目
            .sortedBy { (_, text) -> text.length }
        val detailUrl = hits.firstOrNull()?.first ?: return null

        // 2. 详情页 → 找目标集的播放链接
        val detailHtml = httpGet(detailUrl) ?: return null
        val playUrl = extractLinks(detailHtml)
            .map { (href, text) -> absolutize(href, detailUrl) to text }
            .filter { (href, text) ->
                isPlayLink(href, text) && episodeMatches(text, episodeSort)
            }
            .firstOrNull()?.first
            ?: extractLinks(detailHtml)
                .map { (href, text) -> absolutize(href, detailUrl) to text }
                .filter { (href, _) -> isPlayLink(href, "") && episodeMatches("", episodeSort) }
                .firstOrNull()?.first
            ?: return null

        // 3. 播放页 → 抠直链(必要时跟一层 iframe)
        val playHtml = httpGet(playUrl) ?: return null
        val video = extractVideoUrl(playHtml, playUrl)
            ?: return null

        return AniMediaCandidate(
            sourceId = cfg.id,
            sourceName = cfg.name,
            kind = com.android.purebilibili.data.model.animeko.AniMediaSourceKind.ONLINE.name,
            url = video,
            title = "$subjectName 第${episodeSort}话",
            quality = "web",
            subtitleGroup = cfg.name,
        )
    }

    // ---------------------------------------------------------------
    // HTML 解析工具
    // ---------------------------------------------------------------

    /** 抽取页面全部 <a href> 及其文本。 */
    private fun extractLinks(html: String): List<Pair<String, String>> =
        LINK_REGEX.findAll(html).mapNotNull { m ->
            val href = m.groupValues[1].trim()
            val text = m.groupValues[2].replace(TAG_STRIP_REGEX, "").trim()
            if (href.isEmpty() || href.startsWith("javascript")) null else href to text
        }.toList()

    private val LINK_REGEX =
        Regex("""<a\s[^>]*href=["']([^"']+)["'][^>]*>([\s\S]{0,200}?)</a>""", RegexOption.IGNORE_CASE)
    private val TAG_STRIP_REGEX = Regex("<[^>]+>")

    /** 名字匹配: 目标番剧名(或其关键词)出现在链接文本里。 */
    private fun nameMatches(text: String, subjectName: String): Boolean {
        if (text.isBlank()) return false
        val simplified = simplifyName(subjectName)
        return text.contains(simplified) || (simplified.length >= 3 && simplified.contains(text))
    }

    /** 去掉「第X季」「(TV)」等后缀, 提高搜索命中。 */
    private fun simplifyName(raw: String): String =
        raw.replace(Regex("[（(【\\[].*?[）)】\\]]"), " ")
            .replace(Regex("(?i)\\s*(第\\s*[0-9一二三四五六七八九十]+\\s*[季期部]|season\\s*\\d+|tv|剧场版|总集篇)\\s*"), " ")
            .replace(Regex("[！？。·・：:；;，,、—\\-]"), " ")
            .trim()

    private fun buildSearchKeywords(subjectName: String, onlyFirstWord: Boolean): List<String> {
        val raw = subjectName.trim()
        val simplified = simplifyName(raw)
        val firstWord = simplified.split(" ").firstOrNull().orEmpty()
        return buildList {
            add(raw)
            if (simplified != raw) add(simplified)
            if (onlyFirstWord && firstWord.length >= 2) add(firstWord)
        }.distinct().filter { it.isNotBlank() }
    }

    /** 相对链接转绝对。 */
    private fun absolutize(href: String, baseUrl: String): String = when {
        href.startsWith("http") -> href
        href.startsWith("//") -> "https:$href"
        href.startsWith("/") -> baseUrl.substringBefore("//") + "//" +
            baseUrl.removePrefix("http://").removePrefix("https://").substringBefore('/') + href

        else -> baseUrl.substringBeforeLast('/') + "/" + href
    }

    /** 播放链接特征: vodplay/play/-数字-/ep 等路径, 且不是详情页/搜索页。 */
    private fun isPlayLink(href: String, text: String): Boolean {
        val lower = href.lowercase()
        if (lower.contains("detail") || lower.contains("search") || lower.contains("index.php/vod")) {
            return false
        }
        return lower.contains("play") || lower.contains("vodplay") ||
            Regex("/ep\\d+").containsMatchIn(lower) ||
            Regex("-\\d+-\\d+\\.html").containsMatchIn(lower) ||
            (text.isNotBlank() && Regex("第\\s*[0-9一二三四五六七八九十百]+\\s*[话話集]|^\\s*0*\\d{1,3}\\s*$").containsMatchIn(text))
    }

    /** 集数匹配(文案里能对上目标集数)。 */
    private fun episodeMatches(text: String, episodeSort: String): Boolean {
        val num = episodeSort.substringBefore('.').toIntOrNull() ?: return true
        val patterns = listOf(
            Regex("""第\s*0*$num\s*[话話集]"""),
            Regex("""[-–—]\s*0*$num(?:\D|$)"""),
            Regex("""\[\s*0*$num\s*]"""),
            Regex("""[Ee][Pp]\s*0*$num(?:\D|$)"""),
            Regex("""^0*$num$"""),
            Regex("""\s0*$num\s*$"""),
        )
        return patterns.any { it.containsMatchIn(text.trim()) }
    }

    // ---------------------------------------------------------------
    // 播放页直链提取(base64 → player_aaaa → iframe → 明文)
    // ---------------------------------------------------------------

    fun extractVideoUrl(html: String, pageUrl: String): String? {
        // 1) data-play="aHR0..." —— Base64, 可能掺前缀垃圾; aHR0 是 "http" 的 base64 前缀
        DATA_PLAY_REGEX.findAll(html).forEach { m ->
            val b64 = m.groupValues[1]
            val idx = b64.indexOf("aHR0")
            if (idx >= 0) {
                runCatching {
                    val cleaned = b64.substring(idx)
                    val padded = cleaned + "=".repeat((4 - cleaned.length % 4) % 4)
                    String(Base64.getDecoder().decode(padded), Charsets.UTF_8)
                }.getOrNull()?.let { decoded ->
                    if (decoded.startsWith("http") && VIDEO_HINT_REGEX.containsMatchIn(decoded)) {
                        return decoded
                    }
                }
            }
        }

        // 2) MacCMS 播放器全局变量 player_aaaa = {...,"url":"..."}
        PLAYER_AAAA_REGEX.find(html)?.let { m ->
            val u = m.groupValues[1].replace("\\/", "/")
            if (u.startsWith("http")) return u
        }

        // 3) 明文直链
        PLAIN_VIDEO_REGEX.find(html)?.let { return it.value }

        // 4) iframe 一层嵌套
        IFRAME_REGEX.findAll(html).map { it.groupValues[1] }.forEach { iframeSrc ->
            val iframeUrl = absolutize(iframeSrc, pageUrl)
            if (iframeUrl.startsWith("http") && !iframeUrl.contains(pageUrl)) {
                val child = httpGet(iframeUrl) ?: return@forEach
                extractVideoUrl(child, iframeUrl)?.let { return it }
            }
        }
        return null
    }

    private val DATA_PLAY_REGEX = Regex("""data-play=["']([A-Za-z0-9+/=]{20,})["']""")
    private val PLAYER_AAAA_REGEX = Regex("""player_aaaa\s*=\s*\{[^}]*?"url"\s*:\s*"([^"]+)"""")
    private val PLAIN_VIDEO_REGEX = Regex("""https?://[^"'<>\s\\]+\.(?:m3u8|mp4)[^"'<>\s\\]*""")
    private val IFRAME_REGEX = Regex("""<iframe[^>]+(?:src|data-src)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val VIDEO_HINT_REGEX = Regex("""\.(m3u8|mp4)($|[?#])""")

    private fun httpGet(url: String): String? = try {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", WEB_UA)
            .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
            .build()
        http.newCall(request).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string().orEmpty().ifEmpty { null } else null
        }
    } catch (e: Exception) {
        null
    }

    private fun urlEncode(value: String): String =
        runCatching { java.net.URLEncoder.encode(value, "UTF-8") }.getOrDefault(value)

    private const val WEB_UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
}
