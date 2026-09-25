package com.android.purebilibili.feature.space

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 个人主页「左右滑动切换标签」策略测试。
 *
 * 覆盖：向左划 = 右边一个标签、向右划 = 左边一个标签、阈值不足不触发、边界不越界。
 */
class SpaceTabSwipePolicyTest {

    private val tabs = listOf(
        SpaceMainTab.HOME,
        SpaceMainTab.DYNAMIC,
        SpaceMainTab.CONTRIBUTION,
    )

    @Test
    fun `向左滑动切到右边一个标签`() {
        assertEquals(
            1,
            resolveSpaceTabSwipeTargetIndex(
                tabs = tabs,
                currentTab = SpaceMainTab.HOME,
                totalDragX = -200f,
                containerWidthPx = 1000f,
            )
        )
    }

    @Test
    fun `向右滑动切到左边一个标签`() {
        assertEquals(
            1,
            resolveSpaceTabSwipeTargetIndex(
                tabs = tabs,
                currentTab = SpaceMainTab.CONTRIBUTION,
                totalDragX = 200f,
                containerWidthPx = 1000f,
            )
        )
    }

    @Test
    fun `位移不足阈值时不切标签`() {
        assertNull(
            resolveSpaceTabSwipeTargetIndex(
                tabs = tabs,
                currentTab = SpaceMainTab.HOME,
                totalDragX = -100f,
                containerWidthPx = 1000f,
            )
        )
        // 恰好等于阈值仍不触发（要求「超过」）
        assertNull(
            resolveSpaceTabSwipeTargetIndex(
                tabs = tabs,
                currentTab = SpaceMainTab.HOME,
                totalDragX = -(1000f * SPACE_TAB_SWIPE_TRIGGER_RATIO),
                containerWidthPx = 1000f,
            )
        )
    }

    @Test
    fun `已在边界时不越界`() {
        assertNull(
            resolveSpaceTabSwipeTargetIndex(
                tabs = tabs,
                currentTab = SpaceMainTab.HOME,
                totalDragX = 400f,
                containerWidthPx = 1000f,
            )
        )
        assertNull(
            resolveSpaceTabSwipeTargetIndex(
                tabs = tabs,
                currentTab = SpaceMainTab.CONTRIBUTION,
                totalDragX = -400f,
                containerWidthPx = 1000f,
            )
        )
    }

    @Test
    fun `宽度未知或当前标签不在列表内时返回空`() {
        assertNull(
            resolveSpaceTabSwipeTargetIndex(
                tabs = tabs,
                currentTab = SpaceMainTab.HOME,
                totalDragX = -500f,
                containerWidthPx = 0f,
            )
        )
        assertNull(
            resolveSpaceTabSwipeTargetIndex(
                tabs = tabs,
                currentTab = SpaceMainTab.CHEESE,
                totalDragX = -500f,
                containerWidthPx = 1000f,
            )
        )
    }

    // ---------- 甩动（fling）判定：跟手 pager 手感 ----------

    @Test
    fun `快速甩动即使位移不足阈值也切标签`() {
        // 位移只有 10%（低于 18% 阈值），但甩得够快 —— 应当切页。
        assertEquals(
            1,
            resolveSpaceTabSwipeTargetIndex(
                tabs = tabs,
                currentTab = SpaceMainTab.HOME,
                totalDragX = -100f,
                containerWidthPx = 1000f,
                velocityX = -1500f,
            )
        )
    }

    @Test
    fun `慢速小幅拖拽不切标签`() {
        assertNull(
            resolveSpaceTabSwipeTargetIndex(
                tabs = tabs,
                currentTab = SpaceMainTab.HOME,
                totalDragX = -100f,
                containerWidthPx = 1000f,
                velocityX = -300f,
            )
        )
    }

    @Test
    fun `急速抖动几乎没有位移时不切标签`() {
        // 原地快速抖一下（位移 1%）不应翻页，否则会疯狂误触。
        assertNull(
            resolveSpaceTabSwipeTargetIndex(
                tabs = tabs,
                currentTab = SpaceMainTab.HOME,
                totalDragX = -10f,
                containerWidthPx = 1000f,
                velocityX = -3000f,
            )
        )
    }

    @Test
    fun `甩动方向决定切页方向`() {
        assertEquals(
            1,
            resolveSpaceTabSwipeTargetIndex(
                tabs = tabs,
                currentTab = SpaceMainTab.CONTRIBUTION,
                totalDragX = 120f,
                containerWidthPx = 1000f,
                velocityX = 1600f,
            )
        )
    }

    @Test
    fun `带速度版本在边界处仍不越界`() {
        assertNull(
            resolveSpaceTabSwipeTargetIndex(
                tabs = tabs,
                currentTab = SpaceMainTab.HOME,
                totalDragX = 150f,
                containerWidthPx = 1000f,
                velocityX = 2000f,
            )
        )
    }

    @Test
    fun `空标签列表返回空`() {
        assertNull(
            resolveSpaceTabSwipeTargetIndex(
                tabs = emptyList(),
                currentTab = SpaceMainTab.HOME,
                totalDragX = -500f,
                containerWidthPx = 1000f,
                velocityX = -2000f,
            )
        )
    }
}
