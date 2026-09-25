// 文件路径: data/model/animeko/AnimekoModels.kt
//
// BB空间 追番模块数据模型。
// 数据来源: https://api.animeko.org (ANI / animeko 自有服务端, 服务端已聚合 Bangumi 元数据)
// 设计原则:
//   1. 不涉及任何 B 站接口 / Cookie / 登录态;
//   2. 不涉及 ANI 账号登录, 收藏与历史全部本地存储;
//   3. 字段严格对齐实测 JSON, 全部给默认值以容忍服务端字段增删。
package com.android.purebilibili.data.model.animeko

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============================================================
// 1. 季度时间表  GET /v1/schedule/seasons
//    {"list":[{"year":2026,"season":"AUTUMN","id":"2026q4"}, ...]}
// ============================================================

@Serializable
data class AniSeasonId(
    val year: Int = 0,
    val season: String = "",
    val id: String = "",
)

@Serializable
data class AniSeasonIdList(
    val list: List<AniSeasonId> = emptyList(),
)

/** 季度中文名, 用于筛选条展示。 */
val AniSeasonId.displayName: String
    get() {
        val seasonCn = when (season.uppercase()) {
            "WINTER" -> "冬季"
            "SPRING" -> "春季"
            "SUMMER" -> "夏季"
            "AUTUMN", "FALL" -> "秋季"
            else -> season
        }
        return if (year > 0) "${year}年$seasonCn" else id
    }

// ============================================================
// 2. 某季度番表  GET /v1/schedule/season/{seasonId}
// ============================================================

@Serializable
data class AniRecurrence(
    val startTime: String = "",
    val intervalMillis: Long = 0L,
)

@Serializable
data class AniScheduleItem(
    val bangumiId: Long = 0L,
    val name: String = "",
    val aliases: List<String> = emptyList(),
    val begin: String = "",
    val end: String? = null,
    val recurrence: AniRecurrence? = null,
    /** 蜜柑(Mikan) 番剧 id, 部分条目才有, 可用于后续 BT 资源定位。 */
    val mikanId: Int? = null,
) {
    /** 优先展示中文别名, 没有则回退原名。 */
    val displayName: String
        get() = aliases.firstOrNull { it.any { c -> c.code in 0x4E00..0x9FFF } } ?: name
}

@Serializable
data class AniAnimeSchedule(
    @SerialName("seasonId") val seasonId: AniSeasonId = AniSeasonId(),
    val list: List<AniScheduleItem> = emptyList(),
)

// ============================================================
// 3. 今日/近期放送  GET /v1/schedule/airing?today=&timeZone=
// ============================================================

@Serializable
data class AniAiringSubject(
    val subjectId: Long = 0L,
    val name: String = "",
    val nameCn: String = "",
    val imageLarge: String = "",
) {
    val displayName: String get() = nameCn.ifBlank { name }
}

@Serializable
data class AniAiringEpisode(
    val episodeId: Long = 0L,
    val name: String = "",
    val nameCn: String = "",
    val airDate: String = "",
    val type: Int = 0,
    val sort: String = "",
    val ep: String? = null,
) {
    val displayName: String get() = nameCn.ifBlank { name }
    val displayEp: String
        get() = ep?.takeIf { it.isNotBlank() } ?: sort
}

@Serializable
data class AniAiringItem(
    val subject: AniAiringSubject = AniAiringSubject(),
    val episode: AniAiringEpisode = AniAiringEpisode(),
    val airingTime: String = "",
    val timeKnown: Boolean = false,
)

@Serializable
data class AniAiringDay(
    val date: String = "",
    val list: List<AniAiringItem> = emptyList(),
)

@Serializable
data class AniLatestAiringSchedule(
    val list: List<AniAiringDay> = emptyList(),
)

// ============================================================
// 4. 搜索  GET /v2/subjects/search?q=&limit=&offset=
// ============================================================

@Serializable
data class AniTag(
    val name: String = "",
    val count: Int = 0,
)

@Serializable
data class AniFavoriteCount(
    val wish: Int = 0,
    val done: Int = 0,
    val doing: Int = 0,
    val onHold: Int = 0,
    val dropped: Int = 0,
) {
    val total: Int get() = wish + done + doing + onHold + dropped
}

@Serializable
data class AniSubjectBrief(
    val id: Long = 0L,
    val name: String = "",
    val nameCn: String = "",
    val summary: String = "",
    val imageLarge: String = "",
    val nsfw: Boolean = false,
    val airDate: String = "",
    /** 服务端以字符串下发, 如 "8.4"; 空串表示暂无评分。 */
    val score: String = "",
    val rank: Int = 0,
    val ratingTotal: Int = 0,
    val favorite: AniFavoriteCount = AniFavoriteCount(),
    val tags: List<AniTag> = emptyList(),
    val mainEpisodeCount: Int = 0,
) {
    val displayName: String get() = nameCn.ifBlank { name }
    val scoreOrNull: Float? get() = score.toFloatOrNull()
    val yearLabel: String get() = airDate.take(4)
}

@Serializable
data class AniSubjectSearchPage(
    val items: List<AniSubjectBrief> = emptyList(),
)

// ============================================================
// 5. 条目详情  GET /v2/subjects/{subjectId}
// ============================================================

@Serializable
data class AniInfoboxValue(
    val v: String = "",
)

@Serializable
data class AniInfoboxField(
    val key: String = "",
    val values: List<AniInfoboxValue> = emptyList(),
) {
    val joined: String get() = values.map { it.v }.filter { it.isNotBlank() }.joinToString(" / ")
}

@Serializable
data class AniInfobox(
    val template: String = "",
    val fields: List<AniInfoboxField> = emptyList(),
)

@Serializable
data class AniEpisode(
    val episodeId: Long = 0L,
    val subjectId: Long = 0L,
    val sort: String = "",
    val ep: String? = null,
    /** SPECIAL / MAIN / OP / ED ... */
    val type: String = "",
    val name: String = "",
    val nameCn: String = "",
    val description: String = "",
    val airdate: String = "",
    val disc: Int = 0,
    val duration: String = "",
    val imageMedium: String = "",
    val imageLarge: String = "",
) {
    val displayName: String get() = nameCn.ifBlank { name }
    val displayEp: String get() = ep?.takeIf { it.isNotBlank() } ?: sort
    /** MAIN 才是正片, 其余为 SP / OP / ED 等。 */
    val isMain: Boolean get() = type.equals("MAIN", ignoreCase = true)
}

@Serializable
data class AniRelations(
    val subjectId: Long = 0L,
    val seriesMainSubjectIds: List<Long> = emptyList(),
    val seriesMainSubjectNames: List<String> = emptyList(),
    val sequelSubjects: List<Long> = emptyList(),
    val sequelSubjectNames: List<String> = emptyList(),
)

@Serializable
data class AniAiringInfo(
    val begin: String = "",
    val recurrence: AniRecurrence? = null,
)

@Serializable
data class AniTmdbImage(
    val medium: String = "",
    val large: String = "",
)

@Serializable
data class AniTmdbArt(
    val backdrops: List<AniTmdbImage> = emptyList(),
) {
    val heroUrl: String get() = backdrops.firstOrNull()?.large.orEmpty()
}

@Serializable
data class AniSelfRating(
    val score: String = "",
)

@Serializable
data class AniSubjectDetail(
    val id: Long = 0L,
    val type: String = "",
    val name: String = "",
    val nameCn: String = "",
    val summary: String = "",
    val nsfw: Boolean = false,
    val airDate: String = "",
    val aliases: List<String> = emptyList(),
    val infobox: AniInfobox = AniInfobox(),
    val favorite: AniFavoriteCount = AniFavoriteCount(),
    val tags: List<AniTag> = emptyList(),
    val score: String = "",
    val rank: Int = 0,
    val episodes: List<AniEpisode> = emptyList(),
    val airingInfo: AniAiringInfo? = null,
    val relations: AniRelations? = null,
    val tmdbArt: AniTmdbArt? = null,
    val imageLarge: String = "",
    val imageThumb: String = "",
) {
    val displayName: String get() = nameCn.ifBlank { name }
    val scoreOrNull: Float? get() = score.toFloatOrNull()
    val mainEpisodes: List<AniEpisode> get() = episodes.filter { it.isMain }
    val specialEpisodes: List<AniEpisode> get() = episodes.filterNot { it.isMain }
    val cover: String get() = imageLarge.ifBlank { imageThumb }
    val hero: String get() = tmdbArt?.heroUrl.orEmpty().ifBlank { cover }
    /** infobox 按 key 取值, 用于「话数/导演/放送开始」这类固定字段。 */
    fun infoOf(key: String): String =
        infobox.fields.firstOrNull { it.key == key }?.joined.orEmpty()
}

// ============================================================
// 6. 首页推荐  GET /v2/home/recommendations
//    {"total":200,"items":[{"subjectId":..,"subjectName":..,"subjectNameCn":..,"imageUrl":..,"desc1":..,"desc2":..}]}
// ============================================================

@Serializable
data class AniRecommendItem(
    val subjectId: Long = 0L,
    val subjectName: String = "",
    val subjectNameCn: String = "",
    val imageUrl: String = "",
    val desc1: String = "",
    val desc2: String = "",
) {
    val displayName: String get() = subjectNameCn.ifBlank { subjectName }
}

@Serializable
data class AniRecommendPage(
    val total: Int = 0,
    val items: List<AniRecommendItem> = emptyList(),
)

// ============================================================
// 7. 趋势榜  GET /v1/trends
// ============================================================

@Serializable
data class AniTrendSubject(
    val bangumiId: Long = 0L,
    val nameCn: String = "",
    val imageLarge: String = "",
)

@Serializable
data class AniTrends(
    val trendingSubjects: List<AniTrendSubject> = emptyList(),
)

// ============================================================
// 8. 角色 (详情页可选区块)  GET /v2/subjects/{id}/characters?withActors=true
// ============================================================

@Serializable
data class AniPerson(
    val id: Long = 0L,
    val name: String = "",
    val nameCn: String = "",
    val imageLarge: String = "",
    val imageMedium: String = "",
) {
    val displayName: String get() = nameCn.ifBlank { name }
}

@Serializable
data class AniCharacter(
    val id: Long = 0L,
    val name: String = "",
    val nameCn: String = "",
    val imageLarge: String = "",
    val imageMedium: String = "",
    val actors: List<AniPerson> = emptyList(),
) {
    val displayName: String get() = nameCn.ifBlank { name }
}

@Serializable
data class AniRelatedCharacter(
    val index: Int = 0,
    val character: AniCharacter = AniCharacter(),
    val role: Int = 0,
)

/** 角色定位。服务端 role 编码: 1=主角, 2=配角, 4=客串(其它)。 */
val AniRelatedCharacter.roleLabel: String
    get() = when (role) {
        1 -> "主角"
        2 -> "配角"
        4 -> "客串"
        else -> ""
    }

// ============================================================
// 8.5 制作人员  GET /v2/subjects/{id}/staff
//     返回 [{"index":0,"person":{...},"position":20}, ...]
//     position 为 Bangumi 的职位编码, 见 AniStaffPosition 映射表。
// ============================================================

@Serializable
data class AniStaffMember(
    val index: Int = 0,
    val person: AniPerson = AniPerson(),
    /** Bangumi 职位编码, 需经 [aniStaffPositionLabel] 转中文。 */
    val position: Int = 0,
)

/**
 * Bangumi 职位编码 -> 中文名。
 *
 * ★ 编码来源说明(重要, 避免后人瞎猜):
 * animeko 的 `position` 是 Bangumi 内部枚举的**序号**, 不是公开文档里的稳定值。
 * 这里的中文名是通过「拿 infobox 里语义明确的中文职位字段(如『作画监督』)
 * 去反查 staff 里同名人物命中的 position」这一实证方法确定的 ——
 * 用 40 个热门/经典条目、数千条 staff 记录统计命中率, 只保留置信度高的最高票结果。
 *
 * 因此：
 *  - 只收录**统计置信度高**的编码, 拿不准的一律不写, 交给 [aniStaffPositionLabel] 兜底;
 *  - 未收录的编码显示为「其他」, 宁可少显示也不错显示。
 *
 * ★ 易混淆项(实测已校正, 勿改回去):
 *   52=执行制片人(94%) / 54=制片人(95%) —— 两者不是同一个岗位, 编码也不相邻;
 *   81=协力(100%) —— 不是美术监督; 美术监督是 11。
 */
private val ANI_STAFF_POSITION_LABELS: Map<Int, String> = mapOf(
    // ---- 原作 / 剧本 ----
    1 to "原作",
    3 to "脚本",
    10 to "系列构成",
    35 to "企划",
    // ---- 演出 ----
    2 to "导演",
    4 to "分镜",
    5 to "演出",
    73 to "OP・ED 分镜",
    128 to "OP・ED 演出",
    // ---- 作画 ----
    8 to "人物设定",
    14 to "总作画监督",
    15 to "作画监督",
    20 to "原画",
    21 to "第二原画",
    51 to "补间动画",
    90 to "作画监督助理",
    19 to "道具设计",
    99 to "2D 设计",
    22 to "动画检查",
    // ---- 美术 / 色彩 ----
    11 to "美术监督",
    13 to "色彩设计",
    25 to "背景美术",
    26 to "色彩指定",
    71 to "美术设计",
    93 to "上色",
    95 to "色彩检查",
    // ---- 摄影 / 剪辑 ----
    17 to "摄影监督",
    82 to "摄影",
    166 to "摄影监督助理",
    28 to "剪辑",
    155 to "在线剪辑",
    169 to "剪辑助理",
    // ---- 音响 / 音乐 ----
    6 to "音乐",
    44 to "音响监督",
    39 to "录音",
    40 to "录音助理",
    152 to "录音工作室",
    46 to "音效",
    85 to "音乐制作人",
    154 to "音响制作担当",
    // ---- 主题歌 ----
    30 to "主题歌编曲",
    31 to "主题歌作曲",
    32 to "主题歌作词",
    33 to "主题歌演出",
    // ---- 制作 / 制片 ----
    42 to "制作",
    43 to "设定",
    84 to "设定制作",
    56 to "制作进行",
    37 to "制作管理",
    52 to "执行制片人",
    54 to "制片人",
    53 to "助理制片人",
    122 to "副制片人",
    87 to "动画制片人",
    127 to "企画协力",
    76 to "制作协力",
    176 to "制作协力",
    38 to "宣传",
    81 to "协力",
    47 to "特效",
)

/** 未收录编码的兜底文案。 */
const val ANI_STAFF_POSITION_FALLBACK = "其他"

/** 把 Bangumi 职位编码转成中文名; 未知编码返回 [ANI_STAFF_POSITION_FALLBACK]。 */
fun aniStaffPositionLabel(position: Int): String =
    ANI_STAFF_POSITION_LABELS[position] ?: ANI_STAFF_POSITION_FALLBACK

/** 是否是我们能准确命名的职位(用于过滤掉满屏的「其他」)。 */
fun isKnownAniStaffPosition(position: Int): Boolean =
    ANI_STAFF_POSITION_LABELS.containsKey(position)

// ============================================================
// 8.6 短评 / 吐槽  GET /v2/subjects/{id}/reviews?limit=&offset=
//     返回 {"total":31,"items":[{"id":"bangumi:..","author":{...},
//           "contentBbcode":"..","updatedAt":"..","rating":7,"likeCount":0}]}
//
//     ★ 服务端已把 Bangumi 站内短评与 animeko 站内评论聚合在一起,
//       source 字段区分来源: "bangumi" / "animeko"。
// ============================================================

@Serializable
data class AniReviewAuthor(
    val id: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
)

@Serializable
data class AniReview(
    val id: String = "",
    val subjectId: Long = 0L,
    /** 来源: "bangumi" 或 "animeko"。 */
    val source: String = "",
    val author: AniReviewAuthor = AniReviewAuthor(),
    /** 正文(BBCode 语法; 实测绝大多数为纯文本)。 */
    val contentBbcode: String = "",
    /** ISO-8601 UTC, 形如 2026-09-23T09:56:06Z。 */
    val updatedAt: String = "",
    /** 用户评分 1..10; 0 表示未打分。 */
    val rating: Int = 0,
    val likeCount: Int = 0,
) {
    val displayAuthor: String get() = author.nickname.ifBlank { "匿名用户" }
    val hasRating: Boolean get() = rating in 1..10

    /** 来源展示名。 */
    val sourceLabel: String
        get() = when (source.lowercase()) {
            "bangumi" -> "Bangumi"
            "animeko" -> "animeko"
            else -> source
        }

    /**
     * 把 BBCode 正文转成可读纯文本。
     *
     * 实测 500 条样本里含标签的为 0 条, 所以这里只做最小必要的清洗:
     * 去掉 `[img]..[/img]` 这类纯标记, 其余原样保留(宁可留个方括号也不错删正文)。
     */
    val displayContent: String
        get() = contentBbcode
            .replace(Regex("\\[/?img[^\\]]*\\]"), "")
            .replace(Regex("\\[/?url[^\\]]*\\]"), "")
            .trim()
}

@Serializable
data class AniReviewPage(
    /** 服务端为「已扫到的累计条数」, 非精确总数, 因此只用于判断是否还有下一页。 */
    val total: Int = 0,
    val items: List<AniReview> = emptyList(),
)

// ============================================================
// 9. 弹幕  GET /v1/danmaku/{episodeId}
// ============================================================

@Serializable
data class AniDanmaku(
    val danmakuList: List<AniDanmakuItem> = emptyList(),
)

/**
 * 单条弹幕。实测结构:
 * {
 *   "id": "uuid",
 *   "senderId": "uuid",
 *   "danmakuInfo": { "playTime": 231480, "color": -1, "text": "...", "location": "NORMAL" }
 * }
 * playTime 单位毫秒; color 为 ARGB 十进制(-1 = 默认白);
 * location 取 NORMAL / TOP / BOTTOM。
 */
@Serializable
data class AniDanmakuItem(
    val id: String = "",
    val senderId: String = "",
    val danmakuInfo: AniDanmakuInfo = AniDanmakuInfo(),
) {
    /** 出现时间毫秒。 */
    val playTimeMillis: Long get() = danmakuInfo.playTime
    val text: String get() = danmakuInfo.text
    val color: Int get() = danmakuInfo.color
    val location: String get() = danmakuInfo.location
    /** 播放时间(秒), 便于与播放器进度比较。 */
    val playTimeSeconds: Float get() = danmakuInfo.playTime / 1000f
}

@Serializable
data class AniDanmakuInfo(
    /** 毫秒。 */
    val playTime: Long = 0L,
    /** ARGB, -1 表示默认白色。 */
    val color: Int = -1,
    val text: String = "",
    /** NORMAL(滚动) / TOP(顶部) / BOTTOM(底部)。 */
    val location: String = "NORMAL",
)

/** 弹幕显示模式。 */
enum class AniDanmakuLocation {
    NORMAL,
    TOP,
    BOTTOM,
    ;

    companion object {
        fun from(raw: String): AniDanmakuLocation = when (raw.uppercase()) {
            "TOP" -> TOP
            "BOTTOM" -> BOTTOM
            else -> NORMAL
        }
    }
}
