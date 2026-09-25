package com.android.purebilibili

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainActivityNotificationNavigationPolicyTest {

    @Test
    fun skipsNavigationWhenAlreadyOnSameVideoRoute() {
        assertFalse(
            shouldNavigateToVideoFromNotification(
                currentRoute = "video/{bvid}?cid={cid}&cover={cover}",
                currentBvid = "BV1same",
                targetBvid = "BV1same"
            )
        )
    }

    @Test
    fun navigatesWhenCurrentVideoDiffers() {
        assertTrue(
            shouldNavigateToVideoFromNotification(
                currentRoute = "video/{bvid}?cid={cid}&cover={cover}",
                currentBvid = "BV1old",
                targetBvid = "BV1new"
            )
        )
    }

    @Test
    fun navigatesWhenNotInVideoRoute() {
        assertTrue(
            shouldNavigateToVideoFromNotification(
                currentRoute = "home",
                currentBvid = "BV1same",
                targetBvid = "BV1same"
            )
        )
    }
}
