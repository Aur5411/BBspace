package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.store.FullscreenAspectRatio

internal fun FullscreenAspectRatio.toVideoAspectRatio(): VideoAspectRatio {
    return when (this) {
        FullscreenAspectRatio.FIT -> VideoAspectRatio.FIT
        FullscreenAspectRatio.FILL -> VideoAspectRatio.FILL
        FullscreenAspectRatio.RATIO_16_9 -> VideoAspectRatio.RATIO_16_9
        FullscreenAspectRatio.RATIO_4_3 -> VideoAspectRatio.RATIO_4_3
        FullscreenAspectRatio.STRETCH -> VideoAspectRatio.STRETCH
    }
}

internal fun VideoAspectRatio.toFullscreenAspectRatio(): FullscreenAspectRatio {
    return when (this) {
        VideoAspectRatio.FIT -> FullscreenAspectRatio.FIT
        VideoAspectRatio.FILL -> FullscreenAspectRatio.FILL
        VideoAspectRatio.RATIO_16_9 -> FullscreenAspectRatio.RATIO_16_9
        VideoAspectRatio.RATIO_4_3 -> FullscreenAspectRatio.RATIO_4_3
        VideoAspectRatio.STRETCH -> FullscreenAspectRatio.STRETCH
    }
}
