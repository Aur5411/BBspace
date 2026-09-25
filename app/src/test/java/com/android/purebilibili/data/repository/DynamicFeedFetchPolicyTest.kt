package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicFeedFetchPolicyTest {

    @Test
    fun `selected user feed pagination sends documented offset without ineffective page`() {
        val params = DynamicRepository.buildSelectedUserDynamicFeedParams(
            hostMid = 123L,
            offset = "next-offset"
        )

        assertEquals("next-offset", params["offset"])
        assertFalse("page" in params)
        assertTrue(params["features"].orEmpty().contains("opusBigCover"))
        assertTrue(params["features"].orEmpty().contains("commentsNewVersion"))
        assertEquals("333.1387", params["web_location"])
    }

    @Test
    fun `continue loading when no visible items yet and next page exists`() {
        assertTrue(
            shouldContinueDynamicFetchAfterFilter(
                accumulatedVisibleCount = 0,
                hasMore = true,
                previousOffset = "100",
                nextOffset = "200",
                pagesFetched = 1,
                maxPages = 3
            )
        )
    }

    @Test
    fun `stop loading when visible items already found`() {
        assertFalse(
            shouldContinueDynamicFetchAfterFilter(
                accumulatedVisibleCount = 2,
                hasMore = true,
                previousOffset = "100",
                nextOffset = "200",
                pagesFetched = 1,
                maxPages = 3
            )
        )
    }

    @Test
    fun `stop loading when has more is false`() {
        assertFalse(
            shouldContinueDynamicFetchAfterFilter(
                accumulatedVisibleCount = 0,
                hasMore = false,
                previousOffset = "100",
                nextOffset = "200",
                pagesFetched = 1,
                maxPages = 3
            )
        )
    }

    @Test
    fun `stop loading when offset does not move forward`() {
        assertFalse(
            shouldContinueDynamicFetchAfterFilter(
                accumulatedVisibleCount = 0,
                hasMore = true,
                previousOffset = "100",
                nextOffset = "100",
                pagesFetched = 1,
                maxPages = 3
            )
        )
    }

    @Test
    fun `stop loading when reaching max pages`() {
        assertFalse(
            shouldContinueDynamicFetchAfterFilter(
                accumulatedVisibleCount = 0,
                hasMore = true,
                previousOffset = "100",
                nextOffset = "200",
                pagesFetched = 3,
                maxPages = 3
            )
        )
    }

    @Test
    fun `incremental refresh continues until all reported updates are covered`() {
        assertTrue(
            shouldContinueDynamicIncrementalFetch(
                accumulatedItemCount = 20,
                updateNum = 25,
                hasMore = true,
                previousOffset = "",
                nextOffset = "200"
            )
        )

        assertFalse(
            shouldContinueDynamicIncrementalFetch(
                accumulatedItemCount = 40,
                updateNum = 25,
                hasMore = true,
                previousOffset = "200",
                nextOffset = "400"
            )
        )
    }

    @Test
    fun `incremental refresh stops when pagination cannot advance`() {
        assertFalse(
            shouldContinueDynamicIncrementalFetch(
                accumulatedItemCount = 20,
                updateNum = 25,
                hasMore = true,
                previousOffset = "200",
                nextOffset = "200"
            )
        )
    }

    @Test
    fun `multi page fetch keeps update baseline from first page`() {
        val firstPageBaseline = resolveDynamicFeedUpdateBaseline(
            currentBaseline = "old",
            responseBaseline = "newest",
            pagesFetched = 0
        )
        val secondPageBaseline = resolveDynamicFeedUpdateBaseline(
            currentBaseline = firstPageBaseline,
            responseBaseline = "older-page",
            pagesFetched = 1
        )

        assertTrue(firstPageBaseline == "newest")
        assertTrue(secondPageBaseline == "newest")
    }
}
