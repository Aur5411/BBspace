package com.android.purebilibili.feature.video.interaction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InteractiveVideoChoicePolicyTest {

    @Test
    fun `trigger should become true once playback reaches question time`() {
        assertTrue(!shouldTriggerInteractiveQuestion(currentPositionMs = 299, triggerTimeMs = 300))
        assertTrue(shouldTriggerInteractiveQuestion(currentPositionMs = 300, triggerTimeMs = 300))
        assertTrue(shouldTriggerInteractiveQuestion(currentPositionMs = 520, triggerTimeMs = 300))
    }

    @Test
    fun `trigger time should include edge start offset`() {
        assertEquals(82000L, resolveInteractiveQuestionTriggerMs(82000L, 0L))
        assertEquals(82300L, resolveInteractiveQuestionTriggerMs(82000L, 300L))
    }

    @Test
    fun `auto choice should prefer default option`() {
        val choice = resolveInteractiveAutoChoice(
            listOf(
                InteractiveChoiceUiModel(edgeId = 10, cid = 100, text = "A"),
                InteractiveChoiceUiModel(edgeId = 20, cid = 200, text = "B", isDefault = true)
            )
        )
        assertEquals(20L, choice?.edgeId)
    }

    @Test
    fun `auto choice should fallback to first option when no default`() {
        val choice = resolveInteractiveAutoChoice(
            listOf(
                InteractiveChoiceUiModel(edgeId = 10, cid = 100, text = "A"),
                InteractiveChoiceUiModel(edgeId = 20, cid = 200, text = "B")
            )
        )
        assertEquals(10L, choice?.edgeId)
    }

    @Test
    fun `auto choice should be null when list empty`() {
        assertNull(resolveInteractiveAutoChoice(emptyList()))
    }

    @Test
    fun `countdown should be null for non-positive duration`() {
        assertNull(normalizeInteractiveCountdownMs(0))
        assertNull(normalizeInteractiveCountdownMs(-1))
        assertEquals(1200L, normalizeInteractiveCountdownMs(1200))
    }

    @Test
    fun `choice cid should fallback to current cid when response cid is missing`() {
        assertEquals(
            300L,
            resolveInteractiveChoiceCid(choiceCid = 0L, platformAction = "", currentCid = 300L)
        )
        assertEquals(
            300L,
            resolveInteractiveChoiceCid(choiceCid = -1L, platformAction = "", currentCid = 300L)
        )
    }

    @Test
    fun `choice cid should prefer response cid when it is valid`() {
        assertEquals(
            500L,
            resolveInteractiveChoiceCid(choiceCid = 500L, platformAction = "JUMP 2 400", currentCid = 300L)
        )
    }

    @Test
    fun `choice cid should fallback to platform action when cid missing`() {
        assertEquals(
            700L,
            resolveInteractiveChoiceCid(choiceCid = 0L, platformAction = "JUMP 123 700", currentCid = 300L)
        )
    }

    @Test
    fun `choice cid should be null when both response and current cid are invalid`() {
        assertNull(resolveInteractiveChoiceCid(choiceCid = 0L, platformAction = "", currentCid = 0L))
    }

    @Test
    fun `choice edge id should fallback to platform action when missing`() {
        assertEquals(123L, resolveInteractiveChoiceEdgeId(0L, "JUMP 123 700"))
        assertEquals(999L, resolveInteractiveChoiceEdgeId(999L, "JUMP 123 700"))
        assertNull(resolveInteractiveChoiceEdgeId(0L, "not_jump"))
    }

    @Test
    fun `interactive question polling should back off aggressively when playback is paused`() {
        assertEquals(
            1_000L,
            resolveInteractiveQuestionPollingIntervalMs(
                currentPositionMs = 25_000L,
                triggerTimeMs = 40_000L,
                isPlaying = false
            )
        )
    }

    @Test
    fun `interactive question polling should speed up near trigger point`() {
        assertEquals(
            100L,
            resolveInteractiveQuestionPollingIntervalMs(
                currentPositionMs = 39_200L,
                triggerTimeMs = 40_000L,
                isPlaying = true
            )
        )
    }

    @Test
    fun `interactive question polling should stay moderate when trigger is still several seconds away`() {
        assertEquals(
            500L,
            resolveInteractiveQuestionPollingIntervalMs(
                currentPositionMs = 30_000L,
                triggerTimeMs = 40_000L,
                isPlaying = true
            )
        )
    }

    @Test
    fun `interactive countdown should slow updates when there is plenty of time left`() {
        assertEquals(500L, resolveInteractiveCountdownUpdateIntervalMs(remainingMs = 12_000L))
    }

    @Test
    fun `interactive countdown should become tighter near the final second`() {
        assertEquals(100L, resolveInteractiveCountdownUpdateIntervalMs(remainingMs = 900L))
    }
}
