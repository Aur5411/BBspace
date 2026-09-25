package com.android.purebilibili.feature.download

internal fun selectAllBatchDownloadCandidates(
    candidates: List<BatchDownloadCandidate>
): List<BatchDownloadCandidate> = candidates.map { it.copy(selected = true) }

internal fun invertBatchDownloadCandidateSelection(
    candidates: List<BatchDownloadCandidate>
): List<BatchDownloadCandidate> = candidates.map { it.copy(selected = !it.selected) }

internal fun selectOnlyUndownloadedBatchCandidates(
    candidates: List<BatchDownloadCandidate>,
    downloadedIds: Set<String>
): List<BatchDownloadCandidate> {
    return candidates.map { candidate ->
        candidate.copy(selected = candidate.id !in downloadedIds)
    }
}

internal fun canConfirmBatchDownload(
    candidates: List<BatchDownloadCandidate>
): Boolean = candidates.any { it.selected }
