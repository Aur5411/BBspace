// 文件路径: feature/anime/AnimeDanmakuOverlay.kt
//
// 追番播放器的弹幕渲染层 (独立实现, 不复用首页视频播放器的弹幕栈)。
//
// 数据来自 animeko: GET /v1/danmaku/{episodeId}
//   { "danmakuList": [ { "id", "senderId",
//       "danmakuInfo": { "playTime": 231480, "color": -1, "text": "...", "location": "NORMAL" } } ] }
//   playTime 单位是「毫秒」, color 是 ARGB 十进制 (-1 = 默认白), location = NORMAL/TOP/BOTTOM。
//
// ★ 本文件的全部可调参数都来自番剧自有设置 AniSettingsStore（经 AnimePlayerScreen 传入）:
//     textSizePx        <- AniSettings.danmakuTextPx         (字号百分比)
//     alpha             <- AniSettings.danmakuAlpha          (不透明度百分比)
//     speedPxPerSecond  <- AniSettings.danmakuSpeedPxPerSecond(滚动速度)
//     displayAreaPercent<- AniSettings.danmakuDisplayAreaPercent(弹幕显示区域)
//     blockTop/blockBottom <- AniSettings.danmakuBlockTop / danmakuBlockBottom
//   不读取 SettingsManager，也不受主 App 弹幕设置影响。
package com.android.purebilibili.feature.anime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.android.purebilibili.data.model.animeko.AniDanmakuItem
import com.android.purebilibili.data.model.animeko.AniDanmakuLocation

/** 一条弹幕在轨道上的落位。 */
private data class DanmakuLane(
    val text: String,
    val color: Color,
    val location: AniDanmakuLocation,
    val startMillis: Long,
    val widthPx: Float,
    /** 该条弹幕从出现到消失的持续时长(ms)。滚动弹幕与速度/宽度有关。 */
    val durationMillis: Long,
    /** 顶部/底部弹幕的行号; 滚动弹幕为分配到的轨道号。 */
    val lane: Int,
)

/** 顶部/底部固定弹幕的停留时长。 */
private const val PINNED_DURATION_MILLIS = 4_000L

/** 顶部 / 底部各自最多占用的行数上限。 */
private const val MAX_PINNED_LANES = 4

/** 轨道分配时的最大候选轨道数(实际使用数会被画面高度裁剪)。 */
private const val MAX_LANE_CANDIDATES = 64

/**
 * 弹幕渲染层。
 *
 * 自绘 Canvas 实现, 支持三种位置: 滚动 (NORMAL) / 顶部固定 (TOP) / 底部固定 (BOTTOM)。
 *
 * @param items 全部弹幕 (已按 playTime 升序时性能最好, 未排序也可)
 * @param positionMillisProvider 播放进度提供者, 每帧调用
 * @param enabled 弹幕开关
 * @param textSizePx 弹幕字号 (px)
 * @param alpha 弹幕不透明度 0..1
 * @param speedPxPerSecond 滚动弹幕每秒移动像素
 * @param displayAreaPercent 弹幕可用高度占画面高度的百分比 (40..100)
 * @param blockTop 屏蔽顶部固定弹幕
 * @param blockBottom 屏蔽底部固定弹幕
 */
@Composable
fun AnimeDanmakuOverlay(
    items: List<AniDanmakuItem>,
    positionMillisProvider: () -> Long,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textSizePx: Float = 40f,
    alpha: Float = 1f,
    speedPxPerSecond: Int = 180,
    displayAreaPercent: Int = 100,
    blockTop: Boolean = false,
    blockBottom: Boolean = false,
) {
    if (!enabled || items.isEmpty()) return

    val measurer: TextMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = remember(textSizePx, density) {
        TextStyle(
            fontSize = (textSizePx / density.density).sp,
            fontWeight = FontWeight.Medium,
        )
    }

    // 每帧推进的时钟 (以毫秒计)
    var frameMillis by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                frameMillis = positionMillisProvider().toFloat()
            }
        }
    }

    val safeSpeed = speedPxPerSecond.coerceAtLeast(1).toFloat()
    val areaRatio = (displayAreaPercent.coerceIn(20, 100)) / 100f

    BoxWithConstraints(modifier = modifier) {
        val canvasWidthPx = constraints.maxWidth.toFloat()
        val canvasHeightPx = constraints.maxHeight.toFloat()

        // 预排: 只按文本测一次宽度, 避免每帧重新测量。
        // 滚动时长 = (画面宽度 + 弹幕宽度) / 速度, 因此需要拿到实际画布宽度。
        val lanes = remember(
            items,
            style,
            canvasWidthPx,
            safeSpeed,
            blockTop,
            blockBottom,
        ) {
            buildLanes(
                items = items,
                measurer = measurer,
                style = style,
                canvasWidthPx = canvasWidthPx,
                speedPxPerSecond = safeSpeed,
                blockTop = blockTop,
                blockBottom = blockBottom,
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            // 弹幕只在画面上方 areaRatio 的高度内活动
            val activeHeight = (h * areaRatio).coerceAtLeast(textSizePx * 1.35f)
            val baseLineHeight = textSizePx * 1.35f

            val maxPinnedLanes = MAX_PINNED_LANES
                .coerceAtMost((activeHeight / baseLineHeight / 2f).toInt())
                .coerceAtLeast(1)
            val maxScrollLanes = ((activeHeight - baseLineHeight * maxPinnedLanes * 2) / baseLineHeight)
                .toInt()
                .coerceIn(1, MAX_LANE_CANDIDATES)

            val now = frameMillis

            lanes.forEach { lane ->
                val active = when (lane.location) {
                    AniDanmakuLocation.TOP, AniDanmakuLocation.BOTTOM ->
                        now >= lane.startMillis && now < lane.startMillis + lane.durationMillis
                    AniDanmakuLocation.NORMAL ->
                        now >= lane.startMillis && now < lane.startMillis + lane.durationMillis
                }
                if (!active) return@forEach

                val progress = ((now - lane.startMillis) / lane.durationMillis.toFloat())
                    .coerceIn(0f, 1f)

                val x: Float
                val y: Float
                when (lane.location) {
                    AniDanmakuLocation.NORMAL -> {
                        // 从右边界外进入, 到左边界外离开
                        val travel = w + lane.widthPx
                        x = w - progress * travel
                        val laneIdx = lane.lane.coerceIn(0, maxScrollLanes - 1)
                        y = baseLineHeight * (laneIdx + 0.5f)
                    }
                    AniDanmakuLocation.TOP -> {
                        x = (w - lane.widthPx) / 2f
                        val laneIdx = lane.lane.coerceIn(0, maxPinnedLanes - 1)
                        y = baseLineHeight * (laneIdx + 0.5f)
                    }
                    AniDanmakuLocation.BOTTOM -> {
                        x = (w - lane.widthPx) / 2f
                        val laneIdx = lane.lane.coerceIn(0, maxPinnedLanes - 1)
                        y = activeHeight - baseLineHeight * (laneIdx + 0.5f)
                    }
                }

                // 描边, 保证浅色画面上也看得清
                val strokeColor = Color.Black.copy(alpha = 0.55f * alpha)
                listOf(-1f to 0f, 1f to 0f, 0f to -1f, 0f to 1f).forEach { (dx, dy) ->
                    drawText(
                        textMeasurer = measurer,
                        text = lane.text,
                        style = style.copy(color = strokeColor),
                        topLeft = Offset(x + dx, y + dy),
                    )
                }
                drawText(
                    textMeasurer = measurer,
                    text = lane.text,
                    style = style.copy(color = lane.color.copy(alpha = alpha)),
                    topLeft = Offset(x, y),
                )
            }
        }
    }
}

/** 预排弹幕: 测量文本宽度并分配轨道。 */
private fun buildLanes(
    items: List<AniDanmakuItem>,
    measurer: TextMeasurer,
    style: TextStyle,
    canvasWidthPx: Float,
    speedPxPerSecond: Float,
    blockTop: Boolean,
    blockBottom: Boolean,
): List<DanmakuLane> {
    val sorted = items
        .filter { it.text.isNotBlank() }
        .sortedBy { it.playTimeMillis }

    val result = ArrayList<DanmakuLane>(sorted.size)
    // 记录每条轨道上最后一条弹幕的「占用结束时间」, 用于简易避让
    val scrollLaneEnd = HashMap<Int, Long>()
    val topLaneEnd = HashMap<Int, Long>()
    val bottomLaneEnd = HashMap<Int, Long>()
    val maxLanes = MAX_LANE_CANDIDATES

    var index = 0
    sorted.forEach { item ->
        val location = AniDanmakuLocation.from(item.location)
        // 屏蔽顶部 / 底部弹幕
        if (location == AniDanmakuLocation.TOP && blockTop) return@forEach
        if (location == AniDanmakuLocation.BOTTOM && blockBottom) return@forEach

        val measured = measurer.measure(
            text = item.text,
            style = style,
        )
        val widthPx = measured.size.width.toFloat()
        val start = item.playTimeMillis
        val currentIndex = index++

        // 滚动时长: 走完 (画面宽 + 自身宽) 所需时间
        val scrollDuration = (
            (canvasWidthPx + widthPx).coerceAtLeast(1f) / speedPxPerSecond * 1000f
            ).toLong().coerceAtLeast(1_000L)

        val duration: Long
        val lane = when (location) {
            AniDanmakuLocation.NORMAL -> {
                var idx = -1
                for (i in 0 until maxLanes) {
                    val end = scrollLaneEnd[i] ?: 0L
                    // 轨道空出一小段即可复用
                    if (start >= end - scrollDuration / 2) {
                        idx = i
                        break
                    }
                }
                if (idx < 0) idx = currentIndex % maxLanes
                // 该弹幕在这条轨道上会占到 start + 一个滚动周期
                val prev = scrollLaneEnd[idx] ?: 0L
                scrollLaneEnd[idx] = maxOf(prev, start + scrollDuration)
                duration = scrollDuration
                idx
            }
            AniDanmakuLocation.TOP -> {
                var idx = 0
                while (idx < MAX_PINNED_LANES && (topLaneEnd[idx] ?: 0L) > start) idx++
                if (idx >= MAX_PINNED_LANES) idx = 0
                topLaneEnd[idx] = start + PINNED_DURATION_MILLIS
                duration = PINNED_DURATION_MILLIS
                idx
            }
            AniDanmakuLocation.BOTTOM -> {
                var idx = 0
                while (idx < MAX_PINNED_LANES && (bottomLaneEnd[idx] ?: 0L) > start) idx++
                if (idx >= MAX_PINNED_LANES) idx = 0
                bottomLaneEnd[idx] = start + PINNED_DURATION_MILLIS
                duration = PINNED_DURATION_MILLIS
                idx
            }
        }

        // color: ARGB 十进制; -1 与 0 都按默认白处理
        val argb = item.color
        val color = if (argb == -1 || argb == 0) Color.White else Color(argb)

        result += DanmakuLane(
            text = item.text,
            color = color,
            location = location,
            startMillis = start,
            widthPx = widthPx,
            durationMillis = duration,
            lane = lane,
        )
    }
    return result
}
