package com.android.purebilibili.feature.video.usecase

import androidx.media3.common.Player
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlaybackPreparationPolicyTest {

    @Test
    fun `shouldPreparePlayerOnLoad still prepares when autoplay is disabled`() {
        assertTrue(shouldPreparePlayerOnLoad(playWhenReady = false))
    }

    @Test
    fun `shouldPreparePlayerOnLoad returns true when autoplay is enabled`() {
        assertTrue(shouldPreparePlayerOnLoad(playWhenReady = true))
    }

    @Test
    fun `shouldPreparePlayerBeforeExplicitPlay returns true for idle queued player`() {
        assertTrue(
            shouldPreparePlayerBeforeExplicitPlay(
                playbackState = Player.STATE_IDLE,
                hasMediaItems = true
            )
        )
    }

    @Test
    fun `shouldPreparePlayerBeforeExplicitPlay returns false once player is already prepared`() {
        assertFalse(
            shouldPreparePlayerBeforeExplicitPlay(
                playbackState = Player.STATE_READY,
                hasMediaItems = true
            )
        )
    }
}
