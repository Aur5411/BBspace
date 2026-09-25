package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.store.FullscreenAspectRatio
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoAspectRatioPreferenceMapperTest {

    @Test
    fun `toVideoAspectRatio should map each stored preference`() {
        assertEquals(VideoAspectRatio.FIT, FullscreenAspectRatio.FIT.toVideoAspectRatio())
        assertEquals(VideoAspectRatio.FILL, FullscreenAspectRatio.FILL.toVideoAspectRatio())
        assertEquals(VideoAspectRatio.RATIO_16_9, FullscreenAspectRatio.RATIO_16_9.toVideoAspectRatio())
        assertEquals(VideoAspectRatio.RATIO_4_3, FullscreenAspectRatio.RATIO_4_3.toVideoAspectRatio())
        assertEquals(VideoAspectRatio.STRETCH, FullscreenAspectRatio.STRETCH.toVideoAspectRatio())
    }

    @Test
    fun `toFullscreenAspectRatio should map supported player ratios`() {
        assertEquals(FullscreenAspectRatio.FIT, VideoAspectRatio.FIT.toFullscreenAspectRatio())
        assertEquals(FullscreenAspectRatio.FILL, VideoAspectRatio.FILL.toFullscreenAspectRatio())
        assertEquals(FullscreenAspectRatio.RATIO_16_9, VideoAspectRatio.RATIO_16_9.toFullscreenAspectRatio())
        assertEquals(FullscreenAspectRatio.RATIO_4_3, VideoAspectRatio.RATIO_4_3.toFullscreenAspectRatio())
        assertEquals(FullscreenAspectRatio.STRETCH, VideoAspectRatio.STRETCH.toFullscreenAspectRatio())
    }
}
