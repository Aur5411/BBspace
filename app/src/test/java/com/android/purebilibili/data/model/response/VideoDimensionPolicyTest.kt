package com.android.purebilibili.data.model.response

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDimensionPolicyTest {

    @Test
    fun isVertical_true_whenHeightGreaterThanWidthWithoutRotation() {
        val dimension = Dimension(width = 720, height = 1280, rotate = 0)
        assertTrue(dimension.isVertical)
    }

    @Test
    fun isVertical_true_whenRotate90SwapsLandscapeSource() {
        val dimension = Dimension(width = 1920, height = 1080, rotate = 90)
        assertTrue(dimension.isVertical)
    }

    @Test
    fun isVertical_true_whenRotate270SwapsLandscapeSource() {
        val dimension = Dimension(width = 1920, height = 1080, rotate = 270)
        assertTrue(dimension.isVertical)
    }

    @Test
    fun isVertical_false_whenRotate180KeepsLandscape() {
        val dimension = Dimension(width = 1920, height = 1080, rotate = 180)
        assertFalse(dimension.isVertical)
    }
}
