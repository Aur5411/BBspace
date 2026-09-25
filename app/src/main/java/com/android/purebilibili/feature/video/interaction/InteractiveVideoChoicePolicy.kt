package com.android.purebilibili.feature.video.interaction

data class InteractiveChoiceUiModel(
    val edgeId: Long,
    val cid: Long,
    val text: String,
    val isDefault: Boolean = false,
    val nativeAction: String = "",
    val x: Int? = null,
    val y: Int? = null,
    val textAlign: Int = 0
)

data class InteractiveChoicePanelUiState(
    val visible: Boolean = false,
    val title: String = "剧情分支",
    val edgeId: Long = 0L,
    val questionId: Long = 0L,
    val questionType: Int = 1,
    val choices: List<InteractiveChoiceUiModel> = emptyList(),
    val remainingMs: Long? = null,
    val pauseVideo: Boolean = false,
    val sourceVideoWidth: Int = 0,
    val sourceVideoHeight: Int = 0
)

internal fun shouldTriggerInteractiveQuestion(currentPositionMs: Long, triggerTimeMs: Long): Boolean {
    return currentPositionMs >= triggerTimeMs
}

internal fun resolveInteractiveQuestionPollingIntervalMs(
    currentPositionMs: Long,
    triggerTimeMs: Long,
    isPlaying: Boolean
): Long {
    if (!isPlaying) return 1_000L
    val remainingMs = (triggerTimeMs - currentPositionMs).coerceAtLeast(0L)
    return when {
        remainingMs <= 1_000L -> 100L
        remainingMs <= 5_000L -> 250L
        remainingMs <= 15_000L -> 500L
        else -> 1_000L
    }
}

internal fun resolveInteractiveAutoChoice(
    choices: List<InteractiveChoiceUiModel>
): InteractiveChoiceUiModel? {
    if (choices.isEmpty()) return null
    return choices.firstOrNull { it.isDefault } ?: choices.first()
}

internal fun normalizeInteractiveCountdownMs(rawDurationMs: Int): Long? {
    return rawDurationMs.takeIf { it > 0 }?.toLong()
}

internal fun resolveInteractiveCountdownUpdateIntervalMs(remainingMs: Long): Long {
    val safeRemainingMs = remainingMs.coerceAtLeast(0L)
    return when {
        safeRemainingMs <= 1_000L -> 100L
        safeRemainingMs <= 3_000L -> 150L
        safeRemainingMs <= 10_000L -> 250L
        else -> 500L
    }
}

internal fun resolveInteractiveQuestionTriggerMs(edgeStartPositionMs: Long, triggerOffsetMs: Long): Long {
    return (edgeStartPositionMs + triggerOffsetMs).coerceAtLeast(0L)
}

internal fun resolveInteractiveChoiceCid(
    choiceCid: Long,
    platformAction: String,
    currentCid: Long
): Long? {
    val action = parseInteractiveJumpAction(platformAction)
    val cidFromAction = action?.cid ?: 0L
    return when {
        choiceCid > 0L -> choiceCid
        cidFromAction > 0L -> cidFromAction
        currentCid > 0L -> currentCid
        else -> null
    }
}

internal fun resolveInteractiveChoiceEdgeId(choiceEdgeId: Long, platformAction: String): Long? {
    if (choiceEdgeId > 0L) return choiceEdgeId
    return parseInteractiveJumpAction(platformAction)?.edgeId?.takeIf { it > 0L }
}

private data class InteractiveJumpAction(
    val edgeId: Long,
    val cid: Long
)

private fun parseInteractiveJumpAction(platformAction: String): InteractiveJumpAction? {
    if (platformAction.isBlank()) return null
    val tokens = platformAction.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (tokens.size < 3) return null
    if (!tokens.first().equals("JUMP", ignoreCase = true)) return null
    val edgeId = tokens.getOrNull(1)?.toLongOrNull() ?: return null
    val cid = tokens.getOrNull(2)?.toLongOrNull() ?: return null
    return InteractiveJumpAction(edgeId = edgeId, cid = cid)
}
