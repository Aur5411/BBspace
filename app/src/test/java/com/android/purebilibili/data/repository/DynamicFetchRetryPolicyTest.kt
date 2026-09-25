package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

class DynamicFetchRetryPolicyTest {

    @Test
    fun `rate limit and risk control api errors should not retry immediately`() {
        assertFalse(isRetryableDynamicApiError(code = -412, message = ""))
        assertFalse(isRetryableDynamicApiError(code = -352, message = ""))
        assertFalse(isRetryableDynamicApiError(code = -509, message = ""))
        assertFalse(isRetryableDynamicApiError(code = 22015, message = ""))
        assertFalse(isRetryableDynamicApiError(code = 34004, message = ""))
    }

    @Test
    fun `api error message containing precondition or risk should not retry immediately`() {
        assertFalse(isRetryableDynamicApiError(code = -999, message = "HTTP 412 Precondition Failed"))
        assertFalse(isRetryableDynamicApiError(code = -999, message = "触发风控"))
        assertFalse(isRetryableDynamicApiError(code = -999, message = "参数错误"))
    }

    @Test
    fun `http rate limit exceptions should not be retried but server failures still can`() {
        val rateLimit = HttpException(Response.error<Any>(429, "".toResponseBody()))
        val serverError = HttpException(Response.error<Any>(503, "".toResponseBody()))

        assertFalse(isRetryableDynamicException(rateLimit))
        assertTrue(isRetryableDynamicException(serverError))
    }

    @Test
    fun `friendly message maps 412 and risk`() {
        assertEquals(
            "请求过于频繁，请稍后重试",
            resolveDynamicFriendlyErrorMessage(code = -412, message = "")
        )
        assertEquals(
            "触发风控，请稍后重试",
            resolveDynamicFriendlyErrorMessage(code = -352, message = "")
        )
        assertEquals(
            "未登录，请先登录",
            resolveDynamicFriendlyErrorMessage(code = -101, message = "")
        )
    }

    @Test
    fun `retry delays should increase by attempts`() {
        assertEquals(250L, resolveDynamicRetryDelayMs(1))
        assertEquals(700L, resolveDynamicRetryDelayMs(2))
        assertEquals(1200L, resolveDynamicRetryDelayMs(3))
    }
}
