package com.android.purebilibili.feature.video.ui.pager

import com.android.purebilibili.feature.video.viewmodel.PlaybackEndAction

internal fun resolveNextPortraitPageAfterPlaybackEnd(
    action: PlaybackEndAction,
    currentPage: Int,
    lastPage: Int
): Int? {
    if (lastPage < 0) return null
    return when (action) {
        PlaybackEndAction.STOP,
        PlaybackEndAction.REPEAT_CURRENT -> null
        PlaybackEndAction.AUTO_CONTINUE,
        PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST -> {
            val nextPage = (currentPage + 1).coerceAtMost(lastPage)
            nextPage.takeIf { it > currentPage }
        }
        PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST_LOOP -> {
            if (currentPage < lastPage) currentPage + 1 else 0
        }
    }
}
