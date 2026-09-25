package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActionRepositoryFollowGroupPolicyTest {

    @Test
    fun `chunkFollowGroupTargetMids should split large sets into fixed-size chunks`() {
        val targets = (1L..45L).toSet()

        val chunks = ActionRepository.chunkFollowGroupTargetMids(targets, chunkSize = 20)

        assertEquals(3, chunks.size)
        assertEquals(20, chunks[0].size)
        assertEquals(20, chunks[1].size)
        assertEquals(5, chunks[2].size)
        assertEquals(targets, chunks.flatten().toSet())
    }

    @Test
    fun `chunkFollowGroupTargetMids should drop invalid mids`() {
        val chunks = ActionRepository.chunkFollowGroupTargetMids(
            targetMids = setOf(-1L, 0L, 2L, 3L),
            chunkSize = 10
        )

        assertEquals(listOf(listOf(2L, 3L)), chunks)
    }

    @Test
    fun `isFollowGroupRetryableError should recognize risk-control responses`() {
        assertTrue(ActionRepository.isFollowGroupRetryableError(-412, "请求过于频繁"))
        assertTrue(ActionRepository.isFollowGroupRetryableError(22015, "触发风控"))
        assertTrue(ActionRepository.isFollowGroupRetryableError(1, "Too many requests"))
        assertFalse(ActionRepository.isFollowGroupRetryableError(-404, "内容不存在"))
    }
}

