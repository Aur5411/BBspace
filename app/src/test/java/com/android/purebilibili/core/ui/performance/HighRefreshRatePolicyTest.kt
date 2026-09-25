package com.android.purebilibili.core.ui.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 高刷新率档位与模式挑选的单测。
 *
 * 这些断言对应真实 ROM 上报的模式表形状：同一分辨率下 60/90/120 三档并存、
 * 或「高刷只在另一个分辨率上提供」这类坑。
 */
class HighRefreshRatePolicyTest {

    private fun mode(id: Int, hz: Float, w: Int = 1080, h: Int = 2400) =
        DisplayRefreshMode(modeId = id, refreshRate = hz, width = w, height = h)

    // ---------- 档位解析 ----------

    @Test
    fun target_fromStorage_unknownValueFallsBackToDefault() {
        assertEquals(HighRefreshRateTarget.MAX, HighRefreshRateTarget.fromStorage(null))
        assertEquals(HighRefreshRateTarget.MAX, HighRefreshRateTarget.fromStorage(""))
        assertEquals(HighRefreshRateTarget.MAX, HighRefreshRateTarget.fromStorage("999"))
    }

    @Test
    fun target_fromStorage_roundTripsEveryEntry() {
        HighRefreshRateTarget.entries.forEach { target ->
            assertEquals(target, HighRefreshRateTarget.fromStorage(target.storageValue))
        }
    }

    @Test
    fun target_systemDoesNotTakeEffect() {
        assertFalse(HighRefreshRateTarget.SYSTEM.takesEffect)
        assertTrue(HighRefreshRateTarget.HZ90.takesEffect)
        assertTrue(HighRefreshRateTarget.HZ120.takesEffect)
        assertTrue(HighRefreshRateTarget.MAX.takesEffect)
    }

    // ---------- 模式挑选 ----------

    @Test
    fun resolve_systemTargetNeverTouchesWindow() {
        val selected = resolveHighRefreshModeId(
            currentModeId = 1,
            supportedModes = listOf(mode(1, 60f), mode(2, 120f)),
            target = HighRefreshRateTarget.SYSTEM,
        )
        assertNull(selected)
    }

    @Test
    fun resolve_explicit120_picksExactMode() {
        val selected = resolveHighRefreshModeId(
            currentModeId = 1,
            supportedModes = listOf(mode(1, 60f), mode(2, 90f), mode(3, 120f)),
            target = HighRefreshRateTarget.HZ120,
        )
        assertEquals(3, selected)
    }

    @Test
    fun resolve_explicit90_picks90Not120() {
        // 关键：明确选 90 时不应偷偷给 120，档位必须尊重用户选择。
        val selected = resolveHighRefreshModeId(
            currentModeId = 1,
            supportedModes = listOf(mode(1, 60f), mode(2, 90f), mode(3, 120f)),
            target = HighRefreshRateTarget.HZ90,
        )
        assertEquals(2, selected)
    }

    @Test
    fun resolve_120RequestedOn90OnlyDevice_degradesToBestAvailable() {
        // 设备只有 90Hz：选 120 不应摆烂回 60，而是给到它最好的那一档。
        val selected = resolveHighRefreshModeId(
            currentModeId = 1,
            supportedModes = listOf(mode(1, 60f), mode(2, 90f)),
            target = HighRefreshRateTarget.HZ120,
        )
        assertEquals(2, selected)
    }

    @Test
    fun resolve_maxTarget_picksHighestAvailable() {
        val selected = resolveHighRefreshModeId(
            currentModeId = 1,
            supportedModes = listOf(mode(1, 60f), mode(2, 90f), mode(3, 120f)),
            target = HighRefreshRateTarget.MAX,
        )
        assertEquals(3, selected)
    }

    @Test
    fun resolve_doesNotSwitchResolution() {
        // 高刷只在另一个分辨率上提供时，绝不能切分辨率（会导致缩放模糊/重排版）。
        val selected = resolveHighRefreshModeId(
            currentModeId = 1,
            supportedModes = listOf(
                mode(1, 60f, w = 960, h = 2142),
                mode(2, 120f, w = 1280, h = 2856),
            ),
            target = HighRefreshRateTarget.MAX,
        )
        assertNull(selected)
    }

    @Test
    fun resolve_noEligibleHighRefresh_returnsNull() {
        val selected = resolveHighRefreshModeId(
            currentModeId = 1,
            supportedModes = listOf(mode(1, 60f), mode(2, 75f)),
            target = HighRefreshRateTarget.MAX,
        )
        assertNull(selected)
    }

    @Test
    fun resolve_emptyModeList_returnsNull() {
        assertNull(
            resolveHighRefreshModeId(
                currentModeId = 1,
                supportedModes = emptyList(),
                target = HighRefreshRateTarget.MAX,
            )
        )
    }

    @Test
    fun resolve_vendorEpsilonOnRequestedRate_isTolerated() {
        // 厂商常上报 119.99 / 120.00001，容差内应视作命中。
        val selected = resolveHighRefreshModeId(
            currentModeId = 1,
            supportedModes = listOf(mode(1, 60f), mode(2, 119.99f)),
            target = HighRefreshRateTarget.HZ120,
        )
        assertEquals(2, selected)
    }

    @Test
    fun resolve_unknownCurrentMode_stillSelectsHighestHighRefresh() {
        // 当前 modeId 不在表里（部分 ROM 的虚拟显示）时，仍应尽力给出最高档。
        val selected = resolveHighRefreshModeId(
            currentModeId = 999,
            supportedModes = listOf(mode(1, 60f), mode(2, 120f)),
            target = HighRefreshRateTarget.MAX,
        )
        assertEquals(2, selected)
    }

    @Test
    fun resolve_currentRefreshRate_readsTable() {
        val modes = listOf(mode(1, 60f), mode(2, 120f))
        assertEquals(60f, resolveCurrentDisplayRefreshRate(1, modes))
        assertEquals(120f, resolveCurrentDisplayRefreshRate(2, modes))
        assertNull(resolveCurrentDisplayRefreshRate(999, modes))
    }

    // ---------- 与视频页旧签名的兼容性 ----------

    @Test
    fun videoPageFacade_matchesLegacyBehaviour() {
        // 旧视频页行为 = 「最高可用」档位；迁移后语义必须完全一致。
        val modes = listOf(mode(1, 60f, w = 2400, h = 1080), mode(3, 120f, w = 2400, h = 1080))
        val viaNewPolicy = resolveHighRefreshModeId(
            currentModeId = 1,
            supportedModes = modes,
            target = HighRefreshRateTarget.MAX,
        )
        assertEquals(3, viaNewPolicy)
    }
}
