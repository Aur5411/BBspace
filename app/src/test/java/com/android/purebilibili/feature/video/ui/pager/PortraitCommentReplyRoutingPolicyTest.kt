package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitCommentReplyRoutingPolicyTest {

    @Test
    fun `portrait comment reply action should open composer`() {
        assertTrue(shouldOpenPortraitCommentReplyComposer())
    }

    @Test
    fun `portrait comment subreply preview should open thread detail in both embedded and detached modes`() {
        assertTrue(shouldOpenPortraitCommentThreadDetail(useEmbeddedPresentation = false))
        assertTrue(shouldOpenPortraitCommentThreadDetail(useEmbeddedPresentation = true))
    }
}
