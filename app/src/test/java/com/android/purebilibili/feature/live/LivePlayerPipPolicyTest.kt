package com.android.purebilibili.feature.live

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LivePlayerPipPolicyTest {

    @Test
    fun `live pip button should require android o or above`() {
        assertFalse(shouldShowLivePipButton(25))
        assertTrue(shouldShowLivePipButton(26))
        assertTrue(shouldShowLivePipButton(34))
    }

    @Test
    fun `live playback should not pause when pip is active or pending`() {
        assertFalse(
            shouldPauseLivePlaybackOnPause(
                isInPictureInPictureMode = true,
                isPipRequested = false,
                shouldKeepPlayingInBackground = false
            )
        )
        assertFalse(
            shouldPauseLivePlaybackOnPause(
                isInPictureInPictureMode = false,
                isPipRequested = true,
                shouldKeepPlayingInBackground = false
            )
        )
        assertTrue(
            shouldPauseLivePlaybackOnPause(
                isInPictureInPictureMode = false,
                isPipRequested = false,
                shouldKeepPlayingInBackground = false
            )
        )
    }

    @Test
    fun `live playback should keep playing when background audio policy allows`() {
        assertFalse(
            shouldPauseLivePlaybackOnPause(
                isInPictureInPictureMode = false,
                isPipRequested = false,
                shouldKeepPlayingInBackground = true
            )
        )
    }
}
