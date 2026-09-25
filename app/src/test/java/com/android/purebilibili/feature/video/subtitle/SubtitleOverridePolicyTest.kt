package com.android.purebilibili.feature.video.subtitle

import kotlin.test.Test
import kotlin.test.assertEquals

class SubtitleOverridePolicyTest {

    @Test
    fun resolveSubtitlePreferenceSession_buildsStableKeyAndInitialModeFromAutoPreference() {
        val session = resolveSubtitlePreferenceSession(
            bvid = "BV1TEST",
            cid = 1001L,
            primaryLanguage = "zh-Hans",
            secondaryLanguage = "en-US",
            primaryTrackLikelyAi = false,
            secondaryTrackLikelyAi = false,
            hasPrimaryTrack = true,
            hasSecondaryTrack = true,
            preference = SubtitleAutoPreference.ON
        )

        assertEquals("BV1TEST_1001_zh-Hans_en-US_false_false_ON", session.key)
        assertEquals(SubtitleDisplayMode.BILINGUAL, session.initialMode)
    }

    @Test
    fun resolveSubtitleDisplayModePreference_keepsManualOverrideForSameVideoSession() {
        val mode = resolveSubtitleDisplayModePreference(
            previousSessionKey = "BV1TEST_1001_zh-Hans_en-US_false_false_ON",
            nextSessionKey = "BV1TEST_1001_zh-Hans_en-US_false_false_ON",
            previousMode = SubtitleDisplayMode.OFF,
            nextInitialMode = SubtitleDisplayMode.BILINGUAL
        )

        assertEquals(SubtitleDisplayMode.OFF, mode)
    }

    @Test
    fun resolveSubtitleDisplayModePreference_reinitializesForNewVideoSession() {
        val mode = resolveSubtitleDisplayModePreference(
            previousSessionKey = "BV1OLD_1000_zh-Hans_en-US_false_false_ON",
            nextSessionKey = "BV1NEW_1001_zh-Hans_en-US_false_false_ON",
            previousMode = SubtitleDisplayMode.OFF,
            nextInitialMode = SubtitleDisplayMode.BILINGUAL
        )

        assertEquals(SubtitleDisplayMode.BILINGUAL, mode)
    }
}
