package com.android.purebilibili.feature.video.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerToastPolicyTest {

    @Test
    fun `quality toast uses centered highlight presentation`() {
        assertEquals(
            PlayerToastPresentation.CenteredHighlight,
            buildQualityToastMessage("已切换至 1080P").presentation
        )
    }

    @Test
    fun `default toast uses standard presentation`() {
        assertEquals(
            PlayerToastPresentation.Standard,
            buildPlayerToastMessage("收藏成功").presentation
        )
    }
}
