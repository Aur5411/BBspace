package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicUserPaginationRegistryTest {

    @Test
    fun `pagination state is isolated per user`() {
        val registry = DynamicUserPaginationRegistry()
        registry.update(hostMid = 1001L, offset = "A1", hasMore = true)
        registry.update(hostMid = 1002L, offset = "B1", hasMore = false)

        assertEquals("A1", registry.offset(1001L))
        assertTrue(registry.hasMore(1001L))
        assertEquals("B1", registry.offset(1002L))
        assertEquals(false, registry.hasMore(1002L))
    }

    @Test
    fun `reset only affects target user`() {
        val registry = DynamicUserPaginationRegistry()
        registry.update(hostMid = 1001L, offset = "A1", hasMore = false)
        registry.update(hostMid = 1002L, offset = "B1", hasMore = false)

        registry.reset(1001L)

        assertEquals("", registry.offset(1001L))
        assertTrue(registry.hasMore(1001L))
        assertEquals("B1", registry.offset(1002L))
        assertEquals(false, registry.hasMore(1002L))
    }

    @Test
    fun `unknown user defaults to first page state`() {
        val registry = DynamicUserPaginationRegistry()

        assertEquals("", registry.offset(9999L))
        assertTrue(registry.hasMore(9999L))
    }
}
