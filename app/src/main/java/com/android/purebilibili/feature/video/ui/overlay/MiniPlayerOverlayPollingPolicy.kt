package com.android.purebilibili.feature.video.ui.overlay

internal fun shouldPollMiniPlayerProgress(
    playerExists: Boolean,
    hostLifecycleStarted: Boolean,
    isMiniMode: Boolean,
    isActive: Boolean,
    isLiveMode: Boolean = false
): Boolean {
    if (isLiveMode) return false  // 📺 直播不需要进度轮询
    return playerExists && hostLifecycleStarted && isMiniMode && isActive
}

internal fun resolveMiniPlayerPollingIntervalMs(
    isPlaying: Boolean
): Long {
    return if (isPlaying) 500L else 1000L
}
