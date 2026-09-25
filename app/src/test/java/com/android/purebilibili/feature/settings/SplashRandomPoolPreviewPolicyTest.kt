package com.android.purebilibili.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SplashRandomPoolPreviewPolicyTest {

    @Test
    fun `resolveSplashRandomPoolPreviewState limits preview to max and keeps total count`() {
        val state = resolveSplashRandomPoolPreviewState(
            poolUris = listOf("a", "b", "c", "d"),
            maxPreviewCount = 3
        )

        assertEquals(4, state.totalCount)
        assertEquals(listOf("a", "b", "c"), state.previewUris)
    }

    @Test
    fun `resolveSplashRandomPoolPreviewState trims blanks and de-duplicates`() {
        val state = resolveSplashRandomPoolPreviewState(
            poolUris = listOf(" ", "a", "a", "b"),
            maxPreviewCount = 3
        )

        assertEquals(2, state.totalCount)
        assertEquals(listOf("a", "b"), state.previewUris)
    }
}
