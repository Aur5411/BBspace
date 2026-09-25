package com.android.purebilibili.feature.download

import kotlin.test.Test
import kotlin.test.assertEquals

class BatchDownloadDialogLayoutPolicyTest {

    @Test
    fun smallScreenShrinksCandidateListToKeepFooterReachable() {
        assertEquals(
            129,
            resolveBatchDownloadCandidateListMaxHeight(
                screenHeightDp = 640,
                qualityOptionCount = 4
            )
        )
    }

    @Test
    fun tallScreenKeepsDefaultCandidateListHeight() {
        assertEquals(
            280,
            resolveBatchDownloadCandidateListMaxHeight(
                screenHeightDp = 900,
                qualityOptionCount = 4
            )
        )
    }

    @Test
    fun extremelyShortScreenKeepsUsableMinimumListHeight() {
        assertEquals(
            120,
            resolveBatchDownloadCandidateListMaxHeight(
                screenHeightDp = 520,
                qualityOptionCount = 5
            )
        )
    }
}
