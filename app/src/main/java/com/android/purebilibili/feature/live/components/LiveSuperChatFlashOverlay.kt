package com.android.purebilibili.feature.live.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.feature.live.LiveDanmakuItem
import com.android.purebilibili.feature.live.formatLiveSuperChatCountdown
import com.android.purebilibili.feature.live.resolveLiveSuperChatColor
import com.android.purebilibili.feature.live.resolveLiveSuperChatDurationSec
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow

/** SC 浮层最长展示时长（秒），防止异常数据长时间遮挡画面 */
private const val SUPER_CHAT_FLASH_MAX_DURATION_SEC = 30L

/**
 * 同屏最多展示的 SC 条数。
 * ★ 2026-10: 只显示「最贵 + 最新」两条 —— 有新的更贵/更新的 SC 就顶上替换。
 */
private const val MAX_VISIBLE_SUPER_CHATS = 2

/** 倒计时刷新间隔（毫秒）。500ms 足够平滑，又不会造成额外负担。 */
private const val TICK_INTERVAL_MS = 500L

/** 两条 SC 之间的间距(dp)，用于高度预算估算。 */
private const val SC_STACK_GAP_DP = 8

/** SC 卡片单行内容高度 + 头部高度(dp)，仅用于「能否放得下两条」的估算。 */
private const val SC_HEADER_DP = 32
private const val SC_CONTENT_LINE_DP = 22
private const val SC_CHARS_PER_LINE = 16

/** 估算一条 SC 卡片的高度(dp)：头部 + 内容行数(最多 3 行)。 */
private fun LiveDanmakuItem.estimatedCardHeightDp(): Int {
    val lines = ((text.length + SC_CHARS_PER_LINE - 1) / SC_CHARS_PER_LINE).coerceIn(1, 3)
    return SC_HEADER_DP + lines * SC_CONTENT_LINE_DP
}

/**
 * SC 顶部悬浮条（挂在弹幕区顶部）。
 *
 * 行为（★ 2026-10 按需求重做）：
 * 1. SC 出现时自动挂到弹幕区顶部，直到倒计时结束或手动关闭后消失。
 * 2. 同屏最多显示 [MAX_VISIBLE_SUPER_CHATS] 条：**最贵的一条 + 最新的一条**。
 *    有更新的 SC 到来时，从顶部自然滑入替换旧卡片。
 * 3. 倒计时从 SC 的**开始时刻**(superChatReportTs/reportTs) 计算，
 *    切后台或重进直播间都不会把倒计时刷新成满时长。
 */
@Composable
fun LiveSuperChatFlashOverlay(
    flashFlow: SharedFlow<LiveDanmakuItem>,
    modifier: Modifier = Modifier,
    onUserClick: (Long) -> Unit = {},
    /**
     * 可用高度上限(dp)，通常传「下方弹幕区域高度的一半」。
     * 两条 SC 的总高超过它时，只显示一条（保证被显示的那条完整可见）。
     */
    maxHeightDp: Int = Int.MAX_VALUE,
) {
    // 当前正在展示的 SC（最多两条：最贵 + 最新）
    val active = remember { mutableStateListOf<LiveDanmakuItem>() }
    // 每秒推进的时间基准，用于计算各条 SC 的剩余时间与过期清理
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 时间推进
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(TICK_INTERVAL_MS)
        }
    }

    // 新 SC 到达：去重 -> 追加(最新在末尾) -> 收敛为「最贵 + 最新」
    LaunchedEffect(flashFlow) {
        flashFlow.collect { item ->
            active.removeAll { it.isSameSuperChatAs(item) }
            active.add(item)
            active.convergeToBestAndNewest()
        }
    }

    // 过期清理：到期（或找不到开始时刻）的 SC 移除后再收敛一次
    LaunchedEffect(nowMs, active.size) {
        val expired = active.filter { it.remainingSecAt(nowMs) <= 0 }
        if (expired.isNotEmpty()) {
            active.removeAll(expired)
            active.convergeToBestAndNewest()
        }
    }

    val displayed = run {
        val all = active.toList()
        if (all.size < 2) {
            all
        } else {
            // ★ 两条放得下(总高 <= 弹幕区一半)才显示两条; 否则只留一条(最贵的那条),
            //   保证显示出来的 SC 完整可见、不被裁切。
            val two = all.take(MAX_VISIBLE_SUPER_CHATS)
            val totalHeight = two.sumOf { it.estimatedCardHeightDp() } + SC_STACK_GAP_DP
            if (totalHeight <= maxHeightDp) two else all.take(1)
        }
    }

    // ★ 2026-10: 不自己占满全屏 —— 由调用方放进「下方弹幕区域」容器, 自然贴在弹幕列表顶部。
    //   ★ 不做高度裁剪(heightIn): 宁可少显示一条, 也要保证被显示的 SC **完整可见**,
    //     所以约束只体现在上面的「条数」判断上, 而不是把卡片切掉一半。
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        // 用 AnimatedContent 做「整体替换」过渡：新列表从顶部滑入、旧列表滑出淡出，
        // 保证「有新的就顶上替换」的动作自然流畅。
        AnimatedContent(
            targetState = displayed,
            transitionSpec = {
                val enter = slideInVertically(tween(260)) { -it / 3 } + fadeIn(tween(260))
                val exit = slideOutVertically(tween(200)) { it / 3 } + fadeOut(tween(200))
                enter togetherWith exit
            },
            label = "liveSuperChatStack",
            modifier = Modifier.fillMaxWidth()
        ) { list ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                list.forEach { item ->
                    LiveSuperChatCard(
                        item = item,
                        remainingSec = item.remainingSecAt(nowMs),
                        onClose = { active.removeAll { it.isSameSuperChatAs(item) } },
                        onUserClick = onUserClick,
                    )
                }
            }
        }
    }
}

/** 单条 SC 悬浮卡片。 */
@Composable
private fun LiveSuperChatCard(
    item: LiveDanmakuItem,
    remainingSec: Int,
    onClose: () -> Unit,
    onUserClick: (Long) -> Unit,
) {
    val accent = resolveLiveSuperChatColor(item.superChatBackgroundColor)
    val shape = RoundedCornerShape(12.dp)

    AppSurface(
        shape = shape,
        color = accent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .shadow(elevation = 8.dp, shape = shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (item.uid > 0L) onUserClick(item.uid)
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 头部：用户名 + 价格 + 倒计时 + 关闭
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(accent, accent.copy(alpha = 0.85f))
                        )
                    )
                    .padding(
                        horizontal = AppSpacingTokens.Medium,
                        vertical = AppSpacingTokens.Small
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AppText(
                        text = item.uname.ifBlank { "醒目留言" },
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.superChatPrice.isNotBlank()) {
                        Spacer(Modifier.width(AppSpacingTokens.Small))
                        AppText(
                            text = item.superChatPrice,
                            color = Color.White.copy(alpha = 0.95f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall)
                ) {
                    AppText(
                        text = formatLiveSuperChatCountdown(remainingSec),
                        color = Color.White.copy(alpha = 0.80f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.24f))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "关闭",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // 内容
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent.copy(alpha = 0.70f))
                    .padding(
                        horizontal = AppSpacingTokens.Medium,
                        vertical = AppSpacingTokens.Small
                    )
            ) {
                AppText(
                    text = item.text,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------
// SC 列表策略
// ---------------------------------------------------------------

/** 判断两条 SC 是否为同一条（用于去重与移除）。 */
private fun LiveDanmakuItem.isSameSuperChatAs(other: LiveDanmakuItem): Boolean {
    if (superChatId != 0L && other.superChatId != 0L) return superChatId == other.superChatId
    if (superChatToken.isNotBlank() && other.superChatToken.isNotBlank()) {
        return superChatToken == other.superChatToken
    }
    return uid == other.uid && text == other.text && reportTs == other.reportTs
}

/** 价格字符串 -> 数值（"¥30" / "30元" / "1000" 均能解析）。 */
private fun LiveDanmakuItem.superChatPriceValue(): Long =
    Regex("\\d+").find(superChatPrice)?.value?.toLongOrNull() ?: 0L

/**
 * 剩余秒数。
 *
 * ★ 从 SC 的开始时刻计算：开始时刻取 superChatReportTs，退回 reportTs；
 *   两者都没有时才退化为「按满时长展示」。
 */
private fun LiveDanmakuItem.remainingSecAt(nowMs: Long): Int {
    val total = resolveLiveSuperChatDurationSec(superChatDuration)
        .coerceAtMost(SUPER_CHAT_FLASH_MAX_DURATION_SEC.toInt())
    val startTs = superChatReportTs.takeIf { it > 0L } ?: reportTs.takeIf { it > 0L }
    val elapsed = if (startTs != null) {
        ((nowMs - startTs) / 1000L).toInt().coerceAtLeast(0)
    } else {
        0
    }
    return (total - elapsed).coerceAtLeast(0)
}

/**
 * 收敛列表为「最贵 + 最新」两条（去重后不足则按价格补齐）。
 *
 * 列表约定：末尾元素为「最新到达」的一条。
 */
private fun MutableList<LiveDanmakuItem>.convergeToBestAndNewest() {
    if (size <= MAX_VISIBLE_SUPER_CHATS) return
    val newest = lastOrNull()
    val priciest = maxByOrNull { it.superChatPriceValue() }
    val byPriceDesc = sortedByDescending { it.superChatPriceValue() }

    val kept = LinkedHashSet<LiveDanmakuItem>()
    // 最贵的优先占位
    priciest?.let(kept::add)
    // 最新的占位
    newest?.let(kept::add)
    // 不足两条时按价格补齐
    for (entry in byPriceDesc) {
        if (kept.size >= MAX_VISIBLE_SUPER_CHATS) break
        kept.add(entry)
    }
    clear()
    addAll(kept.take(MAX_VISIBLE_SUPER_CHATS))
}
