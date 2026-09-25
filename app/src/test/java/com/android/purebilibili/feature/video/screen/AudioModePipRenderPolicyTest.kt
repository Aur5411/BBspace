package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioModePipRenderPolicyTest {

    @Test
    fun normalAudioModeShowsFullChrome() {
        val policy = resolveAudioModeRenderPolicy(isInPipMode = false)

        assertTrue(policy.showTopBar)
        assertTrue(policy.showControlsContent)
        assertFalse(policy.showCompactPipCoverOnly)
    }

    @Test
    fun pipAudioModeCollapsesToCoverOnlyChrome() {
        val policy = resolveAudioModeRenderPolicy(isInPipMode = true)

        assertFalse(policy.showTopBar)
        assertFalse(policy.showControlsContent)
        assertTrue(policy.showCompactPipCoverOnly)
    }
}
