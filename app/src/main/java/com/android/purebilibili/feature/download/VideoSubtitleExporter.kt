// 文件路径: feature/download/VideoSubtitleExporter.kt
//
// 下载 B 站字幕并存成 .srt。
//
// B站字幕流程(与参考实现一致):
//   1. x/player/v2 (bvid,cid) → data.subtitle.subtitles[] 里带 subtitle_url
//      (形如 https://aisubtitle.hdslb.com/bfs/ai_subtitle/....json)
//   2. 拉这个 JSON: {"body":[{"from":0.0,"to":2.34,"content":"台词"}, ...]}
//   3. 转成标准 SRT 文本写出。
//
// ★ 字幕 JSON 里的时间单位是**秒(浮点)**, SRT 需要 hh:mm:ss,mmm —— 这里的
//   换算与逗号毫秒格式是硬性规范, 写错播放器会不认。
package com.android.purebilibili.feature.download

import android.content.Context
import com.android.purebilibili.core.network.animeko.AnimekoNetwork
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.core.util.PublicDownloadStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

private const val TAG = "VideoSubtitleExporter"

object VideoSubtitleExporter {

    private val client get() = AnimekoNetwork.okHttpClient

    /**
     * 下载字幕并导出为 SRT。
     *
     * @param subtitleUrl 播放器接口给出的字幕 json 地址
     * @param baseName 与视频同名的基名(不含扩展名)
     * @return 写出的文件名; 失败返回 null
     */
    suspend fun exportSrt(
        context: Context,
        subtitleUrl: String,
        baseName: String,
        folder: String = PublicDownloadStore.FOLDER_VIDEO,
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val json = fetchText(subtitleUrl) ?: return@withContext null
            val srt = toSrt(json) ?: return@withContext null
            val fileName = "${PublicDownloadStore.sanitizeFileName(baseName)}.srt"
            val (uri, out) = PublicDownloadStore.createFile(
                context, folder, fileName, PublicDownloadStore.mimeOf("x.srt"),
            ) ?: return@withContext null
            out.use { it.write(srt.toByteArray(Charsets.UTF_8)) }
            PublicDownloadStore.finishFile(context, uri)
            Logger.d(TAG, "字幕已导出: $fileName")
            fileName
        }.getOrElse { e ->
            Logger.w(TAG, "字幕导出失败: ${e.message}")
            null
        }
    }

    /**
     * B站字幕 JSON -> SRT 文本。纯函数, 便于单测。
     *
     * 输入: {"body":[{"from":0.0,"to":2.34,"content":"..."}]}
     */
    fun toSrt(json: String): String? {
        val body = runCatching {
            JSONObject(json).optJSONArray("body") ?: return null
        }.getOrNull() ?: return null
        if (body.length() == 0) return null

        val sb = StringBuilder()
        var index = 0
        for (i in 0 until body.length()) {
            val item = body.optJSONObject(i) ?: continue
            val content = item.optString("content").trim()
            if (content.isEmpty()) continue
            val from = item.optDouble("from", -1.0)
            val to = item.optDouble("to", -1.0)
            if (from < 0.0 || to < from) continue
            index++
            sb.append(index).append('\n')
            sb.append(formatSrtTime(from)).append(" --> ").append(formatSrtTime(to)).append('\n')
            sb.append(content).append("\n\n")
        }
        return sb.takeIf { index > 0 }?.toString()
    }

    /** 秒 -> SRT 时间戳 hh:mm:ss,mmm。 */
    fun formatSrtTime(seconds: Double): String {
        val totalMillis = (seconds * 1000.0).toLong().coerceAtLeast(0L)
        val millis = totalMillis % 1000
        val totalSeconds = totalMillis / 1000
        val s = totalSeconds % 60
        val m = (totalSeconds / 60) % 60
        val h = totalSeconds / 3600
        return "%02d:%02d:%02d,%03d".format(h, m, s, millis)
    }

    private fun fetchText(url: String): String? = runCatching {
        val normalised = if (url.startsWith("//")) "https:$url"
        else if (url.startsWith("http://")) url.replace("http://", "https://")
        else url
        val request = Request.Builder()
            .url(normalised)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
            .header("Referer", "https://www.bilibili.com")
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) null else resp.body?.string()
        }
    }.getOrNull()
}
