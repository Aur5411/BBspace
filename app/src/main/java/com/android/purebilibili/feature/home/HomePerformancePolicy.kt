package com.android.purebilibili.feature.home


internal data class HomePerformanceConfig(
    val headerBlurEnabled: Boolean,
    val bottomBarBlurEnabled: Boolean,
    val topBarLiquidGlassEnabled: Boolean,
    val homeSearchLiquidGlassEnabled: Boolean,
    val bottomBarLiquidGlassEnabled: Boolean,
    val cardAnimationEnabled: Boolean,
    val cardTransitionEnabled: Boolean,
    val preloadAheadCount: Int
) {
    val isAnyLiquidGlassEnabled: Boolean
        get() = topBarLiquidGlassEnabled ||
            homeSearchLiquidGlassEnabled ||
            bottomBarLiquidGlassEnabled
}

internal fun resolveHomePreloadAheadCount(
    normalPreloadAheadCount: Int
): Int {
    return normalPreloadAheadCount.coerceAtLeast(0).coerceAtMost(2)
}

internal fun resolveHomeCoverPreloadRange(
    isScrollInProgress: Boolean,
    lastVisibleIndex: Int,
    totalItemCount: Int,
    preloadAheadCount: Int
): IntRange? {
    if (isScrollInProgress || totalItemCount <= 0) return null
    val effectiveAheadCount = resolveHomePreloadAheadCount(
        normalPreloadAheadCount = preloadAheadCount
    )
    if (effectiveAheadCount <= 0) return null

    val preloadStart = (lastVisibleIndex + 1)
        .coerceAtLeast(0)
        .coerceAtMost(totalItemCount)
    val preloadEndExclusive = (preloadStart + effectiveAheadCount).coerceAtMost(totalItemCount)
    if (preloadStart >= preloadEndExclusive) return null
    return preloadStart until preloadEndExclusive
}

@Suppress("UNUSED_PARAMETER")
internal fun resolveHomePerformanceConfig(
    supportsIndependentLiquidGlass: Boolean = true,
    headerBlurEnabled: Boolean,
    bottomBarBlurEnabled: Boolean,
    topBarLiquidGlassEnabled: Boolean,
    homeSearchLiquidGlassEnabled: Boolean = false,
    bottomBarLiquidGlassEnabled: Boolean,
    androidNativeLiquidGlassEnabled: Boolean = true,
    cardAnimationEnabled: Boolean,
    cardTransitionEnabled: Boolean,
    smartVisualGuardEnabled: Boolean,
    normalPreloadAheadCount: Int = 5
): HomePerformanceConfig {
    // Feature retired: keep parameter for compatibility, but never apply runtime smoothness downgrade.
    val shouldPrioritizeSmoothness = false
    // The legacy per-surface values remain readable for settings migration/import compatibility,
    // but the Android liquid-glass switch is now the only runtime enablement source.
    val effectiveTopBarLiquidGlass = androidNativeLiquidGlassEnabled
    val effectiveHomeSearchLiquidGlass = androidNativeLiquidGlassEnabled
    val effectiveBottomBarLiquidGlass = androidNativeLiquidGlassEnabled
    val effectivePreloadAheadCount = when {
        shouldPrioritizeSmoothness -> normalPreloadAheadCount.coerceAtLeast(0).coerceAtMost(2)
        else -> resolveHomePreloadAheadCount(
            normalPreloadAheadCount = normalPreloadAheadCount
        )
    }

    return HomePerformanceConfig(
        headerBlurEnabled = headerBlurEnabled,
        bottomBarBlurEnabled = bottomBarBlurEnabled,
        topBarLiquidGlassEnabled = effectiveTopBarLiquidGlass,
        homeSearchLiquidGlassEnabled = effectiveHomeSearchLiquidGlass,
        bottomBarLiquidGlassEnabled = effectiveBottomBarLiquidGlass,
        cardAnimationEnabled = cardAnimationEnabled,
        cardTransitionEnabled = cardTransitionEnabled,
        preloadAheadCount = effectivePreloadAheadCount
    )
}
