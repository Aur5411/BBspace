package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.ReplyData

internal data class CommentPageResolution(
    val totalCount: Int,
    val isEnd: Boolean
)

internal fun resolveCommentPageResolution(
    data: ReplyData,
    pageToLoad: Int,
    previousRepliesSize: Int,
    combinedRepliesSize: Int,
    newRepliesSize: Int,
    fallbackCount: Int
): CommentPageResolution {
    val totalCount = maxOf(
        data.getAllCount(),
        fallbackCount,
        combinedRepliesSize.coerceAtLeast(0)
    )
    val uniqueGrowth = (combinedRepliesSize - previousRepliesSize).coerceAtLeast(0)
    val hasCursorPaginationSignal =
        data.cursor.allCount > 0 || data.cursor.next > 0 || data.cursor.isEnd
    val isEnd = if (hasCursorPaginationSignal) {
        data.cursor.isEnd || (pageToLoad > 1 && uniqueGrowth == 0)
    } else {
        data.getIsEnd(pageToLoad, combinedRepliesSize) ||
            (newRepliesSize == 0 && combinedRepliesSize == 0) ||
            (pageToLoad > 1 && uniqueGrowth == 0)
    }
    return CommentPageResolution(
        totalCount = totalCount,
        isEnd = isEnd
    )
}
