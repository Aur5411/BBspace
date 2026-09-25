package com.android.purebilibili.feature.video.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoriteSaveUiPolicyTest {

    @Test
    fun `saving non-empty selection should mark video favorited and increment count when previously empty`() {
        val result = resolveFavoriteSaveUiState(
            originalFolderIds = emptySet(),
            selectedFolderIds = setOf(10L),
            currentFavoriteCount = 8
        )

        assertTrue(result.isFavorited)
        assertEquals(9, result.favoriteCount)
        assertTrue(result.membershipChanged)
    }

    @Test
    fun `saving empty selection should clear favorite state and decrement count when previously favorited`() {
        val result = resolveFavoriteSaveUiState(
            originalFolderIds = setOf(10L, 20L),
            selectedFolderIds = emptySet(),
            currentFavoriteCount = 8
        )

        assertFalse(result.isFavorited)
        assertEquals(7, result.favoriteCount)
        assertTrue(result.membershipChanged)
    }

    @Test
    fun `switching between non-empty selections should keep favorite state and count`() {
        val result = resolveFavoriteSaveUiState(
            originalFolderIds = setOf(10L),
            selectedFolderIds = setOf(20L),
            currentFavoriteCount = 8
        )

        assertTrue(result.isFavorited)
        assertEquals(8, result.favoriteCount)
        assertFalse(result.membershipChanged)
    }

    @Test
    fun `favorite count should never go below zero when clearing last folder`() {
        val result = resolveFavoriteSaveUiState(
            originalFolderIds = setOf(10L),
            selectedFolderIds = emptySet(),
            currentFavoriteCount = 0
        )

        assertFalse(result.isFavorited)
        assertEquals(0, result.favoriteCount)
    }
}
