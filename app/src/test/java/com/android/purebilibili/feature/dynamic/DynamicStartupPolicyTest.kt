package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicStartupPolicyTest {

    @Test
    fun startupPlan_defersFollowingsHydrationUntilAfterPrimaryFeed() {
        val plan = resolveDynamicStartupLoadPlan()

        assertTrue(plan.refreshFeedImmediately)
        assertTrue(plan.loadLiveStatusImmediately)
        assertFalse(plan.loadFollowingsImmediately)
        assertEquals(1_200L, plan.followingsHydrationDelayMs)
        assertEquals(1, plan.initialFollowingsPageLimit)
    }

    @Test
    fun followingsPageBudget_isConservativeDuringStartupHydration() {
        assertEquals(1, resolveDynamicFollowingsPageLimit(isStartupHydration = true))
        assertEquals(3, resolveDynamicFollowingsPageLimit(isStartupHydration = false))
    }

    @Test
    fun followingsPagination_stopsOnShortPageOrReportedTotal() {
        assertTrue(
            hasLoadedAllDynamicFollowings(
                pageSize = 12,
                accumulatedCount = 62,
                reportedTotal = 0,
            )
        )
        assertTrue(
            hasLoadedAllDynamicFollowings(
                pageSize = 50,
                accumulatedCount = 100,
                reportedTotal = 100,
            )
        )
        assertFalse(
            hasLoadedAllDynamicFollowings(
                pageSize = 50,
                accumulatedCount = 50,
                reportedTotal = 120,
            )
        )
    }
}
