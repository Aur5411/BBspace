package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class WatchLaterQueueCoverUrlPolicyTest {

    @Test
    fun normalizeProtocolRelativeUrlToHttps() {
        assertEquals(
            "https://i0.hdslb.com/bfs/archive/demo.jpg",
            normalizePlaylistCoverUrlForUi("//i0.hdslb.com/bfs/archive/demo.jpg")
        )
    }

    @Test
    fun normalizeHttpUrlToHttps() {
        assertEquals(
            "https://i0.hdslb.com/bfs/archive/demo.jpg",
            normalizePlaylistCoverUrlForUi("http://i0.hdslb.com/bfs/archive/demo.jpg")
        )
    }

    @Test
    fun keepHttpsUrlUnchanged() {
        assertEquals(
            "https://i0.hdslb.com/bfs/archive/demo.jpg",
            normalizePlaylistCoverUrlForUi("https://i0.hdslb.com/bfs/archive/demo.jpg")
        )
    }

    @Test
    fun returnEmptyForBlankInput() {
        assertEquals("", normalizePlaylistCoverUrlForUi("  "))
    }
}
