package com.android.purebilibili.feature.video.ui.feedback

fun resolveTripleActionFeedbackMessage(
    likeSuccess: Boolean,
    coinSuccess: Boolean,
    favoriteSuccess: Boolean,
    coinFailureMessage: String?
): String {
    if (likeSuccess && coinSuccess && favoriteSuccess) {
        return "三连完成"
    }

    val parts = buildList {
        if (likeSuccess) add("已点赞")
        if (coinSuccess) add("投币成功")
        if (favoriteSuccess) add("已收藏")
    }

    if (parts.isNotEmpty()) {
        return parts.joinToString(" ")
    }

    return coinFailureMessage?.takeIf { it.isNotBlank() } ?: "三连失败"
}
