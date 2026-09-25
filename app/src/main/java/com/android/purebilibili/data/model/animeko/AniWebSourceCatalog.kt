// 文件路径: data/model/animeko/AniWebSourceCatalog.kt
//
// 「Web 站点源」清单 —— 来自 animeko 社区的视频源配置
// (https://sub.creamycake.org/v1/css1.json 与 /v1/bt1.json)。
//
// ★ 与 maccms 采集源(AniOnlineCatalog)的区别:
//   maccms 源是标准 JSON 接口, 一步拿到 m3u8;
//   web 源是普通网站, 要走「搜索页 → 详情页 → 播放页」三跳,
//   从播放页的 JS 配置里把直链抠出来。通用提取顺序(实测 MacCMS 系模板):
//     1. data-play="aHR0..." —— Base64(可能掺前缀垃圾字符), 找 aHR0(=http) 起点解码
//     2. player_aaaa = {...,"url":"..."} —— MacCMS 播放器全局变量
//     3. <iframe src> 一层嵌套, 进去再抽一次
//     4. 明文 https://...m3u8|mp4
//   以上在真实站点上验证: E-ACG / 去看吧 全链路 200 OK 拿到 m3u8。
//
// ★ 测速优先(用户要求): 播放前先并发测各站延迟, 快的源排前面、先请求;
//   实测同一部番在不同站的响应时间能差 3~7 倍, 这一步收益明显。
package com.android.purebilibili.data.model.animeko

/** 一个 web 站点源的解析配置。 */
data class AniWebSourceConfig(
    /** 源 id(会加 web_ 前缀)。 */
    val id: String,
    val name: String,
    val homepage: String,
    /** 搜索 URL 模板, {keyword} 占位。 */
    val searchUrl: String,
    /** 搜索时只取番剧名第一个词(部分站对长名搜索效果差)。 */
    val useOnlyFirstWord: Boolean = false,
)

/** web 站点源清单(与 animeko 社区 css1.json 对齐)。 */
object AniWebSourceCatalog {

    val sources: List<AniWebSourceConfig> = listOf(
        AniWebSourceConfig(
            id = "web_eacg", name = "E-ACG",
            homepage = "https://www.eacg1.com",
            searchUrl = "https://www.eacg1.com/vodsearch/-------------.html?wd={keyword}&submit=",
        ),
        AniWebSourceConfig(
            id = "web_11kt", name = "去看吧",
            homepage = "https://11kt.net",
            searchUrl = "https://11kt.net/index.php/vod/search.html?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_jibi", name = "叽哔动漫",
            homepage = "https://www.jibi.cc",
            searchUrl = "https://www.jibi.cc/index.php/vod/search.html?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_fqdm", name = "番茄动漫",
            homepage = "https://www.fqdm.cc",
            searchUrl = "https://www.fqdm.cc/index.php/vod/search.html?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_wedm", name = "微动漫",
            homepage = "https://www.vdm5.com",
            searchUrl = "https://www.vdm5.com/search_-------------.html?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_didahd", name = "嘀嗒影视",
            homepage = "https://www.didahd.pro",
            searchUrl = "https://www.didahd.pro/search/-------------.html?wd={keyword}&submit=",
        ),
        AniWebSourceConfig(
            id = "web_rebozj", name = "热播之家",
            homepage = "https://www.rebozj.pro",
            searchUrl = "https://www.rebozj.pro/search/-------------.html?wd={keyword}&submit=",
        ),
        AniWebSourceConfig(
            id = "web_girigiri", name = "girigiri愛动漫",
            homepage = "https://ani.girigirilove.com",
            searchUrl = "https://ani.girigirilove.com/search/-------------/?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_senfun", name = "森之屋动漫",
            homepage = "https://senfun.in",
            searchUrl = "https://senfun.in/search.html?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_haixing", name = "海星动漫",
            homepage = "https://www.haixingdmx.com",
            searchUrl = "https://www.haixingdmx.com/s_all?ex=1&kw={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_yinghua", name = "樱花动漫",
            homepage = "https://www.yinghua2.com",
            searchUrl = "https://www.yinghua2.com/index.php/vod/search.html?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_dilidili", name = "嘀哩嘀哩",
            homepage = "https://dilidili.io",
            searchUrl = "https://dilidili.io/search?q={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_xfdm", name = "稀饭动漫",
            homepage = "https://dm1.xfdm.pro",
            searchUrl = "https://dm1.xfdm.pro/search.html?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_youknow", name = "新优酷",
            homepage = "https://www.youknow.tv",
            searchUrl = "https://www.youknow.tv/search/-------------/?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_hc34567", name = "影视森林",
            homepage = "http://www.hc34567.com",
            searchUrl = "http://www.hc34567.com/hcvodsearch/{keyword}----------1---.html",
        ),
    )

    /** bt1.json 里的 BT 聚合站(RSS)。 */
    val btSources: List<AniMediaSource> = listOf(
        AniMediaSource(
            id = "bt_nyaa",
            name = "nyaa.land(BT)",
            kind = AniMediaSourceKind.BT.name,
            homepage = "https://nyaa.land",
            urlTemplate = "https://nyaa.land/?page=rss&q={title}&c=0_0&f=0",
            note = "BT 资源聚合站, RSS 返回种子",
        ),
        AniMediaSource(
            id = "bt_animegarden",
            name = "AnimeGarden(BT)",
            kind = AniMediaSourceKind.BT.name,
            homepage = "https://garden.breadio.wiki",
            urlTemplate = "https://garden.breadio.wiki/feed.xml?filter=%5B%7B%22type%22%3A%22%E5%8A%A8%E7%94%BB%22%2C%22search%22%3A%5B%22{title}%22%5D%7D%5D",
            note = "动画 BT 资源聚合(Atom feed)",
        ),
    )

    /** 按 id 索引。 */
    val byId: Map<String, AniWebSourceConfig> = sources.associateBy { it.id }
}
