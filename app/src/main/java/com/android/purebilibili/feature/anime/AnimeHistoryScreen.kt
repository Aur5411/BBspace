// 文件路径: feature/anime/AnimeHistoryScreen.kt
//
// 「追番历史」页 —— 数据全部来自本地 Room (AniWatchHistory), 不上报服务端。
package com.android.purebilibili.feature.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.android.purebilibili.core.database.entity.AniWatchHistory
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.ImmersiveAppScaffold as AppScaffold
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText

@Composable
fun AnimeHistoryScreen(
    onBack: () -> Unit,
    onSubjectClick: (Long, Long) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: AnimeViewModel = viewModel(),
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme

    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "追番历史",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        AppIconButton(onClick = { viewModel.clearHistory() }) {
                            AppIcon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = "清空历史",
                            )
                        }
                    }
                    // 番剧自有设置入口
                    AppIconButton(onClick = onSettingsClick) {
                        AppIcon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "番剧设置",
                        )
                    }
                },
            )
        },
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                AniEmptyState(text = "还没有观看记录", hint = "看过的番剧会出现在这里")
            }
            return@AppScaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + AppSpacingTokens.Small,
                bottom = AppSpacingTokens.Large,
                start = AppSpacingTokens.Medium,
                end = AppSpacingTokens.Medium,
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
        ) {
            items(history, key = { it.id }) { item ->
                AniHistoryRow(
                    item = item,
                    onClick = { onSubjectClick(item.subjectId, item.episodeId) },
                    onDelete = { viewModel.deleteHistoryEpisode(item.episodeId) },
                )
            }
        }
    }
}

@Composable
private fun AniHistoryRow(
    item: AniWatchHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val progress = if (item.durationMillis > 0L) {
        (item.positionMillis.toFloat() / item.durationMillis).coerceIn(0f, 1f)
    } else {
        0f
    }
    AppSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(AppSpacingTokens.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .aspectRatio(ANI_COVER_RATIO)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.surfaceVariant),
            ) {
                if (item.cover.isNotBlank()) {
                    AsyncImage(
                        model = item.cover,
                        contentDescription = item.subjectName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.width(AppSpacingTokens.Small))
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = item.subjectName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                val epText = buildString {
                    append("第").append(item.episodeSort).append("话")
                    if (item.episodeName.isNotBlank()) {
                        append(" · ").append(item.episodeName)
                    }
                }
                AppText(
                    text = epText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (progress > 0f) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colorScheme.surfaceVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(3.dp)
                                .background(colorScheme.primary),
                        )
                    }
                }
            }
            Spacer(Modifier.width(AppSpacingTokens.Small))
            AppIconButton(onClick = onDelete) {
                AppIcon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "删除这条记录",
                    tint = colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
