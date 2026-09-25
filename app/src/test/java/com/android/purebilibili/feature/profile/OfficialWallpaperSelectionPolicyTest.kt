package com.android.purebilibili.feature.profile

import com.android.purebilibili.data.model.response.SplashItem
import kotlin.test.Test
import kotlin.test.assertEquals

class OfficialWallpaperSelectionPolicyTest {

    @Test
    fun detailSelection_prefersOriginalImageOverThumbnail() {
        val item = SplashItem(
            id = 1L,
            thumb = "//thumb.jpg",
            image = "//full.jpg"
        )

        assertEquals("https://full.jpg", resolveOfficialWallpaperDetailUrl(item))
    }

    @Test
    fun gridThumbnail_prefersThumbForListDisplay() {
        val item = SplashItem(
            id = 1L,
            thumb = "//thumb.jpg",
            image = "//full.jpg"
        )

        assertEquals("https://thumb.jpg", resolveOfficialWallpaperThumbnailUrl(item))
    }
}
