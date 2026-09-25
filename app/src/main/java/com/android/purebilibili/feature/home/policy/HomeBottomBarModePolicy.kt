package com.android.purebilibili.feature.home.policy

import com.android.purebilibili.core.store.SettingsManager

internal fun resolveHomeBottomBarBaseVisibility(
    useSideNavigation: Boolean,
    mode: SettingsManager.BottomBarVisibilityMode
): Boolean? {
    if (useSideNavigation) return false
    return when (mode) {
        SettingsManager.BottomBarVisibilityMode.SCROLL_HIDE -> null
        SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE -> true
        SettingsManager.BottomBarVisibilityMode.ALWAYS_HIDDEN -> false
    }
}
