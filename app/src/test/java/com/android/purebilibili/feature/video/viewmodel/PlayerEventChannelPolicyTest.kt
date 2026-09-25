package com.android.purebilibili.feature.video.viewmodel

import kotlinx.coroutines.channels.Channel
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerEventChannelPolicyTest {

    @Test
    fun `transient player ui events use buffered channels to avoid blocking success flows`() {
        assertEquals(
            Channel.BUFFERED,
            resolvePlayerTransientEventChannelCapacity()
        )
    }
}
