// 文件路径: network-core/src/main/java/com/android/purebilibili/core/network/policy/ThirdPartyHostPolicy.kt
//
// 第三方域名请求头隔离策略。
//
// ★ 背景（实测复现，2026-09-23）
//
// 主 OkHttp 客户端（NetworkModule.okHttpClient）上挂着一个「B 站站点头」拦截器，
// 它会对**所有**非 app.bilibili.com 的请求无条件注入：
//     Referer: https://www.bilibili.com
//     Origin:  https://www.bilibili.com
//
// 这在只访问 B 站域名时没问题，但 App 里还有三类**非 B 站**流量复用同一个客户端：
//   1. 追番模块：api.animeko.org / static.myani.org（animeko 数据与角色图、声优图）
//   2. 番剧换源：cj.lziapi.com / api.ffzyapi.com / mikanime.tv 等采集源
//   3. Coil 图片加载：OkHttpNetworkFetcherFactory 就直接用了这个客户端
//
// 实测结论（curl 复现，逐项验证过）：
//   api.animeko.org 收到 `Origin: https://www.bilibili.com` 时**直接 403**，
//   而 static.myani.org 的任何请求头都不受影响 —— 这正好解释了
//   「封面能显示、角色图与声优图全是空白」这个只看图不看代码很难想通的现象。
//
// 因此这里把「哪些 host 才配得上 B 站站点头」收敛成一个纯函数，
// 让拦截器只在真正的 B 站生态域名上注入 Referer/Origin。
//
// 纯函数、无 Android 依赖，可直接单测。
package com.android.purebilibili.core.network.policy

/**
 * B 站生态域名后缀白名单。
 *
 * 只要 host 等于这些后缀，或以其为子域结尾，就认为该请求属于 B 站生态，
 * 可以安全地附加 `Referer` / `Origin` 等站点头。
 */
private val BILIBILI_HOST_SUFFIXES = listOf(
    "bilibili.com",
    "bilibili.tv",
    "b23.tv",
    "hdslb.com",        // 静态资源 / 图片 CDN
    "bilivideo.com",    // 视频 CDN
    "bilivideo.cn",
    "akamaized.net",    // B 站部分视频走 akamai（host 形如 upos-xxx.akamaized.net）
    "biliapi.net",
    "biliapi.com",
    "biligame.com",
    "bilicdn.com",
)

/**
 * 明确排除的第三方 host（即使碰巧命中上面的后缀也不会被当作 B 站）。
 *
 * 目前为空占位：保留扩展点，避免以后加后缀时误伤。
 */
private val NON_BILIBILI_HOST_SUFFIXES = listOf<String>()

/**
 * 判断该 host 是否属于 B 站生态。
 *
 * 命中规则（二者取一）：
 * - host 与后缀完全相同（`bilibili.com`）；
 * - host 是后缀的子域（`api.bilibili.com`、`upos-sz-mirror.bilivideo.com`）。
 *
 * 大小写不敏感；空串一律返回 false。
 */
fun isBilibiliEcosystemHost(host: String): Boolean {
    if (host.isEmpty()) return false
    val normalized = host.lowercase()
    if (NON_BILIBILI_HOST_SUFFIXES.any { normalized.matchesSuffix(it) }) return false
    return BILIBILI_HOST_SUFFIXES.any { normalized.matchesSuffix(it) }
}

/**
 * 是否允许向该 host 注入 B 站站点头（Referer / Origin）。
 *
 * 语义上就是 [isBilibiliEcosystemHost]，单独开一个名字是为了让调用点
 * 读起来是「策略决策」而不是「字符串判断」，也便于以后加例外规则。
 */
fun shouldAttachBilibiliSiteHeaders(host: String): Boolean = isBilibiliEcosystemHost(host)

/** host 是否等于 [suffix] 或为其子域。 */
private fun String.matchesSuffix(suffix: String): Boolean =
    this == suffix || endsWith(".$suffix")
