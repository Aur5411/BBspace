package com.android.purebilibili.feature.video.ui.section

internal enum class VideoDetailMotionBudget {
    FULL,
    REDUCED
}

internal fun resolveVideoDetailMotionBudget(
    isTabSwitching: Boolean,
    isContentScrolling: Boolean
): VideoDetailMotionBudget {
    return if (isTabSwitching || isContentScrolling) {
        VideoDetailMotionBudget.REDUCED
    } else {
        VideoDetailMotionBudget.FULL
    }
}

internal fun shouldAnimateVideoDetailLayout(
    budget: VideoDetailMotionBudget
): Boolean = budget == VideoDetailMotionBudget.FULL
