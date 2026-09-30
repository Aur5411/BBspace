package com.android.purebilibili.feature.onboarding

import android.content.Context
import com.android.purebilibili.core.store.BottomBarSearchAutoExpandMode
import com.android.purebilibili.core.store.HomeTopLayoutOrder
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.applyOnboardingRecommendedUiStyle

enum class OnboardingSettingsProfile(
    val title: String,
    val subtitle: String
) {
    RECOMMENDED(
        title = "推荐默认",
        subtitle = "MD3、安卓液态玻璃、悬浮底栏、五个纯文字顶部标签"
    ),
    PERFORMANCE(
        title = "流畅优先",
        subtitle = "安卓液态玻璃与悬浮底栏，保留核心过渡"
    )
}

data class OnboardingSettingsGuidePreset(
    val profile: OnboardingSettingsProfile,
    val bottomBarFloating: Boolean,
    val bottomBarLiquidGlassEnabled: Boolean,
    val androidNativeLiquidGlassEnabled: Boolean,
    val bottomBarSearchEnabled: Boolean,
    val topTabLabelMode: Int,
    val topTabOrderIds: List<String>,
    val topTabVisibleIds: Set<String>,
    val homeTopLayoutOrder: HomeTopLayoutOrder,
    val cardTransitionEnabled: Boolean,
    val summaryLines: List<String>
)

private val DEFAULT_ONBOARDING_TOP_TAB_IDS = listOf(
    "RECOMMEND",
    "FOLLOW",
    "POPULAR",
    "LIVE",
    "GAME"
)

fun resolveOnboardingSettingsGuidePreset(
    profile: OnboardingSettingsProfile
): OnboardingSettingsGuidePreset {
    val sharedSummary = listOf(
        "默认使用 MD3 / Material 3",
        "开启安卓液态玻璃和悬浮底栏",
        "首页顶部标签纯文字显示 5 个"
    )
    return when (profile) {
        OnboardingSettingsProfile.RECOMMENDED -> OnboardingSettingsGuidePreset(
            profile = profile,
            bottomBarFloating = true,
            bottomBarLiquidGlassEnabled = true,
            androidNativeLiquidGlassEnabled = true,
            bottomBarSearchEnabled = false,
            topTabLabelMode = SettingsManager.TopTabLabelMode.TEXT_ONLY,
            topTabOrderIds = DEFAULT_ONBOARDING_TOP_TAB_IDS,
            topTabVisibleIds = DEFAULT_ONBOARDING_TOP_TAB_IDS.toSet(),
            homeTopLayoutOrder = HomeTopLayoutOrder.SEARCH_THEN_TABS,
            cardTransitionEnabled = true,
            summaryLines = sharedSummary
        )

        OnboardingSettingsProfile.PERFORMANCE -> OnboardingSettingsGuidePreset(
            profile = profile,
            bottomBarFloating = true,
            bottomBarLiquidGlassEnabled = true,
            androidNativeLiquidGlassEnabled = true,
            bottomBarSearchEnabled = false,
            topTabLabelMode = SettingsManager.TopTabLabelMode.TEXT_ONLY,
            topTabOrderIds = DEFAULT_ONBOARDING_TOP_TAB_IDS,
            topTabVisibleIds = DEFAULT_ONBOARDING_TOP_TAB_IDS.toSet(),
            homeTopLayoutOrder = HomeTopLayoutOrder.SEARCH_THEN_TABS,
            cardTransitionEnabled = true,
            summaryLines = sharedSummary + "保留核心视频过渡"
        )
    }
}

suspend fun applyOnboardingSettingsGuidePreset(
    context: Context,
    profile: OnboardingSettingsProfile
) {
    val preset = resolveOnboardingSettingsGuidePreset(profile)
    applyOnboardingRecommendedUiStyle(context)
    SettingsManager.setBottomBarFloating(context, preset.bottomBarFloating)
    SettingsManager.setBottomBarLiquidGlassEnabled(context, preset.bottomBarLiquidGlassEnabled)
    SettingsManager.setAndroidNativeLiquidGlassEnabled(
        context,
        preset.androidNativeLiquidGlassEnabled
    )
    SettingsManager.setBottomBarSearchEnabled(context, preset.bottomBarSearchEnabled)
    SettingsManager.setTopTabLabelMode(context, preset.topTabLabelMode)
    SettingsManager.setTopTabOrder(context, preset.topTabOrderIds)
    SettingsManager.setTopTabVisibleTabs(context, preset.topTabVisibleIds)
    SettingsManager.setHomeTopLayoutOrder(context, preset.homeTopLayoutOrder)
    SettingsManager.setCardTransitionEnabled(context, preset.cardTransitionEnabled)
}
