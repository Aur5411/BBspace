package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import com.android.purebilibili.feature.video.playback.resolver.AudioNextPlaybackStrategy
import com.android.purebilibili.feature.video.playback.resolver.resolveAudioNextPlaybackStrategy
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioNextPlaybackPolicyTest {

    @Test
    fun `external favorite playlist should use playlist strategy`() {
        assertEquals(
            AudioNextPlaybackStrategy.PLAY_EXTERNAL_PLAYLIST,
            resolveAudioNextPlaybackStrategy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.FAVORITE
            )
        )
    }

    @Test
    fun `external watch later playlist should use playlist strategy`() {
        assertEquals(
            AudioNextPlaybackStrategy.PLAY_EXTERNAL_PLAYLIST,
            resolveAudioNextPlaybackStrategy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.WATCH_LATER
            )
        )
    }

    @Test
    fun `non external playlist should keep legacy page and season strategy`() {
        assertEquals(
            AudioNextPlaybackStrategy.PAGE_THEN_SEASON_THEN_RELATED,
            resolveAudioNextPlaybackStrategy(
                isExternalPlaylist = false,
                externalPlaylistSource = ExternalPlaylistSource.NONE
            )
        )
    }
}
