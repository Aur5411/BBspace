package com.android.purebilibili.feature.video.policy

import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.UgcSection
import com.android.purebilibili.data.model.response.ViewInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResumePlaybackSuggestionPolicyTest {

    @Test
    fun `suggests multi page resume target when default page has no progress`() {
        val info = ViewInfo(
            bvid = "BV1multi",
            cid = 101L,
            pages = listOf(
                Page(cid = 101L, page = 1, part = "P1"),
                Page(cid = 205L, page = 5, part = "P5")
            )
        )

        val suggestion = resolveResumePlaybackSuggestion(
            requestCid = 101L,
            loadedInfo = info,
            progressLookup = { bvid, cid ->
                when ("$bvid#$cid") {
                    "BV1multi#205" -> 15 * 60 * 1000L
                    else -> 0L
                }
            }
        )

        assertEquals("BV1multi", suggestion?.targetBvid)
        assertEquals(205L, suggestion?.targetCid)
        assertEquals("P5", suggestion?.targetLabel)
    }

    @Test
    fun `suggests cross episode resume target for ugc season`() {
        val info = ViewInfo(
            bvid = "BV1ep1",
            cid = 1001L,
            pages = listOf(Page(cid = 1001L, page = 1)),
            ugc_season = UgcSeason(
                id = 77L,
                sections = listOf(
                    UgcSection(
                        id = 1L,
                        episodes = listOf(
                            UgcEpisode(bvid = "BV1ep1", cid = 1001L, title = "第1集"),
                            UgcEpisode(bvid = "BV1ep5", cid = 5005L, title = "第5集")
                        )
                    )
                )
            )
        )

        val suggestion = resolveResumePlaybackSuggestion(
            requestCid = 1001L,
            loadedInfo = info,
            progressLookup = { bvid, cid ->
                when ("$bvid#$cid") {
                    "BV1ep5#5005" -> 12 * 60 * 1000L
                    else -> 0L
                }
            }
        )

        assertEquals("BV1ep5", suggestion?.targetBvid)
        assertEquals(5005L, suggestion?.targetCid)
        assertEquals("第2集 第5集", suggestion?.targetLabel)
    }

    @Test
    fun `does not suggest when user explicitly requested non entry cid`() {
        val info = ViewInfo(
            bvid = "BV1explicit",
            cid = 1002L,
            pages = listOf(
                Page(cid = 1001L, page = 1),
                Page(cid = 1002L, page = 2),
                Page(cid = 1003L, page = 3)
            )
        )

        val suggestion = resolveResumePlaybackSuggestion(
            requestCid = 1002L,
            loadedInfo = info,
            progressLookup = { _, _ -> 20 * 60 * 1000L }
        )

        assertNull(suggestion)
    }

    @Test
    fun `does not suggest when current progress is almost same as best candidate`() {
        val info = ViewInfo(
            bvid = "BV1delta",
            cid = 3001L,
            pages = listOf(
                Page(cid = 3001L, page = 1),
                Page(cid = 3002L, page = 2)
            )
        )

        val suggestion = resolveResumePlaybackSuggestion(
            requestCid = 3001L,
            loadedInfo = info,
            minDeltaFromCurrentMs = 5_000L,
            progressLookup = { _, cid ->
                when (cid) {
                    3001L -> 10 * 60 * 1000L
                    3002L -> 10 * 60 * 1000L + 3_000L
                    else -> 0L
                }
            }
        )

        assertNull(suggestion)
    }

    @Test
    fun `suggests later page even when entry page has longer progress`() {
        val info = ViewInfo(
            bvid = "BV1later",
            cid = 1101L,
            pages = listOf(
                Page(cid = 1101L, page = 1),
                Page(cid = 1102L, page = 2),
                Page(cid = 1103L, page = 3)
            )
        )

        val suggestion = resolveResumePlaybackSuggestion(
            requestCid = 1101L,
            loadedInfo = info,
            progressLookup = { _, cid ->
                when (cid) {
                    1101L -> 9 * 60 * 1000L
                    1103L -> 25_000L
                    else -> 0L
                }
            }
        )

        assertEquals(1103L, suggestion?.targetCid)
        assertEquals("P3", suggestion?.targetLabel)
    }
}
