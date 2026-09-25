package com.android.purebilibili.feature.settings

data class SplashRandomPoolPreviewState(
    val totalCount: Int,
    val previewUris: List<String>
)

internal fun resolveSplashRandomPoolPreviewState(
    poolUris: List<String>,
    maxPreviewCount: Int = 3
): SplashRandomPoolPreviewState {
    val normalizedPool = poolUris
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .toList()
    return SplashRandomPoolPreviewState(
        totalCount = normalizedPool.size,
        previewUris = normalizedPool.take(maxPreviewCount.coerceAtLeast(0))
    )
}
