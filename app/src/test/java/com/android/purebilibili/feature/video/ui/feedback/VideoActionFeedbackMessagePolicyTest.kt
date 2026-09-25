package com.android.purebilibili.feature.video.ui.feedback

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoActionFeedbackMessagePolicyTest {

    @Test
    fun `full triple success resolves to concise completion message`() {
        assertEquals(
            "三连完成",
            resolveTripleActionFeedbackMessage(
                likeSuccess = true,
                coinSuccess = true,
                favoriteSuccess = true,
                coinFailureMessage = null
            )
        )
    }

    @Test
    fun `partial triple success resolves to compact joined summary`() {
        assertEquals(
            "已点赞 已收藏",
            resolveTripleActionFeedbackMessage(
                likeSuccess = true,
                coinSuccess = false,
                favoriteSuccess = true,
                coinFailureMessage = null
            )
        )
    }

    @Test
    fun `coin failure falls back to concise failure reason`() {
        assertEquals(
            "硬币不足",
            resolveTripleActionFeedbackMessage(
                likeSuccess = false,
                coinSuccess = false,
                favoriteSuccess = false,
                coinFailureMessage = "硬币不足"
            )
        )
    }
}
