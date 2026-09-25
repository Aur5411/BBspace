package com.android.purebilibili.feature.watchlater

import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WatchLaterAudioPlayPolicyTest {

    @Test
    fun resolveWatchLaterPlayAllStartTarget_returnsFirstVideoWhenAvailable() {
        val target = resolveWatchLaterPlayAllStartTarget(
            listOf(
                VideoItem(bvid = "BV1", cid = 101L, title = "first"),
                VideoItem(bvid = "BV2", cid = 202L, title = "second")
            )
        )

        assertEquals("BV1", target?.first)
        assertEquals(101L, target?.second)
    }

    @Test
    fun resolveWatchLaterPlayAllStartTarget_returnsNullForEmptyList() {
        assertNull(resolveWatchLaterPlayAllStartTarget(emptyList()))
    }
}
