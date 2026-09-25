package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceSettingsDisplayPolicyTest {

    @Test
    fun `dpi display should preserve non default override`() {
        assertEquals(95, resolveDisplayedAppDpiPercent(95))
        assertEquals(90, resolveDisplayedAppDpiPercent(90))
    }

    @Test
    fun `dpi display should fall back to 100 when override disabled`() {
        assertEquals(100, resolveDisplayedAppDpiPercent(0))
    }
}
