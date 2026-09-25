package com.android.purebilibili.feature.video.ui.section

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailMotionBudgetPolicyTest {

    @Test
    fun switchingOrScrolling_reducesVideoDetailBudget() {
        assertEquals(
            VideoDetailMotionBudget.REDUCED,
            resolveVideoDetailMotionBudget(
                isTabSwitching = true,
                isContentScrolling = false
            )
        )
        assertEquals(
            VideoDetailMotionBudget.REDUCED,
            resolveVideoDetailMotionBudget(
                isTabSwitching = false,
                isContentScrolling = true
            )
        )
    }

    @Test
    fun onlyFullBudget_allowsLayoutAnimation() {
        assertTrue(shouldAnimateVideoDetailLayout(VideoDetailMotionBudget.FULL))
        assertFalse(shouldAnimateVideoDetailLayout(VideoDetailMotionBudget.REDUCED))
    }
}
