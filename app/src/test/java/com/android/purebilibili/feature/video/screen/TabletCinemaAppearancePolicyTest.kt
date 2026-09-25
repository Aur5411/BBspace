package com.android.purebilibili.feature.video.screen

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TabletCinemaAppearancePolicyTest {

    @Test
    fun darkModeMetaPanelUsesSurfaceInsteadOfPureWhite() {
        val darkSurface = Color(0xFF111722)

        val result = resolveCinemaMetaPanelContainerColor(
            isDarkTheme = true,
            surfaceColor = darkSurface
        )

        assertEquals(darkSurface.copy(alpha = 0.92f), result)
        assertNotEquals(Color.White, result)
    }

    @Test
    fun darkModeIntroCardUsesSurfaceContainerInsteadOfPureWhite() {
        val darkContainer = Color(0xFF161D29)

        val result = resolveCinemaIntroCardContainerColor(
            isDarkTheme = true,
            surfaceContainerLowColor = darkContainer
        )

        assertEquals(darkContainer.copy(alpha = 0.96f), result)
        assertNotEquals(Color.White, result)
    }

    @Test
    fun lightModeKeepsLegacyWhiteCards() {
        val result = resolveCinemaMetaPanelContainerColor(
            isDarkTheme = false,
            surfaceColor = Color(0xFFF6F6F6)
        )

        assertEquals(Color.White, result)
    }
}
