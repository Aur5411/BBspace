package com.android.purebilibili.feature.video.ui.section

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoPlayerUiLayoutPolicyTest {

    @Test
    fun phoneProfile_usesCompactControlTargets() {
        val policy = resolveVideoPlayerUiLayoutPolicy(widthDp = 393)

        assertEquals(120, policy.gestureOverlaySizeDp)
        assertEquals(48, policy.gestureIconSizeDp)
        assertEquals(100, policy.seekFeedbackSizeDp)
        assertEquals(24, policy.gestureBoundaryPaddingDp)
    }

    @Test
    fun expandedTablet_enlargesControlTargetsAndSpacing() {
        val policy = resolveVideoPlayerUiLayoutPolicy(widthDp = 1280)

        assertEquals(132, policy.gestureOverlaySizeDp)
        assertEquals(52, policy.gestureIconSizeDp)
        assertEquals(108, policy.seekFeedbackSizeDp)
        assertEquals(112, policy.restoreButtonBottomOffsetDp)
    }

    @Test
    fun ultraWideTablet_furtherImprovesReadability() {
        val policy = resolveVideoPlayerUiLayoutPolicy(widthDp = 1920)

        assertEquals(140, policy.gestureOverlaySizeDp)
        assertEquals(56, policy.gestureIconSizeDp)
        assertTrue(policy.restoreButtonHorizontalPaddingDp >= 20)
        assertTrue(policy.longPressBadgeHorizontalPaddingDp >= 24)
    }

    @Test
    fun ultraWide_prefersStableLargeTouchTargets() {
        val policy = resolveVideoPlayerUiLayoutPolicy(widthDp = 1920)

        assertEquals(140, policy.gestureOverlaySizeDp)
        assertEquals(56, policy.gestureIconSizeDp)
        assertEquals(112, policy.seekFeedbackSizeDp)
        assertEquals(28, policy.gestureBoundaryPaddingDp)
    }

    @Test
    fun collapsedInlineViewport_scalesGestureAndRestoreTargetsDown() {
        val policy = resolveVideoPlayerUiLayoutPolicy(widthDp = 168)

        assertEquals(96, policy.gestureOverlaySizeDp)
        assertEquals(36, policy.gestureIconSizeDp)
        assertEquals(84, policy.seekFeedbackSizeDp)
        assertEquals(16, policy.gestureBoundaryPaddingDp)
        assertEquals(72, policy.restoreButtonBottomOffsetDp)
    }
}
