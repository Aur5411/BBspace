package com.android.purebilibili.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectionUtilsTest {

    @Test
    fun `findClosestTarget prefers highest value not greater than target`() {
        val selected = listOf(120, 116, 80, 64).findClosestTarget(
            target = 100,
            fallback = ClosestTargetFallback.NEAREST_HIGHER
        )

        assertEquals(80, selected)
    }

    @Test
    fun `findClosestTarget uses nearest higher when no lower candidate exists`() {
        val selected = listOf(120, 116, 80, 64).findClosestTarget(
            target = 60,
            fallback = ClosestTargetFallback.NEAREST_HIGHER
        )

        assertEquals(64, selected)
    }

    @Test
    fun `findClosestTarget returns null for empty list`() {
        val selected = emptyList<Int>().findClosestTarget(
            target = 80,
            fallback = ClosestTargetFallback.NEAREST_HIGHER
        )

        assertNull(selected)
    }
}
