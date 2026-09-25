package com.android.purebilibili.feature.video.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoDetailMotionSpecPolicyTest {

    @Test
    fun `motion spec clamps entry and content phases to minimum`() {
        val spec = resolveVideoDetailMotionSpec(transitionEnterDurationMillis = 80)

        assertEquals(120, spec.entryPhaseDurationMillis)
        assertEquals(180, spec.contentSwapFadeDurationMillis)
        assertEquals(180, spec.contentRevealFadeDurationMillis)
    }

    @Test
    fun `motion spec keeps longer transition duration`() {
        val spec = resolveVideoDetailMotionSpec(transitionEnterDurationMillis = 320)

        assertEquals(320, spec.entryPhaseDurationMillis)
        assertEquals(320, spec.contentSwapFadeDurationMillis)
        assertEquals(320, spec.contentRevealFadeDurationMillis)
    }
}
