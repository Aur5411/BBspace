package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LandscapeDanmakuInputPolicyTest {

    @Test
    fun placeholderPolicy_forcesSingleLineAndReservesSpaceForSettingsIcon() {
        val policy = resolveLandscapeDanmakuPlaceholderPolicy(
            settingButtonSizeDp = 34,
            settingEndPaddingDp = 4,
            extraBufferDp = 8
        )

        assertEquals(1, policy.maxLines)
        assertTrue(policy.ellipsis)
        assertEquals(46, policy.trailingTextPaddingDp)
    }
}
