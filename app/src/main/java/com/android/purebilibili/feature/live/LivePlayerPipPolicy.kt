package com.android.purebilibili.feature.live

private const val LIVE_PIP_MIN_SDK_INT = 26

internal fun shouldShowLivePipButton(
    sdkInt: Int
): Boolean {
    return sdkInt >= LIVE_PIP_MIN_SDK_INT
}

internal fun shouldPauseLivePlaybackOnPause(
    isInPictureInPictureMode: Boolean,
    isPipRequested: Boolean,
    shouldKeepPlayingInBackground: Boolean = false
): Boolean {
    if (isInPictureInPictureMode || isPipRequested) {
        return false
    }
    return !shouldKeepPlayingInBackground
}
