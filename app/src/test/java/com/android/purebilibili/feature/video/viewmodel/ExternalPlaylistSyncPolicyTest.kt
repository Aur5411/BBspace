package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.feature.video.player.PlaylistItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalPlaylistSyncPolicyTest {

    @Test
    fun `keeps external playlist when current bvid exists in queue`() {
        val playlist = listOf(
            PlaylistItem(bvid = "BV1", title = "v1", cover = "c1", owner = "u1"),
            PlaylistItem(bvid = "BV2", title = "v2", cover = "c2", owner = "u2")
        )

        val decision = resolveExternalPlaylistSyncDecision(
            isExternalPlaylist = true,
            playlist = playlist,
            currentBvid = "BV2"
        )

        assertTrue(decision.keepExternalPlaylist)
        assertEquals(1, decision.matchedIndex)
    }

    @Test
    fun `falls back to normal playlist when current bvid missing in external queue`() {
        val playlist = listOf(
            PlaylistItem(bvid = "BV10", title = "v10", cover = "c10", owner = "u10"),
            PlaylistItem(bvid = "BV11", title = "v11", cover = "c11", owner = "u11")
        )

        val decision = resolveExternalPlaylistSyncDecision(
            isExternalPlaylist = true,
            playlist = playlist,
            currentBvid = "BV99"
        )

        assertFalse(decision.keepExternalPlaylist)
        assertEquals(-1, decision.matchedIndex)
    }

    @Test
    fun `normal playlist mode never locks to external queue`() {
        val playlist = listOf(
            PlaylistItem(bvid = "BV1", title = "v1", cover = "c1", owner = "u1")
        )

        val decision = resolveExternalPlaylistSyncDecision(
            isExternalPlaylist = false,
            playlist = playlist,
            currentBvid = "BV1"
        )

        assertFalse(decision.keepExternalPlaylist)
        assertEquals(-1, decision.matchedIndex)
    }
}
