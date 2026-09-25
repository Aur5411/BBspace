package com.android.purebilibili.feature.video.subtitle

import kotlin.test.Test
import kotlin.test.assertTrue

class SubtitleFeaturePolicyTest {

    @Test
    fun `subtitle feature is enabled globally`() {
        assertTrue(isSubtitleFeatureEnabledForUser())
    }
}
