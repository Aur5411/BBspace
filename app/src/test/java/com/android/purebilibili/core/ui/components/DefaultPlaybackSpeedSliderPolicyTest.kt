package com.android.purebilibili.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultPlaybackSpeedSliderPolicyTest {

    @Test
    fun `normalize default playback speed quantizes to five-hundredths`() {
        assertEquals(1.25f, normalizeDefaultPlaybackPreferenceSpeed(1.23f))
        assertEquals(1.20f, normalizeDefaultPlaybackPreferenceSpeed(1.22f))
    }

    @Test
    fun `normalize default playback speed clamps into supported range`() {
        assertEquals(0.5f, normalizeDefaultPlaybackPreferenceSpeed(0.1f))
        assertEquals(2.0f, normalizeDefaultPlaybackPreferenceSpeed(2.4f))
    }

    @Test
    fun `resolve default playback preset only returns configured common values`() {
        assertEquals(1.25f, resolveDefaultPlaybackPreset(1.25f))
        assertEquals(2.0f, resolveDefaultPlaybackPreset(2.0f))
        assertNull(resolveDefaultPlaybackPreset(1.2f))
    }

    @Test
    fun `format default playback speed trims trailing zeros`() {
        assertEquals("1x", formatDefaultPlaybackSpeed(1.0f))
        assertEquals("1.25x", formatDefaultPlaybackSpeed(1.25f))
        assertEquals("2x", formatDefaultPlaybackSpeed(2.0f))
    }
}
