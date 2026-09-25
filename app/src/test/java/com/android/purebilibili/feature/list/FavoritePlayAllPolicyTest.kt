package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoritePlayAllPolicyTest {

    private fun video(bvid: String): VideoItem = VideoItem(bvid = bvid, title = bvid)

    @Test
    fun resolveFavoritePlayAllItems_prefersSelectedFolderInPagerMode() {
        val result = resolveFavoritePlayAllItems(
            mode = FavoriteContentMode.PAGER,
            baseItems = listOf(video("BV_base")),
            selectedFolderItems = listOf(video("BV_selected")),
            singleFolderItems = emptyList()
        )

        assertEquals(listOf("BV_selected"), result.map { it.bvid })
    }

    @Test
    fun resolveFavoritePlayAllItems_fallbacksToBaseWhenPagerFolderStillEmpty() {
        val result = resolveFavoritePlayAllItems(
            mode = FavoriteContentMode.PAGER,
            baseItems = listOf(video("BV_base")),
            selectedFolderItems = emptyList(),
            singleFolderItems = emptyList()
        )

        assertEquals(listOf("BV_base"), result.map { it.bvid })
    }

    @Test
    fun resolveFavoritePlayAllItems_usesSingleFolderItemsInSingleMode() {
        val result = resolveFavoritePlayAllItems(
            mode = FavoriteContentMode.SINGLE_FOLDER,
            baseItems = listOf(video("BV_base")),
            selectedFolderItems = emptyList(),
            singleFolderItems = listOf(video("BV_single"))
        )

        assertEquals(listOf("BV_single"), result.map { it.bvid })
    }

    @Test
    fun resolveFavoritePlayAllItems_filtersOutCollectionResources() {
        val result = resolveFavoritePlayAllItems(
            mode = FavoriteContentMode.PAGER,
            baseItems = emptyList(),
            selectedFolderItems = listOf(
                VideoItem(title = "合集", isCollectionResource = true, collectionId = 1L),
                video("BV_selected")
            ),
            singleFolderItems = emptyList()
        )

        assertEquals(listOf("BV_selected"), result.map { it.bvid })
    }
}
