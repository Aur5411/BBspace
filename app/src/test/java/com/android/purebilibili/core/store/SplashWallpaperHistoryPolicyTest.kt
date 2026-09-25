package com.android.purebilibili.core.store

import org.junit.Assert.assertEquals
import org.junit.Test

class SplashWallpaperHistoryPolicyTest {

    @Test
    fun `appendSplashWallpaperHistory prepends latest and de-duplicates`() {
        val updated = appendSplashWallpaperHistory(
            existing = listOf("content://a", "content://b"),
            newUri = "content://b",
            maxCount = 5
        )
        assertEquals(listOf("content://b", "content://a"), updated)
    }

    @Test
    fun `appendSplashWallpaperHistory enforces max count`() {
        val updated = appendSplashWallpaperHistory(
            existing = listOf("1", "2", "3"),
            newUri = "4",
            maxCount = 3
        )
        assertEquals(listOf("4", "1", "2"), updated)
    }

    @Test
    fun `decode and encode splash wallpaper history keeps normalized ordering`() {
        val encoded = encodeSplashWallpaperHistory(
            listOf("content://1", "", "content://2", "content://1")
        )
        assertEquals(listOf("content://1", "content://2"), decodeSplashWallpaperHistory(encoded))
    }
}
