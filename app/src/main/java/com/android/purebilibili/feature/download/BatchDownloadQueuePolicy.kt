package com.android.purebilibili.feature.download

internal data class BatchDownloadQueueResult(
    val addedCount: Int,
    val skippedExistingCount: Int,
    val failedCount: Int
)

internal fun summarizeBatchDownloadQueueResult(
    result: BatchDownloadQueueResult
): String {
    val segments = buildList {
        if (result.addedCount > 0) add("已加入 ${result.addedCount} 个任务")
        if (result.skippedExistingCount > 0) add("${result.skippedExistingCount} 个已存在")
        if (result.failedCount > 0) add("${result.failedCount} 个失败")
    }
    return segments.joinToString("，").ifBlank { "未加入任何任务" }
}
