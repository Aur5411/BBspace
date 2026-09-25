package com.android.purebilibili.feature.video.ui.feedback

data class TripleActionVisualState(
    val isLiked: Boolean,
    val coinCount: Int,
    val isFavorited: Boolean
)

internal fun shouldTreatTripleActionCoinFailureAsAlreadyCoined(
    coinFailureMessage: String?
): Boolean {
    return coinFailureMessage?.contains("已投满2个硬币") == true
}

fun resolveTripleActionVisualState(
    currentLiked: Boolean,
    currentCoinCount: Int,
    currentFavorited: Boolean,
    likeSuccess: Boolean,
    coinSuccess: Boolean,
    coinFailureMessage: String?,
    favoriteSuccess: Boolean
): TripleActionVisualState {
    return TripleActionVisualState(
        isLiked = currentLiked || likeSuccess,
        coinCount = when {
            coinSuccess -> maxOf(currentCoinCount, 2)
            shouldTreatTripleActionCoinFailureAsAlreadyCoined(coinFailureMessage) -> 2
            else -> currentCoinCount
        },
        isFavorited = currentFavorited || favoriteSuccess
    )
}
