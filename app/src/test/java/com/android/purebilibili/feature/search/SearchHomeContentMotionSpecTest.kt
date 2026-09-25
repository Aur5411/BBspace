package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertTrue

class SearchHomeContentMotionSpecTest {

    @Test
    fun fullMotion_usesSlowerSilkyTransition() {
        val spec = resolveSearchHomeContentMotionSpec(reducedMotion = false)

        assertTrue(spec.fadeInDurationMillis >= 260)
        assertTrue(spec.fadeOutDurationMillis >= 180)
        assertTrue(spec.sizeTransformDurationMillis >= 320)
        assertTrue(spec.enterFromTop)
        assertTrue(spec.exitTowardTop)
        assertTrue(spec.enterOffsetDp > 0)
        assertTrue(spec.exitOffsetDp > 0)
    }

    @Test
    fun reducedMotion_collapsesToNearInstantMotion() {
        val spec = resolveSearchHomeContentMotionSpec(reducedMotion = true)

        assertTrue(spec.fadeInDurationMillis <= 120)
        assertTrue(spec.fadeOutDurationMillis <= 120)
        assertTrue(spec.sizeTransformDurationMillis <= 140)
        assertTrue(spec.enterFromTop)
        assertTrue(spec.exitTowardTop)
    }
}
