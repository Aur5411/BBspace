package com.android.purebilibili.feature.video.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuDialogTopReservePolicyTest {

    @Test
    fun `portrait non fullscreen should reserve player bottom as top safe area`() {
        val reservedPx = resolveDanmakuDialogTopReservePx(
            isLandscape = false,
            isFullscreenMode = false,
            isPortraitFullscreen = false,
            playerBottomPx = 420
        )

        assertEquals(420, reservedPx)
    }

    @Test
    fun `fullscreen or landscape should not reserve top safe area`() {
        assertEquals(
            0,
            resolveDanmakuDialogTopReservePx(
                isLandscape = true,
                isFullscreenMode = false,
                isPortraitFullscreen = false,
                playerBottomPx = 420
            )
        )

        assertEquals(
            0,
            resolveDanmakuDialogTopReservePx(
                isLandscape = false,
                isFullscreenMode = true,
                isPortraitFullscreen = false,
                playerBottomPx = 420
            )
        )

        assertEquals(
            0,
            resolveDanmakuDialogTopReservePx(
                isLandscape = false,
                isFullscreenMode = false,
                isPortraitFullscreen = true,
                playerBottomPx = 420
            )
        )
    }

    @Test
    fun `should use fallback height when player bottom is unavailable`() {
        val reservedPx = resolveDanmakuDialogTopReservePx(
            isLandscape = false,
            isFullscreenMode = false,
            isPortraitFullscreen = false,
            playerBottomPx = null,
            fallbackPlayerBottomPx = 360
        )

        assertEquals(360, reservedPx)
    }
}
