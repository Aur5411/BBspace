// 文件路径: feature/anime/AniQualityProbe.kt
//
// 「画质」面板的数据探测。
//
// ★ 实测结论(2026-09, 勿凭想象改):
//   maccms 采集源的 m3u8 绝大多数是**单码率** media playlist(只有一条
//   EXTINF 序列), 少数是只含一档的 master playlist(如量子 1080x608)。
//   所以同一源内通常没有清晰度可切; **不同源之间的画质不同但接口不标注**。
//
//   因此「清晰度切换」落地为两层:
//     1. 当前流若真是多档 HLS master → 用 TrackSelectionParameters 限高切换;
//     2. 否则把其它在线源候选逐个拉一下 m3u8 首部, 解析出各自的分辨率,
//        在面板里标注「1080P / 720P / 未知」, 点选即换源 —— 用户可感知的
//        「切换清晰度」。
package com.android.purebilibili.feature.anime

import com.android.purebilibili.core.network.animeko.AnimekoNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/** 一档视频清晰度。height=0 表示解析不出来。 */
data class AniVideoTier(
    val width: Int,
    val height: Int,
) {
    val label: String
        get() = when {
            height >= 2000 -> "4K"
            height >= 1400 -> "2K"
            height > 0 -> "${height}P"
            else -> "未知"
        }
}

object AniQualityProbe {

    private val client get() = AnimekoNetwork.okHttpClient

    /** 分辨率解析结果缓存(url -> 最高档高度)。 */
    private val cache = HashMap<String, Int>()

    /**
     * 探测一个直链的分辨率(取 m3u8 首部 8KB 就够, 不下载全片)。
     *
     * @return 最高档的高度; 解析不出返回 0
     */
    suspend fun probeMaxHeight(url: String): Int = withContext(Dispatchers.IO) {
        cache[url]?.let { return@withContext it }
        val result = runCatching { fetchTiers(url).maxOrNull() ?: 0 }.getOrDefault(0)
        synchronized(cache) { cache[url] = result }
        result
    }

    /** 拉播放列表首部并解析全部分辨率档位。 */
    private fun fetchTiers(url: String): List<Int> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DL_UA)
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.byteStream() ?: return emptyList()
            // master playlist 的档位声明在文件头部, 读前 16KB 足够
            val buf = ByteArray(16 * 1024)
            val n = body.read(buf)
            val head = String(buf, 0, n.coerceAtLeast(0), Charsets.UTF_8)
            val heights = RESOLUTION_REGEX.findAll(head)
                .map { it.groupValues[2].toIntOrNull() ?: 0 }
                .filter { it > 0 }
                .toList()
            return heights
        }
    }

    private val RESOLUTION_REGEX = Regex("""RESOLUTION=(\d+)x(\d+)""")

    private const val DL_UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
}
