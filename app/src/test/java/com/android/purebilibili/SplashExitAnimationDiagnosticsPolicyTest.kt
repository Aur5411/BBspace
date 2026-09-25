package com.android.purebilibili

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashExitAnimationDiagnosticsPolicyTest {

    @Test
    fun resolvesSystemIconAsPreferredFlyoutTarget() {
        assertEquals(
            SplashFlyoutTargetType.SYSTEM_ICON,
            resolveSplashFlyoutTargetType(hasSystemIcon = true, hasFallbackIcon = true)
        )
    }

    @Test
    fun resolvesFallbackIconWhenSystemIconIsMissing() {
        assertEquals(
            SplashFlyoutTargetType.FALLBACK_ICON,
            resolveSplashFlyoutTargetType(hasSystemIcon = false, hasFallbackIcon = true)
        )
    }

    @Test
    fun resolvesSplashRootWhenNoIconTargetExists() {
        assertEquals(
            SplashFlyoutTargetType.SPLASH_ROOT,
            resolveSplashFlyoutTargetType(hasSystemIcon = false, hasFallbackIcon = false)
        )
    }

    @Test
    fun warmResumeLoggingEnabledOnlyAfterFirstResumeAndWithoutConfigChange() {
        assertTrue(
            shouldLogWarmResume(
                hasCompletedInitialResume = true,
                isChangingConfigurations = false
            )
        )
        assertFalse(
            shouldLogWarmResume(
                hasCompletedInitialResume = false,
                isChangingConfigurations = false
            )
        )
        assertFalse(
            shouldLogWarmResume(
                hasCompletedInitialResume = true,
                isChangingConfigurations = true
            )
        )
    }
}
