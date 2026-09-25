package com.android.purebilibili.feature.video.playback.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PluginPollingPolicyTest {

    @Test
    fun `active playback should use tighter plugin poll interval`() {
        assertEquals(
            750L,
            resolvePluginPollingIntervalMs(
                hasPlugins = true,
                isPlaying = true
            )
        )
    }

    @Test
    fun `paused playback should slow plugin poll interval`() {
        assertEquals(
            2_000L,
            resolvePluginPollingIntervalMs(
                hasPlugins = true,
                isPlaying = false
            )
        )
    }

    @Test
    fun `no plugin state should back off polling aggressively`() {
        assertEquals(
            5_000L,
            resolvePluginPollingIntervalMs(
                hasPlugins = false,
                isPlaying = true
            )
        )
    }

    @Test
    fun `position updates should dispatch immediately the first time`() {
        assertTrue(
            shouldDispatchPluginPositionUpdate(
                lastDispatchedPositionMs = null,
                currentPositionMs = 0L,
                minPositionDeltaMs = 400L
            )
        )
    }

    @Test
    fun `position updates should skip tiny movement`() {
        assertFalse(
            shouldDispatchPluginPositionUpdate(
                lastDispatchedPositionMs = 1_000L,
                currentPositionMs = 1_250L,
                minPositionDeltaMs = 400L
            )
        )
    }

    @Test
    fun `position updates should dispatch after meaningful progress`() {
        assertTrue(
            shouldDispatchPluginPositionUpdate(
                lastDispatchedPositionMs = 1_000L,
                currentPositionMs = 1_500L,
                minPositionDeltaMs = 400L
            )
        )
    }

    @Test
    fun `position updates should dispatch after user seeks backwards`() {
        assertTrue(
            shouldDispatchPluginPositionUpdate(
                lastDispatchedPositionMs = 12_000L,
                currentPositionMs = 7_000L,
                minPositionDeltaMs = 400L
            )
        )
    }
}
