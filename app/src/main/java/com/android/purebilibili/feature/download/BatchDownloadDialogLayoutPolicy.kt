package com.android.purebilibili.feature.download

import kotlin.math.roundToInt

private const val BATCH_DOWNLOAD_DIALOG_MAX_HEIGHT_RATIO = 0.92f
private const val BATCH_DOWNLOAD_DIALOG_VERTICAL_CHROME_DP = 268
private const val BATCH_DOWNLOAD_DIALOG_QUALITY_ROW_HEIGHT_DP = 48
private const val BATCH_DOWNLOAD_DIALOG_MIN_CANDIDATE_LIST_MAX_HEIGHT_DP = 120
private const val BATCH_DOWNLOAD_DIALOG_DEFAULT_CANDIDATE_LIST_MAX_HEIGHT_DP = 280

internal fun resolveBatchDownloadDialogMaxHeight(screenHeightDp: Int): Int {
    return (screenHeightDp * BATCH_DOWNLOAD_DIALOG_MAX_HEIGHT_RATIO).roundToInt()
}

internal fun resolveBatchDownloadCandidateListMaxHeight(
    screenHeightDp: Int,
    qualityOptionCount: Int
): Int {
    val availableHeight = resolveBatchDownloadDialogMaxHeight(screenHeightDp) -
        BATCH_DOWNLOAD_DIALOG_VERTICAL_CHROME_DP -
        (qualityOptionCount.coerceAtLeast(1) * BATCH_DOWNLOAD_DIALOG_QUALITY_ROW_HEIGHT_DP)

    return availableHeight.coerceIn(
        minimumValue = BATCH_DOWNLOAD_DIALOG_MIN_CANDIDATE_LIST_MAX_HEIGHT_DP,
        maximumValue = BATCH_DOWNLOAD_DIALOG_DEFAULT_CANDIDATE_LIST_MAX_HEIGHT_DP
    )
}
