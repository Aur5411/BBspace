package com.android.purebilibili.feature.video.danmaku

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuMergedAdvancedFilterPolicyTest {

    private val defaultSettings = DanmakuTypeFilterSettings(
        allowScroll = true,
        allowTop = true,
        allowBottom = true,
        allowColorful = true,
        allowSpecial = true
    )

    @Test
    fun mergedAdvanced_hiddenWhenSpecialDisabled() {
        val settings = defaultSettings.copy(allowSpecial = false)

        assertFalse(
            shouldDisplayMergedAdvancedDanmaku(
                content = "测试弹幕",
                color = 0xFFD700,
                settings = settings,
                blockedMatchers = emptyList()
            )
        )
    }

    @Test
    fun mergedAdvanced_hiddenWhenColorfulDisabled() {
        val settings = defaultSettings.copy(allowColorful = false)

        assertFalse(
            shouldDisplayMergedAdvancedDanmaku(
                content = "测试弹幕",
                color = 0xFFD700,
                settings = settings,
                blockedMatchers = emptyList()
            )
        )
    }

    @Test
    fun mergedAdvanced_hiddenWhenKeywordBlocked() {
        val blockedMatchers = compileDanmakuBlockRules(listOf("剧透"))

        assertFalse(
            shouldDisplayMergedAdvancedDanmaku(
                content = "这段有剧透",
                color = 0xFFD700,
                settings = defaultSettings,
                blockedMatchers = blockedMatchers
            )
        )
    }

    @Test
    fun mergedAdvanced_visibleWhenAllowedAndNotBlocked() {
        assertTrue(
            shouldDisplayMergedAdvancedDanmaku(
                content = "正常弹幕",
                color = 0xFFD700,
                settings = defaultSettings,
                blockedMatchers = emptyList()
            )
        )
    }
}
