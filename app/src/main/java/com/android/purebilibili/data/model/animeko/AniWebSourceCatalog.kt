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

    /**
     * ★ 2026-10 实测清单(中国直连, 见 README「番剧源实测」)。
     *   两个测试番剧交叉验证：**孤独摇滚** + **进击的巨人**。
     *
     * 判定标准: 搜索页 200 且能定位到条目链接(非 Cloudflare 挑战页、非超时)。
     * 17 个源对两部番剧均搜索可达。
     * 全链路抽查(`搜索→详情→播放页`)确认下列源可抠出 m3u8 直链:
     *   叽哔动漫 / 番茄动漫 / ssrfun / 追剧影院 / 樱之空动漫 / 影视森林
     *
     * 下列源搜索与详情都通, 但播放页直链是「播放器标识+加密串」形态
     * (如微动漫 player_aaaa.url = "yinghuazy_8505b652e724..."), 静态抓取拿不到,
     * 需按站补播放器解析规则 —— 保留在清单里备用:
     *   微动漫 / 樱花动漫 / 异世界动漫 / 七色番 / 蚂蚁影视 / 森之屋动漫
     *   / girigiri愛动漫 / 樱花风车动漫
     *
     * 已淘汰(不再保留):
     *   - E-ACG(eacg1.com)        → HTTP 403(反爬)
     *   - 去看吧(11kt.net)         → HTTP 403
     *   - 新优酷(youknow.tv)       → HTTP 403
     *   - 嘀嗒影视(didahd.pro)     → Cloudflare "Just a moment" 挑战页
     *   - girigiri 主域 / hanime1  → 连接超时(被墙)
     *   - 稀饭动漫/热播之家/MX动漫/萌番/次元城/迷影社/橘子/佩可爱/新动漫网
     *     /酱紫社/风铃/米粒/高清点播/MiFun/中国动漫网/饭团  → 搜索页无结果
     *   - 蜜柑计划/动漫花园/末日动漫/acgnx 等 RSS 归入 btSources 处理
     */
    val sources: List<AniWebSourceConfig> = listOf(
        // ---- 全链路实测可出直链的源(优先) ----
        AniWebSourceConfig(
            id = "web_jibi", name = "叽哔动漫",
            homepage = "https://www.jibi.cc",
            searchUrl = "https://www.jibi.cc/index.php/vod/search.html?wd={keyword}",
            useOnlyFirstWord = true,
        ),
        AniWebSourceConfig(
            id = "web_fqdm", name = "番茄动漫",
            homepage = "https://www.fqdm.cc",
            searchUrl = "https://www.fqdm.cc/index.php/vod/search.html?wd={keyword}",
            useOnlyFirstWord = true,
        ),
        AniWebSourceConfig(
            id = "web_ssrfun", name = "ssrfun",
            homepage = "https://www.ssrfun.com",
            searchUrl = "https://www.ssrfun.com/vodsearch/-------------.html?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_pzlyw", name = "追剧影院",
            homepage = "https://pzlyw.com",
            searchUrl = "https://pzlyw.com/vodsearch/{keyword}----------1---.html",
        ),
        AniWebSourceConfig(
            id = "web_skr2", name = "樱之空动漫",
            homepage = "https://skr.skr2.cc:666",
            searchUrl = "https://skr.skr2.cc:666/vodsearch/-------------/?wd={keyword}&submit=",
        ),
        // ---- 搜索可达、链路结构不同(详情页需再多一跳), 保留备用 ----
        AniWebSourceConfig(
            id = "web_7sefun", name = "七色番",
            homepage = "https://www.7sefun.top",
            searchUrl = "https://www.7sefun.top/vodsearch/-------------.html?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_senfun", name = "森之屋动漫",
            homepage = "https://senfun.in",
            searchUrl = "https://senfun.in/search.html?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_wedm", name = "微动漫",
            homepage = "https://www.vdm5.com",
            searchUrl = "https://www.vdm5.com/search_-------------.html?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_yinghua2", name = "樱花动漫",
            homepage = "https://www.yinghua2.com",
            searchUrl = "https://www.yinghua2.com/index.php/vod/search.html?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_dmmiku", name = "异世界动漫",
            homepage = "https://www.dmmiku.com",
            searchUrl = "https://www.dmmiku.com/index.php/vod/search.html?wd={keyword}&submit=",
            useOnlyFirstWord = true,
        ),
        AniWebSourceConfig(
            id = "web_mayi520", name = "蚂蚁影视",
            homepage = "https://www.mayi520.cc",
            searchUrl = "https://www.mayi520.cc/vodsearch/-------------.html?wd={keyword}&sq=yes",
        ),
        AniWebSourceConfig(
            id = "web_605dm", name = "樱花风车动漫",
            homepage = "https://www.605dm.com",
            searchUrl = "https://www.605dm.com/search/-------------.html?wd={keyword}&submit=",
        ),
        AniWebSourceConfig(
            id = "web_akianime", name = "秋之动漫",
            homepage = "https://www.akianime.cc",
            searchUrl = "https://www.akianime.cc/bgmsearch/-------------.html?wd={keyword}",
        ),
        // 2026-10 保留原清单中实测仍可达的源
        AniWebSourceConfig(
            id = "web_girigiri", name = "girigiri愛动漫",
            homepage = "https://ani.girigirilove.com",
            searchUrl = "https://ani.girigirilove.com/search/-------------/?wd={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_haixing", name = "海星动漫",
            homepage = "https://www.haixingdmx.com",
            searchUrl = "https://www.haixingdmx.com/s_all?ex=1&kw={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_dilidili", name = "嘀哩嘀哩",
            homepage = "https://dilidili.io",
            searchUrl = "https://dilidili.io/search?q={keyword}",
        ),
        AniWebSourceConfig(
            id = "web_hc34567", name = "影视森林",
            homepage = "http://www.hc34567.com",
            searchUrl = "http://www.hc34567.com/hcvodsearch/{keyword}----------1---.html",
        ),
        // ★ 2026-10 回归实测淘汰:
        //   - 稀饭动漫(dm1.xfdm.pro)  → 搜索页 200 但无结果(该站已停更/改版)
        //   - 新优酷(youknow.tv)      → HTTPError(403 反爬)
    )

    /** bt1.json 里的 BT 聚合站(RSS)。 */
    /**
     * BT / 磁力源清单。
     *
     * ★ 2026-10 实测(中国直连, 「孤独摇滚」专项验证):
     *   - 蜜柑 mikanime.tv        ✅ 200, ~1.85s, 返回结果
     *   - AnimeGarden garden.breadio.wiki ✅ 200, ~2.5s(Atom)
     *   - nyaa.land               ❌ 403 + Cloudflare "Just a moment..." 挑战页,
     *                                非浏览器客户端永远拿不到数据, 已移除
     *   - nyaa.si / dmhy / acg.rip / bangumi.moe / miobt / acgnx  ❌ 超时(被墙)
     *   - kisssub(爱恋) / comicat(漫猫)  ⚠️ 站点可达但 RSS 的 keyword 参数无效
     *                                (任意关键词都返回全站最新 50 条), 不能当搜索源
     */
    val btSources: List<AniMediaSource> = listOf(
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
