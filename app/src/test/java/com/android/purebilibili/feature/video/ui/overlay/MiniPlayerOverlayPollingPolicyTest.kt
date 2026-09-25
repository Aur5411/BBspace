package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiniPlayerOverlayPollingPolicyTest {

    @Test
    fun pollingEnabled_onlyWhenMiniModeActiveWithPlayer() {
        assertTrue(
            shouldPollMiniPlayerProgress(
                playerExists = true,
                hostLifecycleStarted = true,
                isMiniMode = true,
                isActive = true
            )
        )
    }

    @Test
    fun pollingDisabled_whenAnyRequiredConditionMissing() {
        assertFalse(
            shouldPollMiniPlayerProgress(
                playerExists = false,
                hostLifecycleStarted = true,
                isMiniMode = true,
                isActive = true
            )
        )
        assertFalse(
            shouldPollMiniPlayerProgress(
                playerExists = true,
                hostLifecycleStarted = true,
                isMiniMode = false,
                isActive = true
            )
        )
        assertFalse(
            shouldPollMiniPlayerProgress(
                playerExists = true,
                hostLifecycleStarted = true,
                isMiniMode = true,
                isActive = false
            )
        )
    }

    @Test
    fun pausedPlayback_usesLongerPollingInterval() {
        assertEquals(500L, resolveMiniPlayerPollingIntervalMs(isPlaying = true))
        assertEquals(1000L, resolveMiniPlayerPollingIntervalMs(isPlaying = false))
    }

    @Test
    fun pollingDisabled_whenLiveMode() {
        // 📺 直播模式不需要进度轮询
        assertFalse(
            shouldPollMiniPlayerProgress(
                playerExists = true,
                hostLifecycleStarted = true,
                isMiniMode = true,
                isActive = true,
                isLiveMode = true
            )
        )
    }

    @Test
    fun pollingDisabled_whenHostLifecycleStopped() {
        assertFalse(
            shouldPollMiniPlayerProgress(
                playerExists = true,
                hostLifecycleStarted = false,
                isMiniMode = true,
                isActive = true
            )
        )
    }
}
