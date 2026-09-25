package com.android.purebilibili.feature.video.playback.policy

internal fun resolvePluginPollingIntervalMs(
    hasPlugins: Boolean,
    isPlaying: Boolean
): Long {
    return when {
        !hasPlugins -> 5_000L
        isPlaying -> 750L
        else -> 2_000L
    }
}

internal fun shouldDispatchPluginPositionUpdate(
    lastDispatchedPositionMs: Long?,
    currentPositionMs: Long,
    minPositionDeltaMs: Long = 400L
): Boolean {
    val previous = lastDispatchedPositionMs ?: return true
    return currentPositionMs < previous || currentPositionMs - previous >= minPositionDeltaMs
}
