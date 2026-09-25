package com.android.purebilibili.feature.home

internal fun supportsPopularLoadMore(subCategory: PopularSubCategory): Boolean {
    return subCategory == PopularSubCategory.COMPREHENSIVE
}

internal fun resolveWeeklyNumberForRequest(candidates: List<Int>): Int {
    return candidates.maxOrNull() ?: 1
}
