// 文件路径: feature/anime/AniSourceManagerSheet.kt
//
// 番剧「视频源管理」面板。
//
// 三件事:
//   1. 勾选「优先使用」的源 —— 播放番剧时只先搜这些源, 命中即播(省掉全量搜索的等待);
//   2. 调整顺序 —— 列表顺序就是搜索优先级(上移/下移);
//   3. 测速 —— 逐个请求源站首页计时, 把快的排到前面。
//
// 只写番剧自己的设置(AniSourcePreference), 不碰主 App 设置。
package com.android.purebilibili.feature.anime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AniSourceManagerSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val allSources = remember { AniSourcePreference.allSources() }
    var preferred by remember { mutableStateOf(AniSourcePreference.getPreferred(context)) }
    var latencies by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var testing by remember { mutableStateOf(false) }

    fun persist(next: List<String>) {
        preferred = next
        AniSourcePreference.setPreferred(context, next)
    }

    fun testAll() {
        scope.launch {
            testing = true
            latencies = AniSourcePreference.measureAll(context)
            testing = false
        }
    }

    val preferredSources = preferred.mapNotNull { id -> allSources.firstOrNull { it.id == id } }
    val others = allSources.filter { it.id !in preferred }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacingTokens.Medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = "视频源管理",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    onClick = { if (!testing) testAll() },
                    enabled = !testing,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    AppIcon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.height(18.dp),
                    )
                    AppText(
                        text = if (testing) "测试中…" else "测试速度",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            AppText(
                text = "勾选优先使用的源，顺序即搜索优先级。播放番剧时先搜这些源，没结果再自动全量搜索。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
            ) {
                // 优先源(有序, 可上下移动)
                items(preferredSources.size, key = { preferredSources[it].id }) { index ->
                    val source = preferredSources[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Checkbox(
                            checked = true,
                            onCheckedChange = { checked ->
                                if (!checked) persist(preferred - source.id)
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            AppText(
                                text = "${index + 1}. ${source.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                            )
                            AppText(
                                text = "${source.kindLabel} · ${latencyLabel(latencies[source.id])}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AppIconButton(
                            onClick = {
                                if (index > 0) {
                                    val next = preferred.toMutableList().apply {
                                        removeAt(index)
                                        add(index - 1, source.id)
                                    }
                                    persist(next)
                                }
                            },
                            enabled = index > 0,
                        ) {
                            AppIcon(
                                imageVector = Icons.Outlined.ArrowUpward,
                                contentDescription = "上移",
                            )
                        }
                        AppIconButton(
                            onClick = {
                                if (index < preferred.size - 1) {
                                    val next = preferred.toMutableList().apply {
                                        removeAt(index)
                                        add(index + 1, source.id)
                                    }
                                    persist(next)
                                }
                            },
                            enabled = index < preferred.size - 1,
                        ) {
                            AppIcon(
                                imageVector = Icons.Outlined.ArrowDownward,
                                contentDescription = "下移",
                            )
                        }
                    }
                }

                // 其它源(可加入优先)
                items(others.size, key = { others[it].id }) { index ->
                    val source = others[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Checkbox(
                            checked = false,
                            onCheckedChange = { checked ->
                                if (checked) persist(preferred + source.id)
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            AppText(
                                text = source.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                            )
                            AppText(
                                text = "${source.kindLabel} · ${latencyLabel(latencies[source.id])}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item { SpacerBottom() }
            }
        }
    }
}

@Composable
private fun SpacerBottom() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
}

/** 延迟文案: 未测 / 毫秒 / 不可达。 */
private fun latencyLabel(ms: Long?): String = when {
    ms == null -> "未测速"
    ms < 0L -> "不可达"
    else -> "${ms}ms"
}
