package com.android.purebilibili.data.repository

internal const val DYNAMIC_EMPTY_PAGE_FETCH_LIMIT = 3

internal fun shouldContinueDynamicFetchAfterFilter(
    accumulatedVisibleCount: Int,
    hasMore: Boolean,
    previousOffset: String,
    nextOffset: String,
    pagesFetched: Int,
    maxPages: Int = DYNAMIC_EMPTY_PAGE_FETCH_LIMIT
): Boolean {
    if (accumulatedVisibleCount > 0) return false
    if (!hasMore) return false
    if (pagesFetched >= maxPages) return false

    val previous = previousOffset.trim()
    val next = nextOffset.trim()
    if (next.isBlank()) return false
    if (next == previous) return false

    return true
}

internal fun shouldContinueDynamicIncrementalFetch(
    accumulatedItemCount: Int,
    updateNum: Int,
    hasMore: Boolean,
    previousOffset: String,
    nextOffset: String
): Boolean {
    if (accumulatedItemCount >= updateNum.coerceAtLeast(0)) return false
    if (!hasMore) return false

    val previous = previousOffset.trim()
    val next = nextOffset.trim()
    if (next.isBlank()) return false
    if (next == previous) return false

    return true
}

internal fun resolveDynamicFeedUpdateBaseline(
    currentBaseline: String,
    responseBaseline: String,
    pagesFetched: Int
): String {
    if (pagesFetched > 0) return currentBaseline
    return responseBaseline.ifBlank { currentBaseline }
}
