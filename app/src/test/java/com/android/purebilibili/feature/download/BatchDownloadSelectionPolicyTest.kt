package com.android.purebilibili.feature.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BatchDownloadSelectionPolicyTest {

    private val candidates = listOf(
        BatchDownloadCandidate(
            id = "a",
            bvid = "BV1",
            cid = 1L,
            title = "A",
            label = "A",
            cover = "cover",
            ownerName = "UP",
            selected = false
        ),
        BatchDownloadCandidate(
            id = "b",
            bvid = "BV2",
            cid = 2L,
            title = "B",
            label = "B",
            cover = "cover",
            ownerName = "UP",
            selected = true
        )
    )

    @Test
    fun selectAllBatchDownloadCandidates_marksEveryCandidateSelected() {
        assertTrue(selectAllBatchDownloadCandidates(candidates).all { it.selected })
    }

    @Test
    fun invertBatchDownloadCandidateSelection_flipsEverySelectionBit() {
        val inverted = invertBatchDownloadCandidateSelection(candidates)

        assertTrue(inverted[0].selected)
        assertFalse(inverted[1].selected)
    }

    @Test
    fun selectOnlyUndownloadedBatchCandidates_clearsDownloadedItems() {
        val selected = selectOnlyUndownloadedBatchCandidates(
            candidates = candidates,
            downloadedIds = setOf("b")
        )

        assertTrue(selected[0].selected)
        assertFalse(selected[1].selected)
    }

    @Test
    fun canConfirmBatchDownload_requiresAtLeastOneSelection() {
        assertTrue(canConfirmBatchDownload(candidates))
        assertFalse(canConfirmBatchDownload(candidates.map { it.copy(selected = false) }))
    }
}
