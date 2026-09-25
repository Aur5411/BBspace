// 文件路径: feature/anime/AniSourceSheet.kt
//
// 「换源」面板 —— 对应 animeko 的 EpisodeVideoMediaSelectorSideSheet / MediaSelectorItem。
//
// 版式照搬 animeko:
//   每张候选卡 = 标题 (originalTitle) + 一排 chips (文件大小 / 分辨率 / 字幕组 / 做种数)
//   选中项高亮; 点击即切换播放源。
package com.android.purebilibili.feature.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.data.model.animeko.AniMediaCandidate

/**
 * 换源面板内容 (由调用方放进 ModalBottomSheet / 侧边栏)。
 *
 * @param candidates 候选资源
 * @param sourceStatus 各源执行状态
 * @param selectedUrl 当前正在播放的地址, 用于高亮
 * @param loading 是否加载中
 * @param onSelect 选中某个候选
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AniSourceSheetContent(
    candidates: List<AniMediaCandidate>,
    sourceStatus: List<com.android.purebilibili.data.repository.AniSourceStatus>,
    selectedUrl: String?,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onSelect: (AniMediaCandidate) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth()) {
        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        text = "正在检索可用源…",
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            candidates.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AppText(
                            text = "没有找到可用源",
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                        // 展示每个源的状态, 便于排查
                        sourceStatus.forEach { st ->
                            AppText(
                                text = if (st.ok) {
                                    "${st.sourceName}: 0 条"
                                } else {
                                    "${st.sourceName}: ${st.message}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = AppSpacingTokens.Medium,
                        end = AppSpacingTokens.Medium,
                        bottom = AppSpacingTokens.Large,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                ) {
                    item {
                        AppText(
                            text = "共 ${candidates.size} 个源",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    items(candidates, key = { it.sourceId + "|" + it.url }) { item ->
                        key(item.url) {
                            AniSourceCandidateCard(
                                item = item,
                                selected = selectedUrl != null && item.url == selectedUrl,
                                onClick = { onSelect(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 单个候选资源卡 —— 对应 animeko 的 MediaSelectorItemLayout。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AniSourceCandidateCard(
    item: AniMediaCandidate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val borderColor = if (selected) colorScheme.primary else colorScheme.outlineVariant
    AppSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            colorScheme.surfaceContainerLow
        },
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = borderColor,
        ),
    ) {
        Column(modifier = Modifier.padding(AppSpacingTokens.Small)) {
            AppText(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))

            // 与 animeko 一致的 chips 排布: 大小 -> 分辨率 -> 字幕组 -> 做种数
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (item.sizeBytes > 0L) {
                    AniSourceChip(text = item.sizeLabel)
                }
                if (item.quality.isNotBlank()) {
                    AniSourceChip(text = item.quality)
                }
                if (item.subtitleGroup.isNotBlank()) {
                    AniSourceChip(text = item.subtitleGroup)
                }
                if (item.seeders >= 0) {
                    AniSourceChip(text = "做种 ${item.seeders}")
                }
                AniSourceChip(text = item.sourceName)
            }

            if (selected) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(
                        text = "正在播放",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** 候选卡上的小 chip。 */
@Composable
private fun AniSourceChip(text: String) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .background(
                color = colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        AppText(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
