package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailAudioModePolicyTest {

    @Test
    fun autoEnterAudioMode_onlyWhenRouteWantsAudio_andLoadSucceeded_andNotTriggeredBefore() {
        assertTrue(
            shouldAutoEnterAudioModeFromRoute(
                startAudioFromRoute = true,
                hasAutoEnteredAudioMode = false,
                isVideoLoadSuccess = true
            )
        )
    }

    @Test
    fun autoEnterAudioMode_disabledWhenAlreadyTriggered() {
        assertFalse(
            shouldAutoEnterAudioModeFromRoute(
                startAudioFromRoute = true,
                hasAutoEnteredAudioMode = true,
                isVideoLoadSuccess = true
            )
        )
    }

    @Test
    fun autoEnterAudioMode_disabledWhenNotRequestedOrNotReady() {
        assertFalse(
            shouldAutoEnterAudioModeFromRoute(
                startAudioFromRoute = false,
                hasAutoEnteredAudioMode = false,
                isVideoLoadSuccess = true
            )
        )
        assertFalse(
            shouldAutoEnterAudioModeFromRoute(
                startAudioFromRoute = true,
                hasAutoEnteredAudioMode = false,
                isVideoLoadSuccess = false
            )
        )
    }
}
