package com.android.purebilibili.feature.video.viewmodel

internal data class FavoriteSaveUiState(
    val isFavorited: Boolean,
    val favoriteCount: Int,
    val membershipChanged: Boolean
)

internal fun resolveFavoriteSaveUiState(
    originalFolderIds: Set<Long>,
    selectedFolderIds: Set<Long>,
    currentFavoriteCount: Int
): FavoriteSaveUiState {
    val wasFavorited = originalFolderIds.isNotEmpty()
    val isFavorited = selectedFolderIds.isNotEmpty()
    val membershipChanged = wasFavorited != isFavorited
    val favoriteCount = when {
        !wasFavorited && isFavorited -> currentFavoriteCount + 1
        wasFavorited && !isFavorited -> (currentFavoriteCount - 1).coerceAtLeast(0)
        else -> currentFavoriteCount
    }

    return FavoriteSaveUiState(
        isFavorited = isFavorited,
        favoriteCount = favoriteCount,
        membershipChanged = membershipChanged
    )
}
