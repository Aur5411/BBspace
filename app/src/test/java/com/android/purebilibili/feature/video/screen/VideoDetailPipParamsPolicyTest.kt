package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailPipParamsPolicyTest {

    @Test
    fun `never updates pip params when system pip mode is disabled`() {
        assertFalse(
            shouldApplyPipParamsUpdate(
                pipModeEnabled = false,
                modeChanged = true,
                boundsChanged = true,
                elapsedSinceLastUpdateMs = 10_000L
            )
        )
    }

    @Test
    fun `updates immediately when pip mode is enabled and mode changed`() {
        assertTrue(
            shouldApplyPipParamsUpdate(
                pipModeEnabled = true,
                modeChanged = true,
                boundsChanged = false,
                elapsedSinceLastUpdateMs = 0L
            )
        )
    }

    @Test
    fun `throttles frequent bounds updates in pip mode`() {
        assertFalse(
            shouldApplyPipParamsUpdate(
                pipModeEnabled = true,
                modeChanged = false,
                boundsChanged = true,
                elapsedSinceLastUpdateMs = 120L,
                minUpdateIntervalMs = 400L
            )
        )
        assertTrue(
            shouldApplyPipParamsUpdate(
                pipModeEnabled = true,
                modeChanged = false,
                boundsChanged = true,
                elapsedSinceLastUpdateMs = 550L,
                minUpdateIntervalMs = 400L
            )
        )
    }
}
