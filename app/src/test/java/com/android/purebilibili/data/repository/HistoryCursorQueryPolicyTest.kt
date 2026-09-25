package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryCursorQueryPolicyTest {

    @Test
    fun `first page should omit cursor params`() {
        val query = resolveHistoryCursorQuery(
            max = 0L,
            viewAt = 0L,
            business = ""
        )

        assertNull(query.max)
        assertNull(query.viewAt)
        assertNull(query.business)
    }

    @Test
    fun `next page should carry cursor max viewAt and business`() {
        val query = resolveHistoryCursorQuery(
            max = 12345L,
            viewAt = 1772011113L,
            business = "archive"
        )

        assertEquals(12345L, query.max)
        assertEquals(1772011113L, query.viewAt)
        assertEquals("archive", query.business)
    }

    @Test
    fun `blank business should be normalized to null`() {
        val query = resolveHistoryCursorQuery(
            max = 12345L,
            viewAt = 1772011113L,
            business = "   "
        )

        assertEquals(12345L, query.max)
        assertEquals(1772011113L, query.viewAt)
        assertNull(query.business)
    }
}
