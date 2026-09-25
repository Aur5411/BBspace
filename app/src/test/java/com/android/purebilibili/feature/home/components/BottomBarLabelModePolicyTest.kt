package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomBarLabelModePolicyTest {

    @Test
    fun `normalize should keep known bottom bar modes`() {
        assertTrue(shouldShowBottomBarIcon(normalizeBottomBarLabelMode(0)))
        assertTrue(shouldShowBottomBarText(normalizeBottomBarLabelMode(0)))

        assertTrue(shouldShowBottomBarIcon(normalizeBottomBarLabelMode(1)))
        assertFalse(shouldShowBottomBarText(normalizeBottomBarLabelMode(1)))

        assertFalse(shouldShowBottomBarIcon(normalizeBottomBarLabelMode(2)))
        assertTrue(shouldShowBottomBarText(normalizeBottomBarLabelMode(2)))
    }

    @Test
    fun `normalize should fallback to icon and text for unknown bottom bar mode`() {
        val mode = normalizeBottomBarLabelMode(99)
        assertTrue(shouldShowBottomBarIcon(mode))
        assertTrue(shouldShowBottomBarText(mode))
    }
}
