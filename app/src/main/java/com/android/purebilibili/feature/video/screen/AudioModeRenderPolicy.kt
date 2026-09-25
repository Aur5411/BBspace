package com.android.purebilibili.feature.video.screen

internal data class AudioModeRenderPolicy(
    val showTopBar: Boolean,
    val showControlsContent: Boolean,
    val showCompactPipCoverOnly: Boolean
)

internal fun resolveAudioModeRenderPolicy(
    isInPipMode: Boolean
): AudioModeRenderPolicy {
    return if (isInPipMode) {
        AudioModeRenderPolicy(
            showTopBar = false,
            showControlsContent = false,
            showCompactPipCoverOnly = true
        )
    } else {
        AudioModeRenderPolicy(
            showTopBar = true,
            showControlsContent = true,
            showCompactPipCoverOnly = false
        )
    }
}
