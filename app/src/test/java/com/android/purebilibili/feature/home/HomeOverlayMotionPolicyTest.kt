package com.android.purebilibili.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeOverlayMotionPolicyTest {

    @Test
    fun `overlay motion spec exposes stable tuned durations`() {
        val spec = resolveHomeOverlayMotionSpec()

        assertEquals(180, spec.refreshTipEnterFadeDurationMillis)
        assertEquals(220, spec.refreshTipExitFadeDurationMillis)
        assertEquals(220, spec.refreshTipSlideDurationMillis)
        assertEquals(200, spec.undoFabFadeDurationMillis)
        assertEquals(250, spec.undoFabSlideDurationMillis)
        assertEquals(200, spec.previewOverlayFadeDurationMillis)
        assertEquals(200, spec.previewOverlayScaleDurationMillis)
        assertEquals(300, spec.sideNavEnterSlideDurationMillis)
        assertEquals(250, spec.sideNavExitSlideDurationMillis)
        assertEquals(200, spec.sideNavFadeDurationMillis)
    }
}
