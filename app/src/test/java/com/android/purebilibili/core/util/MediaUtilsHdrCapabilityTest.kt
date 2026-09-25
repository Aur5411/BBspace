package com.android.purebilibili.core.util

import android.view.Display
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaUtilsHdrCapabilityTest {

    @Test
    fun `supportsGenericHdrTypes returns true for hdr10 and hlg`() {
        assertTrue(
            MediaUtils.supportsGenericHdrTypes(
                intArrayOf(
                    Display.HdrCapabilities.HDR_TYPE_HDR10,
                    Display.HdrCapabilities.HDR_TYPE_HLG
                )
            )
        )
    }

    @Test
    fun `supportsGenericHdrTypes returns false for null or empty types`() {
        assertFalse(MediaUtils.supportsGenericHdrTypes(null))
        assertFalse(MediaUtils.supportsGenericHdrTypes(intArrayOf()))
    }

    @Test
    fun `supportsDolbyVisionType returns true when dolby type exists`() {
        assertTrue(
            MediaUtils.supportsDolbyVisionType(
                intArrayOf(Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION)
            )
        )
    }

    @Test
    fun `supportsDolbyVisionType returns false when dolby type missing`() {
        assertFalse(
            MediaUtils.supportsDolbyVisionType(
                intArrayOf(Display.HdrCapabilities.HDR_TYPE_HDR10)
            )
        )
    }
}
