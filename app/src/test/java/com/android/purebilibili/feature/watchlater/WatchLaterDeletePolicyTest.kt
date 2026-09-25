package com.android.purebilibili.feature.watchlater

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatchLaterDeletePolicyTest {

    @Test
    fun `isRetryableWatchLaterDeleteError should recognize throttle errors`() {
        assertTrue(isRetryableWatchLaterDeleteError(-412, "请求过于频繁"))
        assertTrue(isRetryableWatchLaterDeleteError(34004, "操作太频繁"))
        assertTrue(isRetryableWatchLaterDeleteError(1, "Too many requests"))
    }

    @Test
    fun `isRetryableWatchLaterDeleteError should reject non-throttle errors`() {
        assertFalse(isRetryableWatchLaterDeleteError(-404, "内容不存在"))
        assertFalse(isRetryableWatchLaterDeleteError(0, ""))
    }
}

