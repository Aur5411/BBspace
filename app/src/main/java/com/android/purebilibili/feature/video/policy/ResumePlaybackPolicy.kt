package com.android.purebilibili.feature.video.policy

import com.android.purebilibili.data.model.response.ViewInfo

data class ResumePlaybackSuggestion(
    val targetBvid: String,
    val targetCid: Long,
    val targetLabel: String,
    val positionMs: Long
)

private data class ResumeProgressSource(
    val bvid: String,
    val cid: Long,
    val label: String
)

private data class OrderedResumeCandidate(
    val order: Int,
    val suggestion: ResumePlaybackSuggestion
)

internal fun resolveResumePlaybackSuggestion(
    requestCid: Long,
    loadedInfo: ViewInfo,
    minResumePositionMs: Long = 15_000L,
    minDeltaFromCurrentMs: Long = 8_000L,
    progressLookup: (bvid: String, cid: Long) -> Long
): ResumePlaybackSuggestion? {
    val firstCid = loadedInfo.pages.firstOrNull { it.cid > 0L }?.cid
        ?: loadedInfo.cid.takeIf { it > 0L }
        ?: 0L
    if (requestCid > 0L && requestCid != firstCid) return null

    val currentBvid = loadedInfo.bvid
    val currentCid = loadedInfo.cid
    val currentPositionMs = if (currentBvid.isNotBlank() && currentCid > 0L) {
        progressLookup(currentBvid, currentCid).coerceAtLeast(0L)
    } else {
        0L
    }

    val pageSources = loadedInfo.pages.mapIndexedNotNull { index, page ->
        val cid = page.cid.takeIf { it > 0L } ?: return@mapIndexedNotNull null
        val pageNo = page.page.takeIf { it > 0 } ?: (index + 1)
        ResumeProgressSource(
            bvid = loadedInfo.bvid,
            cid = cid,
            label = "P$pageNo"
        )
    }

    val seasonEpisodeSources = loadedInfo.ugc_season
        ?.sections
        .orEmpty()
        .flatMap { section -> section.episodes }
        .mapIndexedNotNull { index, episode ->
            val bvid = episode.bvid.trim()
            val cid = episode.cid
            if (bvid.isBlank() || cid <= 0L) return@mapIndexedNotNull null
            val title = episode.title.trim().ifBlank { episode.arc?.title?.trim().orEmpty() }
            ResumeProgressSource(
                bvid = bvid,
                cid = cid,
                label = if (title.isBlank()) "第${index + 1}集" else "第${index + 1}集 $title"
            )
        }

    val orderedSources = (pageSources + seasonEpisodeSources)
        .distinctBy { source -> "${source.bvid}#${source.cid}" }
    val currentSourceIndex = orderedSources.indexOfFirst { source ->
        source.bvid == currentBvid && source.cid == currentCid
    }

    val candidates = orderedSources
        .mapIndexedNotNull { index, source ->
            if (source.bvid == currentBvid && source.cid == currentCid) return@mapIndexedNotNull null
            val positionMs = progressLookup(source.bvid, source.cid).coerceAtLeast(0L)
            if (positionMs < minResumePositionMs) {
                null
            } else {
                OrderedResumeCandidate(
                    order = index,
                    suggestion = ResumePlaybackSuggestion(
                        targetBvid = source.bvid,
                        targetCid = source.cid,
                        targetLabel = source.label,
                        positionMs = positionMs
                    )
                )
            }
        }

    val bestCandidate = candidates
        .maxByOrNull { candidate ->
            val isForwardFromCurrent = currentSourceIndex >= 0 && candidate.order > currentSourceIndex
            if (isForwardFromCurrent) {
                candidate.order + orderedSources.size
            } else {
                candidate.order
            }
        }?.suggestion ?: return null

    val candidateOnlySlightlyAhead = bestCandidate.positionMs > currentPositionMs &&
        bestCandidate.positionMs <= currentPositionMs + minDeltaFromCurrentMs
    if (candidateOnlySlightlyAhead) return null
    return bestCandidate
}

internal fun resolveResumePlaybackPromptKey(
    suggestion: ResumePlaybackSuggestion
): String = "${suggestion.targetBvid}#${suggestion.targetCid}"

internal fun shouldShowResumePlaybackPrompt(
    suggestion: ResumePlaybackSuggestion?,
    promptEnabled: Boolean,
    hasPromptedBefore: (String) -> Boolean
): Boolean {
    if (!promptEnabled) return false
    val candidate = suggestion ?: return false
    return !hasPromptedBefore(resolveResumePlaybackPromptKey(candidate))
}
