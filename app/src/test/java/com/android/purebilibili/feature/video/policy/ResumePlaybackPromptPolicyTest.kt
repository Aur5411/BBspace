package com.android.purebilibili.feature.video.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResumePlaybackPromptPolicyTest {

    @Test
    fun `prompt key should combine target bvid and cid`() {
        val key = resolveResumePlaybackPromptKey(
            ResumePlaybackSuggestion(
                targetBvid = "BV1abc",
                targetCid = 12345L,
                targetLabel = "P5",
                positionMs = 15 * 60 * 1000L
            )
        )

        assertEquals("BV1abc#12345", key)
    }

    @Test
    fun `does not show prompt when feature disabled`() {
        val show = shouldShowResumePlaybackPrompt(
            suggestion = ResumePlaybackSuggestion(
                targetBvid = "BV1abc",
                targetCid = 12345L,
                targetLabel = "P5",
                positionMs = 15 * 60 * 1000L
            ),
            promptEnabled = false,
            hasPromptedBefore = { false }
        )

        assertFalse(show)
    }

    @Test
    fun `does not show prompt when already prompted`() {
        val show = shouldShowResumePlaybackPrompt(
            suggestion = ResumePlaybackSuggestion(
                targetBvid = "BV1abc",
                targetCid = 12345L,
                targetLabel = "P5",
                positionMs = 15 * 60 * 1000L
            ),
            promptEnabled = true,
            hasPromptedBefore = { key -> key == "BV1abc#12345" }
        )

        assertFalse(show)
    }

    @Test
    fun `shows prompt when enabled and first time`() {
        val show = shouldShowResumePlaybackPrompt(
            suggestion = ResumePlaybackSuggestion(
                targetBvid = "BV1abc",
                targetCid = 12345L,
                targetLabel = "P5",
                positionMs = 15 * 60 * 1000L
            ),
            promptEnabled = true,
            hasPromptedBefore = { false }
        )

        assertTrue(show)
    }

    @Test
    fun `does not show prompt when suggestion is null`() {
        val show = shouldShowResumePlaybackPrompt(
            suggestion = null,
            promptEnabled = true,
            hasPromptedBefore = { false }
        )

        assertFalse(show)
    }
}
