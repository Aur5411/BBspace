// 文件路径: data/model/animeko/AniCategoryCatalog.kt
//
// 「番剧分类」清单 —— 供番剧首页「分类」页签使用。
//
// ★ 数据来源与实证说明(勿凭想象改动):
//   animeko 服务端**没有**分类枚举接口(实测 /v2/subjects/categories、
//   /v1/tags、/v1/rank、/v2/explore 全部 400/404)。
//   唯一的分类浏览入口是 **搜索接口的 tags 参数**:
//
//       GET /v2/subjects/search?q=&limit=30&offset=0&tags=剧情
//
//   实测这个组合有效:
//     - q 允许为空串(纯按标签浏览), limit 上限 100, offset 可翻页;
//     - 多标签用英文逗号连接, 语义是 AND: tags=剧情,TV;
//     - 标签本身是 Bangumi 中文标签, 直接用中文即可, 无需编码映射。
//
//   所以这里只负责给出「一批实测存在、且内容量足够」的标签名,
//   请求拼装、分页、解析全部复用 AnimekoRepository.searchByTags()。
//
// ★ sortBy 枚举(实测非法值会 400 报错): 只有 RELEVANCE 合法。
//   服务端错误信息会打印枚举全名 me.him188.ani.danmaku.server.routes.subjects.SubjectSearchSortBy,
//   逐个试过 SCORE / RANK / HEAT / RATING / FAVORITE / AIR_DATE / DATE / LATEST 均 400。
package com.android.purebilibili.data.model.animeko

/** 一个分类分组(标题 + 组内标签)。 */
data class AniCategoryGroup(
    val title: String,
    val tags: List<String>,
)

/**
 * 番剧分类清单。
 *
 * 分两层: [primary] 是用户最常用的题材(首页横向筛选条直接铺出来),
 * [all] 是完整分组清单(「全部分类」面板里按组展示)。
 */
object AniCategoryCatalog {

    /** 首页横向筛选条上默认展示的题材(顺序即展示顺序, 按热度排)。 */
    val primary: List<String> = listOf(
        "剧情", "动画", "TV", "日本", "漫画改", "小说改", "原创",
        "搞笑", "恋爱", "奇幻", "战斗", "日常", "校园", "科幻",
        "治愈", "热血", "悬疑", "运动", "音乐", "机战", "后宫", "偶像",
    )

    /** 完整分组清单(「全部分类」面板)。 */
    val all: List<AniCategoryGroup> = listOf(
        AniCategoryGroup(
            title = "题材",
            tags = listOf(
                "剧情", "搞笑", "奇幻", "战斗", "科幻", "悬疑", "恋爱",
                "日常", "治愈", "热血", "运动", "音乐", "历史", "机战",
                "后宫", "偶像", "百合", "耽美", "猎奇", "恐怖",
            ),
        ),
        AniCategoryGroup(
            title = "受众",
            tags = listOf("少年向", "少女向", "青年向", "成人向", "女性向", "子供向"),
        ),
        AniCategoryGroup(
            title = "形式",
            tags = listOf("TV", "OVA", "剧场版", "泡面番", "WEB", "短片", "CM", "PV"),
        ),
        AniCategoryGroup(
            title = "来源",
            tags = listOf("漫画改", "小说改", "原创", "游戏改", "轻小说改", "绘本改", "轻改", "漫改"),
        ),
        AniCategoryGroup(
            title = "地区",
            tags = listOf("日本", "中国", "美国", "韩国", "欧美", "国产"),
        ),
        AniCategoryGroup(
            title = "制作公司",
            tags = listOf(
                "MADHOUSE", "京都动画", "京阿尼", "ufotable", "BONES", "MAPPA",
                "J.C.STAFF", "SHAFT", "SUNRISE", "东映动画", "CloverWorks",
                "TRIGGER", "P.A.WORKS", "动画工房", "吉卜力", "芳文社",
            ),
        ),
        AniCategoryGroup(
            title = "风格 / 题材补充",
            tags = listOf(
                "异世界", "穿越", "催泪", "青春", "神作", "中二病", "推理",
                "魔法少女", "空想科学", "赛博朋克", "萌", "轻百合", "人生", "公路片",
                "百合", "耽美", "猎奇", "恐怖",
            ),
        ),
        AniCategoryGroup(
            title = "年份",
            tags = listOf(
                "2026", "2025", "2024", "2023", "2022", "2021", "2020",
                "2019", "2018", "2017", "2016", "2015", "2014",
            ),
        ),
        AniCategoryGroup(
            title = "经典年代",
            tags = listOf("2013", "2012", "2011", "2010", "2009", "2008", "2007"),
        ),
        AniCategoryGroup(
            title = "更早",
            tags = listOf("2006", "2005", "2004", "2003", "2002", "2001", "2000"),
        ),
    )

    /** 全部去重标签, 便于校验用。 */
    val allTags: List<String> = (primary + all.flatMap { it.tags }).distinct()
}
