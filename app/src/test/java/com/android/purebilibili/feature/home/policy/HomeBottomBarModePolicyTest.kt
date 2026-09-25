package com.android.purebilibili.feature.home.policy

import com.android.purebilibili.core.store.SettingsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeBottomBarModePolicyTest {

    @Test
    fun sideNavigation_forcesBottomBarHidden() {
        val visible = resolveHomeBottomBarBaseVisibility(
            useSideNavigation = true,
            mode = SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE
        )

        assertEquals(false, visible)
    }

    @Test
    fun alwaysVisibleMode_showsBottomBar() {
        val visible = resolveHomeBottomBarBaseVisibility(
            useSideNavigation = false,
            mode = SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE
        )

        assertEquals(true, visible)
    }

    @Test
    fun alwaysHiddenMode_hidesBottomBar() {
        val visible = resolveHomeBottomBarBaseVisibility(
            useSideNavigation = false,
            mode = SettingsManager.BottomBarVisibilityMode.ALWAYS_HIDDEN
        )

        assertEquals(false, visible)
    }

    @Test
    fun scrollHideMode_defersToScrollSignal() {
        val visible = resolveHomeBottomBarBaseVisibility(
            useSideNavigation = false,
            mode = SettingsManager.BottomBarVisibilityMode.SCROLL_HIDE
        )

        assertNull(visible)
    }
}
