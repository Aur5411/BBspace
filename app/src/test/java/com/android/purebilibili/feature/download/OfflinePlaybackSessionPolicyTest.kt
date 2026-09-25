package com.android.purebilibili.feature.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflinePlaybackSessionPolicyTest {

    @Test
    fun registersOfflinePlaybackSessionOnlyForPlayableDownloadedFiles() {
        assertTrue(
            shouldRegisterOfflinePlaybackSession(
                fileExists = true,
                filePath = "/storage/emulated/0/Android/data/offline.m4a"
            )
        )
        assertFalse(
            shouldRegisterOfflinePlaybackSession(
                fileExists = false,
                filePath = "/storage/emulated/0/Android/data/offline.m4a"
            )
        )
        assertFalse(
            shouldRegisterOfflinePlaybackSession(
                fileExists = true,
                filePath = null
            )
        )
    }

    @Test
    fun resolvesOfflinePlaybackSessionMetadataFromDownloadTask() {
        val task = DownloadTask(
            bvid = "BV1offline",
            cid = 100L,
            title = "缓存课程",
            cover = "https://example.com/cover.jpg",
            ownerName = "讲师",
            ownerFace = "",
            duration = 120,
            quality = 80,
            qualityDesc = "1080P",
            videoUrl = "",
            audioUrl = "",
            filePath = "/tmp/offline.mp4",
            localCoverPath = "/tmp/offline-cover.jpg",
            isAudioOnly = true
        )

        val metadata = resolveOfflinePlaybackSessionMetadata(task)

        assertEquals("缓存课程", metadata.title)
        assertEquals("讲师", metadata.artist)
        assertEquals("https://example.com/cover.jpg", metadata.coverUrl)
    }

    @Test
    fun resolvesMiniPlayerPayloadForOfflinePlaybackSession() {
        val task = DownloadTask(
            bvid = "BV1offline",
            cid = 100L,
            title = "缓存课程",
            cover = "https://example.com/cover.jpg",
            ownerName = "",
            ownerFace = "",
            duration = 120,
            quality = 80,
            qualityDesc = "1080P",
            videoUrl = "",
            audioUrl = "",
            filePath = "/tmp/offline.mp4",
            isAudioOnly = true
        )

        val payload = resolveOfflineMiniPlayerPayload(task)

        assertEquals("BV1offline", payload.bvid)
        assertEquals(100L, payload.cid)
        assertEquals("缓存课程", payload.title)
        assertEquals("离线音频", payload.owner)
        assertEquals("https://example.com/cover.jpg", payload.coverUrl)
    }
}
