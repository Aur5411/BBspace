package com.android.purebilibili.feature.video.viewmodel

enum class PlayerToastPresentation {
    Standard,
    CenteredHighlight
}

data class PlayerToastMessage(
    val message: String,
    val presentation: PlayerToastPresentation = PlayerToastPresentation.Standard
)

internal fun buildPlayerToastMessage(message: String): PlayerToastMessage {
    return PlayerToastMessage(
        message = message,
        presentation = PlayerToastPresentation.Standard
    )
}

internal fun buildQualityToastMessage(message: String): PlayerToastMessage {
    return PlayerToastMessage(
        message = message,
        presentation = PlayerToastPresentation.CenteredHighlight
    )
}
