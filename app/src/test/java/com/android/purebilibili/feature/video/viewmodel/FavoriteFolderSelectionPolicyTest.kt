package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.feature.video.policy.resolveFavoriteFolderMediaId
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteFolderSelectionPolicyTest {

    @Test
    fun `should compute add and remove folders when selection changed`() {
        val mutation = resolveFavoriteFolderMutation(
            original = setOf(10L, 20L),
            selected = setOf(20L, 30L)
        )

        assertEquals(setOf(30L), mutation.addFolderIds)
        assertEquals(setOf(10L), mutation.removeFolderIds)
    }

    @Test
    fun `should produce empty mutation when selection unchanged`() {
        val mutation = resolveFavoriteFolderMutation(
            original = setOf(100L, 200L),
            selected = setOf(100L, 200L)
        )

        assertEquals(emptySet(), mutation.addFolderIds)
        assertEquals(emptySet(), mutation.removeFolderIds)
    }

    @Test
    fun `should support removing all folders`() {
        val mutation = resolveFavoriteFolderMutation(
            original = setOf(1L, 2L),
            selected = emptySet()
        )

        assertEquals(emptySet(), mutation.addFolderIds)
        assertEquals(setOf(1L, 2L), mutation.removeFolderIds)
    }

    @Test
    fun `should prefer id when resolving folder media id`() {
        val folder = FavFolder(id = 100L, fid = 200L)

        assertEquals(100L, resolveFavoriteFolderMediaId(folder))
    }

    @Test
    fun `should fallback to fid when id is invalid`() {
        val folder = FavFolder(id = 321L, fid = 0L)

        assertEquals(321L, resolveFavoriteFolderMediaId(folder.copy(id = 0L, fid = 321L)))
    }
}
