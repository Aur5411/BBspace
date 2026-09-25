package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class PersistentProgressBarPolicyTest {

    @Test
    fun `resolve persistent progress fraction should clamp to valid range`() {
        assertEquals(0f, resolvePersistentProgressFraction(current = 0L, duration = 0L))
        assertEquals(0f, resolvePersistentProgressFraction(current = -100L, duration = 1000L))
        assertEquals(0.5f, resolvePersistentProgressFraction(current = 500L, duration = 1000L))
        assertEquals(1f, resolvePersistentProgressFraction(current = 1500L, duration = 1000L))
    }
}
