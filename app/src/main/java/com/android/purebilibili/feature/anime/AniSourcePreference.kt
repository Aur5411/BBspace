// 文件路径: feature/anime/AniSourcePreference.kt
//
// 番剧「视频源管理」: 优先源列表(有序) + 源测速。
//
// ★ 应用户要求:
//   在番剧设置里能勾选「优先使用哪些源」, 并能调整顺序 —— 播放番剧时
//   就按这个顺序去搜, 命中即播; 还能一键测试每个源的速度(通过请求源站
//   首页计时), 方便把快的源排到前面。
//
// 存储用 SharedPreferences(逗号分隔的有序 id 列表), 不进 SettingsManager
// 是为了和主 App 设置保持隔离, 与 AniSettingsStore 的做法一致。
package com.android.purebilibili.feature.anime

import android.content.Context
import com.android.purebilibili.core.network.animeko.AnimekoNetwork
import com.android.purebilibili.data.model.animeko.AniMediaSourceKind
import com.android.purebilibili.data.model.animeko.AniOnlineCatalog
import com.android.purebilibili.data.model.animeko.AniWebSourceCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.HttpURLConnection

/** 一个可被管理的视频源。 */
data class AniSourceEntry(
    val id: String,
    val name: String,
    val kind: String,
    /** 测速用的地址(源站首页)。 */
    val testUrl: String,
) {
    val kindLabel: String
        get() = when (kind) {
            AniMediaSourceKind.BT.name -> "BT"
            AniMediaSourceKind.CUSTOM.name -> "自定义"
            AniMediaSourceKind.LOCAL.name -> "本地"
            else -> "在线"
        }
}

object AniSourcePreference {

    private const val PREFS_NAME = "ani_source_pref"
    private const val KEY_PREFERRED = "preferred_sources"

    fun getPreferred(context: Context): List<String> {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PREFERRED, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        // 过滤掉已经不存在的源 id, 避免旧数据把检索范围清空
        val validIds = allSources().map { it.id }.toSet()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() && it in validIds }
    }

    fun setPreferred(context: Context, ids: List<String>) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFERRED, ids.joinToString(","))
            .apply()
    }

    /** 全部可管理的源: maccms 采集源 + 网页源 + BT 源。 */
    fun allSources(): List<AniSourceEntry> = buildList {
        AniOnlineCatalog.sources.forEach { s ->
            add(
                AniSourceEntry(
                    id = s.id,
                    name = s.name,
                    kind = AniMediaSourceKind.ONLINE.name,
                    testUrl = s.baseUrl,
                )
            )
        }
        AniWebSourceCatalog.sources.forEach { s ->
            add(
                AniSourceEntry(
                    id = s.id,
                    name = s.name,
                    kind = AniMediaSourceKind.ONLINE.name,
                    testUrl = s.homepage,
                )
            )
        }
        AniWebSourceCatalog.btSources.forEach { s ->
            add(
                AniSourceEntry(
                    id = s.id,
                    name = s.name,
                    kind = s.kind,
                    testUrl = rootOf(s.urlTemplate.ifBlank { s.homepage }),
                )
            )
        }
    }

    /** 取 URL 的 scheme://host, 用于 BT 源测速(BT 地址带查询参数, 不适合直接当首页测)。 */
    private fun rootOf(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""
        val afterScheme = trimmed.substringAfter("://", "")
        if (afterScheme.isBlank()) return trimmed
        val scheme = trimmed.substringBefore("://")
        return "$scheme://" + afterScheme.substringBefore('/')
    }

    /**
     * 逐个源测速(请求源站首页计时)。
     *
     * @return id -> 毫秒; -1 表示不可达
     */
    suspend fun measureAll(context: Context): Map<String, Long> = withContext(Dispatchers.IO) {
        val client = AnimekoNetwork.okHttpClient
        val result = linkedMapOf<String, Long>()
        allSources().forEach { source ->
            result[source.id] = runCatching {
                val url = source.testUrl
                if (url.isBlank()) return@runCatching -1L
                val started = System.currentTimeMillis()
                val request = Request.Builder()
                    .url(url)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36",
                    )
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.code >= HttpURLConnection.HTTP_BAD_REQUEST) {
                        -1L
                    } else {
                        // 触发一次响应头读取即可, 不下载整页
                        response.body?.source()?.use { it.readByte() }
                        System.currentTimeMillis() - started
                    }
                }
            }.getOrDefault(-1L)
        }
        result
    }
}
