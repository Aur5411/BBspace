package com.android.purebilibili.feature.download

import kotlin.test.Test
import kotlin.test.assertEquals

class BatchDownloadQueuePolicyTest {

    @Test
    fun summarizeBatchDownloadQueueResult_formatsAddedSkippedAndFailedCounts() {
        assertEquals(
            "已加入 3 个任务，1 个已存在，2 个失败",
            summarizeBatchDownloadQueueResult(
                BatchDownloadQueueResult(
                    addedCount = 3,
                    skippedExistingCount = 1,
                    failedCount = 2
                )
            )
        )
    }

    @Test
    fun summarizeBatchDownloadQueueResult_hidesZeroCountSegments() {
        assertEquals(
            "已加入 2 个任务",
            summarizeBatchDownloadQueueResult(
                BatchDownloadQueueResult(
                    addedCount = 2,
                    skippedExistingCount = 0,
                    failedCount = 0
                )
            )
        )
    }
}
