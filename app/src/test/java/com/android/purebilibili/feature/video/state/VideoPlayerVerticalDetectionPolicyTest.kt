package com.android.purebilibili.feature.video.state

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlayerVerticalDetectionPolicyTest {

    @Test
    fun apiDimensionRotate90_isVertical() {
        assertTrue(resolveApiDimensionIsVertical(width = 1920, height = 1080, rotate = 90))
    }

    @Test
    fun apiDimensionRotate270_isVertical() {
        assertTrue(resolveApiDimensionIsVertical(width = 1920, height = 1080, rotate = 270))
    }

    @Test
    fun apiDimensionRotate0_landscapeIsNotVertical() {
        assertFalse(resolveApiDimensionIsVertical(width = 1920, height = 1080, rotate = 0))
    }
}
