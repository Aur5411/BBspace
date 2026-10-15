// 文件路径: data/model/animeko/AniEpisodeCommentModels.kt
//
// 单集「吐槽」数据模型 —— 来自 animeko 服务端 `GET /v2/episodes/{episodeId}/comments`。
//
// ★ 与详情页短评(reviews)同源, 都在 api.animeko.org, 国内可达, 稳定。
// ★ 播放页只做「看」: 展示作者/时间/正文/图片/已有回复, 不做点赞/发表/回复等交互。
package com.android.purebilibili.data.model.animeko

import kotlinx.serialization.Serializable

/** 单集吐槽作者。 */
@Serializable
data class AniEpisodeCommentAuthor(
    val id: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
)

/** 楼中楼回复(一条)。 */
@Serializable
data class AniEpisodeReply(
    val id: String = "",
    val author: AniEpisodeCommentAuthor = AniEpisodeCommentAuthor(),
    /** 正文(BBCode 语法, 可能含 [img] 标签)。 */
    val contentBbcode: String = "",
    val createdAtMillis: Long = 0L,
) {
    val displayAuthor: String get() = author.nickname.ifBlank { "匿名用户" }
}

/** 一条单集吐槽。 */
@Serializable
data class AniEpisodeComment(
    val id: String = "",
    val sourceCommentId: String = "",
    val episodeId: Long = 0L,
    val author: AniEpisodeCommentAuthor = AniEpisodeCommentAuthor(),
    /** 正文(BBCode 语法)。 */
    val contentBbcode: String = "",
    val createdAtMillis: Long = 0L,
    /** 回复总数(与 briefReplies 的条数可能不一致, 服务端只回前几条)。 */
    val replyCount: Int = 0,
    /** 已有的楼中楼回复(服务端返回的前几条, 纯展示)。 */
    val briefReplies: List<AniEpisodeReply> = emptyList(),
    /** 来源: "animeko" / "bangumi"。 */
    val source: String = "",
    val likeCount: Int = 0,
) {
    val displayAuthor: String get() = author.nickname.ifBlank { "匿名用户" }
    val hasReplies: Boolean get() = briefReplies.isNotEmpty()

    /** BBCode 正文转纯文本(保留换行, 去掉标签, 图片另存)。 */
    val displayContent: String
        get() = contentBbcode
            .let { Regex("\\[/?img[^\\]]*\\]").replace(it, "") }
            .let { Regex("\\[/?url[^\\]]*\\]").replace(it, "") }
            .replace("[br]", "\n")
            .replace("[/br]", "")
            .trim()

    /** 正文里解析出的图片地址。 */
    val images: List<String>
        get() = Regex("\\[img\\]\\s*(https?://[^\\[\\]]+?)\\s*\\[/img\\]", RegexOption.IGNORE_CASE)
            .findAll(contentBbcode)
            .map { it.groupValues[1].trim() }
            .filter { it.startsWith("http") }
            .distinct()
            .toList()
}

/** 单集吐槽分页结果。 */
@Serializable
data class AniEpisodeCommentPage(
    val total: Int = 0,
    val items: List<AniEpisodeComment> = emptyList(),
    /** 是否因 Bangumi 不可用而缺数据(仅提示用)。 */
    val bangumiUnavailable: Boolean = false,
) {
    val isEmpty: Boolean get() = items.isEmpty()
}
