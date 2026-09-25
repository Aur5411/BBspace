package com.android.purebilibili.feature.video.ui.pager

import com.android.purebilibili.feature.video.viewmodel.PlaybackEndAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PortraitPlaybackAdvancePolicyTest {

    @Test
    fun stopAndRepeatCurrent_doNotRequestPageAdvance() {
        assertNull(
            resolveNextPortraitPageAfterPlaybackEnd(
                action = PlaybackEndAction.STOP,
                currentPage = 1,
                lastPage = 4
            )
        )
        assertNull(
            resolveNextPortraitPageAfterPlaybackEnd(
                action = PlaybackEndAction.REPEAT_CURRENT,
                currentPage = 1,
                lastPage = 4
            )
        )
    }

    @Test
    fun autoContinueAndPlayNext_advanceToNextPageWhenAvailable() {
        assertEquals(
            3,
            resolveNextPortraitPageAfterPlaybackEnd(
                action = PlaybackEndAction.AUTO_CONTINUE,
                currentPage = 2,
                lastPage = 4
            )
        )
        assertEquals(
            3,
            resolveNextPortraitPageAfterPlaybackEnd(
                action = PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST,
                currentPage = 2,
                lastPage = 4
            )
        )
    }

    @Test
    fun autoContinueAndPlayNext_stopAtEndOfList() {
        assertNull(
            resolveNextPortraitPageAfterPlaybackEnd(
                action = PlaybackEndAction.AUTO_CONTINUE,
                currentPage = 4,
                lastPage = 4
            )
        )
        assertNull(
            resolveNextPortraitPageAfterPlaybackEnd(
                action = PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST,
                currentPage = 4,
                lastPage = 4
            )
        )
    }

    @Test
    fun loopPlaylist_wrapsToFirstPageAtEnd() {
        assertEquals(
            0,
            resolveNextPortraitPageAfterPlaybackEnd(
                action = PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST_LOOP,
                currentPage = 4,
                lastPage = 4
            )
        )
    }
}
