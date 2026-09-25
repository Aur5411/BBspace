package com.android.purebilibili.feature.video.screen

internal fun resolveVideoDetailBeyondViewportPageCount(
    isVideoPlaying: Boolean,
    selectedTabIndex: Int = 0
): Int = when {
    isVideoPlaying -> 0
    // 停在评论 Tab 时不必保活简介页，减少双 LazyColumn 同存。
    selectedTabIndex == 1 -> 0
    else -> 1
}

internal fun shouldLoadMoreVideoComments(
    lastVisibleItemIndex: Int,
    totalItemsCount: Int,
    isLoading: Boolean,
    isEnd: Boolean,
    prefetchThreshold: Int = 3
): Boolean {
    if (isLoading || isEnd) return false
    if (totalItemsCount <= 0 || lastVisibleItemIndex < 0) return false
    return lastVisibleItemIndex >= totalItemsCount - 1 - prefetchThreshold
}

internal fun shouldUseLightweightCommentRendering(
    selectedTabIndex: Int,
    isVideoPlaying: Boolean,
    isCommentListScrolling: Boolean = false
): Boolean {
    if (selectedTabIndex != 1) return false
    // 仅在滚动中或播放中临时降级附属装饰；静止后恢复完整视觉效果。
    return isVideoPlaying || isCommentListScrolling
}