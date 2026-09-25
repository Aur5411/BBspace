package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.feature.video.playback.resolver.PlayInOrderNextSource
import com.android.purebilibili.feature.video.playback.resolver.resolvePlayInOrderNextSource
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayInOrderNextPolicyTest {

    @Test
    fun `sequential should prioritize page or season before playlist`() {
        assertEquals(
            PlayInOrderNextSource.PAGE_OR_SEASON,
            resolvePlayInOrderNextSource(
                hasNextPage = false,
                hasNextSeasonEpisode = true,
                hasNextPlaylistItem = true
            )
        )
    }

    @Test
    fun `sequential should fallback to playlist when collection has no next`() {
        assertEquals(
            PlayInOrderNextSource.PLAYLIST,
            resolvePlayInOrderNextSource(
                hasNextPage = false,
                hasNextSeasonEpisode = false,
                hasNextPlaylistItem = true
            )
        )
    }

    @Test
    fun `sequential should stop when neither collection nor playlist has next`() {
        assertEquals(
            PlayInOrderNextSource.NONE,
            resolvePlayInOrderNextSource(
                hasNextPage = false,
                hasNextSeasonEpisode = false,
                hasNextPlaylistItem = false
            )
        )
    }
}
