package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals

class FullscreenModeMappingPolicyTest {

    @Test
    fun legacyRatioModeValue_fallsBackToAuto() {
        assertEquals(FullscreenMode.AUTO, FullscreenMode.fromValue(4))
    }

    @Test
    fun legacyGravityModeValue_fallsBackToAuto() {
        assertEquals(FullscreenMode.AUTO, FullscreenMode.fromValue(5))
    }
}
