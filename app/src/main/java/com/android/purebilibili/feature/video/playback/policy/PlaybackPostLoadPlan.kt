package com.android.purebilibili.feature.video.playback.policy

internal enum class PlaybackPostLoadTask {
    PLAYER_INFO,
    VIDEO_SHOT,
    REFRESH_DEFERRED_SIGNALS,
    LOAD_FOLLOWING_MIDS,
    OWNER_STATS,
    VIDEO_TAGS,
    AI_SUMMARY,
    ONLINE_COUNT,
    HEARTBEAT,
    PLUGIN_ON_VIDEO_LOAD,
    START_PLUGIN_CHECK
}

internal data class PlaybackPostLoadTaskSpec(
    val task: PlaybackPostLoadTask,
    val delayMs: Long
)

internal fun buildPlaybackPostLoadPlan(
    isLoggedIn: Boolean,
    shouldShowOnlineCount: Boolean
): List<PlaybackPostLoadTaskSpec> {
    val plan = mutableListOf(
        PlaybackPostLoadTaskSpec(
            task = PlaybackPostLoadTask.PLAYER_INFO,
            delayMs = 150L
        ),
        PlaybackPostLoadTaskSpec(
            task = PlaybackPostLoadTask.VIDEO_SHOT,
            delayMs = 150L
        ),
        PlaybackPostLoadTaskSpec(
            task = PlaybackPostLoadTask.OWNER_STATS,
            delayMs = 450L
        ),
        PlaybackPostLoadTaskSpec(
            task = PlaybackPostLoadTask.VIDEO_TAGS,
            delayMs = 450L
        ),
        PlaybackPostLoadTaskSpec(
            task = PlaybackPostLoadTask.HEARTBEAT,
            delayMs = 900L
        ),
        PlaybackPostLoadTaskSpec(
            task = PlaybackPostLoadTask.PLUGIN_ON_VIDEO_LOAD,
            delayMs = 900L
        ),
        PlaybackPostLoadTaskSpec(
            task = PlaybackPostLoadTask.START_PLUGIN_CHECK,
            delayMs = 1_200L
        ),
        PlaybackPostLoadTaskSpec(
            task = PlaybackPostLoadTask.AI_SUMMARY,
            delayMs = 1_500L
        )
    )

    if (isLoggedIn) {
        plan += PlaybackPostLoadTaskSpec(
            task = PlaybackPostLoadTask.REFRESH_DEFERRED_SIGNALS,
            delayMs = 450L
        )
        plan += PlaybackPostLoadTaskSpec(
            task = PlaybackPostLoadTask.LOAD_FOLLOWING_MIDS,
            delayMs = 450L
        )
    }

    if (shouldShowOnlineCount) {
        plan += PlaybackPostLoadTaskSpec(
            task = PlaybackPostLoadTask.ONLINE_COUNT,
            delayMs = 1_500L
        )
    }

    return plan.sortedWith(
        compareBy<PlaybackPostLoadTaskSpec> { spec -> spec.delayMs }
            .thenBy { spec -> spec.task.ordinal }
    )
}
