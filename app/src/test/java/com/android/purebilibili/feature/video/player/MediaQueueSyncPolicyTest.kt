package com.android.purebilibili.feature.video.player

import androidx.media3.common.Player
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaQueueSyncPolicyTest {

    private val playlist = listOf(
        PlaylistItem(
            bvid = "BV1first",
            title = "first",
            cover = "cover-1",
            owner = "owner-1"
        ),
        PlaylistItem(
            bvid = "BV1second",
            title = "second",
            cover = "cover-2",
            owner = "owner-2"
        ),
        PlaylistItem(
            bvid = "BV1third",
            title = "third",
            cover = "cover-3",
            owner = "owner-3"
        )
    )

    @Test
    fun queueNavigationShouldOnlyBeEnabledForMultiItemPlaylists() {
        assertFalse(shouldEnableQueueNavigation(playlistSize = 0))
        assertFalse(shouldEnableQueueNavigation(playlistSize = 1))
        assertTrue(shouldEnableQueueNavigation(playlistSize = 2))
    }

    @Test
    fun queueRebuildShouldPreserveCurrentItemByBvidWhenStillPresent() {
        assertEquals(
            1,
            resolveQueueCurrentIndexForPlaylistRebuild(
                playlist = playlist,
                currentBvid = "BV1second",
                fallbackIndex = 0
            )
        )
    }

    @Test
    fun queueRebuildShouldFallbackToClampedIndexWhenCurrentItemMissing() {
        assertEquals(
            2,
            resolveQueueCurrentIndexForPlaylistRebuild(
                playlist = playlist,
                currentBvid = "BV1missing",
                fallbackIndex = 8
            )
        )
        assertEquals(
            0,
            resolveQueueCurrentIndexForPlaylistRebuild(
                playlist = playlist,
                currentBvid = null,
                fallbackIndex = -3
            )
        )
    }

    @Test
    fun queueRebuildShouldReturnNegativeIndexForEmptyPlaylist() {
        assertEquals(
            -1,
            resolveQueueCurrentIndexForPlaylistRebuild(
                playlist = emptyList(),
                currentBvid = "BV1any",
                fallbackIndex = 0
            )
        )
    }

    @Test
    fun playerCurrentIndexShouldDrivePlaylistSyncWhenValidAndChanged() {
        assertEquals(
            2,
            resolvePlaylistIndexSyncFromQueue(
                playerCurrentIndex = 2,
                playlistSize = playlist.size,
                currentPlaylistIndex = 0
            )
        )
    }

    @Test
    fun playlistSyncShouldIgnoreInvalidOrUnchangedPlayerIndexes() {
        assertNull(
            resolvePlaylistIndexSyncFromQueue(
                playerCurrentIndex = -1,
                playlistSize = playlist.size,
                currentPlaylistIndex = 0
            )
        )
        assertNull(
            resolvePlaylistIndexSyncFromQueue(
                playerCurrentIndex = 3,
                playlistSize = playlist.size,
                currentPlaylistIndex = 0
            )
        )
        assertNull(
            resolvePlaylistIndexSyncFromQueue(
                playerCurrentIndex = 1,
                playlistSize = playlist.size,
                currentPlaylistIndex = 1
            )
        )
    }

    @Test
    fun queueMetadataItemsShouldUsePlaylistIdentityAndDisplayMetadata() {
        val items = buildQueueMetadataItems(playlist)

        assertEquals(playlist.size, items.size)
        assertEquals("BV1first", items[0].mediaId)
        assertEquals("first", items[0].mediaMetadata.title)
        assertEquals("owner-1", items[0].mediaMetadata.artist)
        assertEquals("BV1third", items[2].mediaId)
        assertEquals("third", items[2].mediaMetadata.displayTitle)
    }

    @Test
    fun queueNavigationCommandIdsShouldExposeOnlyCommandsThatAreAvailable() {
        val enabledCommands = resolveQueueNavigationCommandIds(
            hasNext = true,
            hasPrevious = true
        )
        assertTrue(enabledCommands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
        assertTrue(enabledCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
        assertTrue(enabledCommands.contains(Player.COMMAND_SEEK_TO_NEXT))
        assertTrue(enabledCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS))

        val nextOnlyCommands = resolveQueueNavigationCommandIds(
            hasNext = true,
            hasPrevious = false
        )
        assertTrue(nextOnlyCommands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
        assertTrue(nextOnlyCommands.contains(Player.COMMAND_SEEK_TO_NEXT))
        assertFalse(nextOnlyCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
        assertFalse(nextOnlyCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS))

        val previousOnlyCommands = resolveQueueNavigationCommandIds(
            hasNext = false,
            hasPrevious = true
        )
        assertFalse(previousOnlyCommands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
        assertFalse(previousOnlyCommands.contains(Player.COMMAND_SEEK_TO_NEXT))
        assertTrue(previousOnlyCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
        assertTrue(previousOnlyCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS))

        val disabledCommands = resolveQueueNavigationCommandIds(
            hasNext = false,
            hasPrevious = false
        )
        assertFalse(disabledCommands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
        assertFalse(disabledCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
        assertFalse(disabledCommands.contains(Player.COMMAND_SEEK_TO_NEXT))
        assertFalse(disabledCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS))
    }

    @Test
    fun virtualSessionQueueShouldBeDisabledWhenTimelineWindowCountDoesNotMatchPlaylist() {
        assertFalse(
            shouldExposeVirtualQueueToSession(
                playlistSize = playlist.size,
                timelineWindowCount = 1,
                isLiveMode = false
            )
        )
        assertTrue(
            shouldExposeVirtualQueueToSession(
                playlistSize = playlist.size,
                timelineWindowCount = playlist.size,
                isLiveMode = false
            )
        )
        assertFalse(
            shouldExposeVirtualQueueToSession(
                playlistSize = playlist.size,
                timelineWindowCount = playlist.size,
                isLiveMode = true
            )
        )
    }
}
