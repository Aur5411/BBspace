package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.WbiImg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeWebFeedWbiPolicyTest {

    @Test
    fun prefersCachedWbiKeysWhenAvailable() {
        val cached = "cached-img" to "cached-sub"
        val result = resolveHomeFeedWbiKeys(
            cachedKeys = cached,
            navWbiImg = WbiImg(
                img_url = "https://i0.hdslb.com/bfs/wbi/nav-img.png",
                sub_url = "https://i0.hdslb.com/bfs/wbi/nav-sub.png"
            )
        )

        assertEquals(cached, result)
    }

    @Test
    fun fallsBackToNavWbiImageWhenCacheMissing() {
        val result = resolveHomeFeedWbiKeys(
            cachedKeys = null,
            navWbiImg = WbiImg(
                img_url = "https://i0.hdslb.com/bfs/wbi/nav-img.png",
                sub_url = "https://i0.hdslb.com/bfs/wbi/nav-sub.png"
            )
        )

        assertEquals("nav-img" to "nav-sub", result)
    }

    @Test
    fun returnsNullWhenNoWbiSourceExists() {
        assertNull(
            resolveHomeFeedWbiKeys(
                cachedKeys = null,
                navWbiImg = null
            )
        )
    }
}
