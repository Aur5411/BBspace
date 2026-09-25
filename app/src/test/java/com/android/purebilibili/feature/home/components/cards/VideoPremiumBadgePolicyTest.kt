package com.android.purebilibili.feature.home.components.cards

import com.android.purebilibili.data.model.response.VideoRights
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoPremiumBadgePolicyTest {

    @Test
    fun ugcPayVideos_showChargeExclusiveBadge() {
        assertEquals(
            "充电专属",
            resolveVideoPremiumBadgeLabel(
                VideoRights(ugcPay = 1)
            )
        )
    }

    @Test
    fun paidVideos_showPaidBadge() {
        assertEquals(
            "付费",
            resolveVideoPremiumBadgeLabel(
                VideoRights(pay = 1)
            )
        )
    }

    @Test
    fun regularVideos_doNotShowBadge() {
        assertNull(resolveVideoPremiumBadgeLabel(VideoRights()))
    }
}
