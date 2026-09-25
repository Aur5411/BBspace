package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PopularFeedPolicyTest {

    @Test
    fun `only comprehensive popular source supports load more`() {
        assertTrue(supportsPopularLoadMore(PopularSubCategory.COMPREHENSIVE))
        assertFalse(supportsPopularLoadMore(PopularSubCategory.RANKING))
        assertFalse(supportsPopularLoadMore(PopularSubCategory.WEEKLY))
        assertFalse(supportsPopularLoadMore(PopularSubCategory.PRECIOUS))
    }

    @Test
    fun `weekly number should pick latest when candidates exist`() {
        assertEquals(279, resolveWeeklyNumberForRequest(listOf(1, 42, 279, 278)))
    }

    @Test
    fun `weekly number should fallback to one when list is empty`() {
        assertEquals(1, resolveWeeklyNumberForRequest(emptyList()))
    }
}
