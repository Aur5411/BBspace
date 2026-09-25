// 文件路径: feature/anime/AnimeSettingsScreen.kt
//
// 「番剧设置」页。
//
// ★★ 用户明确要求 ★★
//   番剧是软件内单独请求的页面，播放器与弹幕都是自己实现的，
//   不经过哔哩哔哩、也不受主 App 设置影响。
//
// 因此本页读写的是 AniSettingsStore（独立 prefs 文件 "ani_settings"），
// 与 SettingsManager / 主 App 设置页 **零耦合**。
//
// 功能项参考 animeko 的设置，只保留「番剧页真的用得上」的四组：
//   播放 / 弹幕 / 内容 / 数据 —— 每一项都有对应消费方（见各项注释）。
package com.android.purebilibili.feature.anime

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.ImmersiveAppScaffold as AppScaffold
import com.android.purebilibili.core.ui.components.AppChoiceOption
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppPreference
import com.android.purebilibili.core.ui.components.AppPreferenceDivider
import com.android.purebilibili.core.ui.components.AppPreferenceGroup
import com.android.purebilibili.core.ui.components.AppPreferenceSectionTitle
import com.android.purebilibili.core.ui.components.AppSingleChoicePreference
import com.android.purebilibili.core.ui.components.AppSliderPreference
import com.android.purebilibili.core.ui.components.AppSwitchPreference
import com.android.purebilibili.core.ui.components.AppText

/**
 * 番剧设置页。
 *
 * @param onBack 返回
 */
@Composable
fun AnimeSettingsScreen(
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    // 番剧自有设置（不走 SettingsManager）
    var settings by remember { mutableStateOf(AniSettingsStore.current(context)) }
    var showResetConfirm by remember { mutableStateOf(false) }
    // 视频源管理面板(优先源 + 排序 + 测速)
    var showSourceManager by remember { mutableStateOf(false) }

    fun persist(transform: (AniSettings) -> AniSettings) {
        AniSettingsStore.update(context, transform)
        settings = AniSettingsStore.current(context)
    }

    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "番剧设置",
                subtitle = "仅作用于番剧页 · 与主设置互不影响",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    AppIconButton(onClick = { showResetConfirm = !showResetConfirm }) {
                        AppIcon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "恢复默认",
                        )
                    }
                },
            )
        },
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState()),
        ) {
            AppText(
                text = "番剧页使用 animeko 数据源与自建播放器，下面的选项只影响番剧页，" +
                    "不会改动首页视频、也不会被主 App 的设置覆盖。",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            if (showResetConfirm) {
                AppPreferenceSectionTitle("操作")
                AppPreferenceGroup {
                    AppPreference(
                        title = "恢复番剧默认设置",
                        subtitle = "把下面所有选项重置为出厂值（不影响追番收藏与历史）",
                        onClick = {
                            AniSettingsStore.reset(context)
                            settings = AniSettingsStore.current(context)
                            showResetConfirm = false
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // ─────────────────────── 数据源 ───────────────────────
            // 消费方: AnimeViewModel.loadMediaSources(优先源搜索)
            AppPreferenceSectionTitle("数据源")
            AppPreferenceGroup {
                AppPreference(
                    title = "视频源管理",
                    subtitle = "勾选优先使用的源并调整顺序，可一键测试各源速度",
                    onClick = { showSourceManager = true },
                )
            }
            Spacer(Modifier.height(8.dp))

            // ───────────────────────── 播放 ─────────────────────────
            // 消费方: AnimePlayerScreen / AnimeDanmakuOverlay
            AppPreferenceSectionTitle("播放")
            AppPreferenceGroup {
                AppSwitchPreference(
                    title = "自动连播下一话",
                    subtitle = "本话播放结束后自动切到下一话并继续播放",
                    checked = settings.autoPlayNextEpisode,
                    onCheckedChange = { v -> persist { it.copy(autoPlayNextEpisode = v) } },
                )
                AppPreferenceDivider()
                AppSwitchPreference(
                    title = "记住播放进度",
                    subtitle = "再次进入同一话时从上次位置继续播放",
                    checked = settings.rememberPlaybackPosition,
                    onCheckedChange = { v -> persist { it.copy(rememberPlaybackPosition = v) } },
                )
                AppPreferenceDivider()
                AppSingleChoicePreference(
                    title = "画面缩放",
                    subtitle = "播放器的默认填充方式，播放中点击画面也可切换",
                    selectedValue = settings.defaultResizeMode,
                    options = AniResizeMode.entries.map { mode ->
                        AppChoiceOption(
                            value = mode,
                            label = mode.label,
                            description = when (mode) {
                                AniResizeMode.FIT -> "完整显示画面，可能留黑边（默认）"
                                AniResizeMode.FILL -> "拉伸铺满，比例可能变形"
                                AniResizeMode.ZOOM -> "裁掉边缘铺满，不变形"
                            },
                        )
                    },
                    onValueChange = { mode -> persist { it.copy(defaultResizeMode = mode) } },
                )
                AppPreferenceDivider()
                AppSliderPreference(
                    title = "快进 / 快退步长",
                    subtitle = "双击播放器左右两侧时跳转的秒数",
                    value = settings.seekStepSeconds.toFloat(),
                    onValueChange = { v -> persist { it.copy(seekStepSeconds = v.toInt()) } },
                    valueRange = AniSettings.SEEK_STEP_RANGE.first.toFloat()..
                        AniSettings.SEEK_STEP_RANGE.last.toFloat(),
                    steps = AniSettings.SEEK_STEP_RANGE.last - AniSettings.SEEK_STEP_RANGE.first - 1,
                    valueLabel = "${settings.seekStepSeconds} 秒",
                )
                AppPreferenceDivider()
                AppSwitchPreference(
                    title = "优先硬件解码",
                    subtitle = "关闭后改用软件解码，某些片源兼容性更好",
                    checked = settings.hardwareDecodePreferred,
                    onCheckedChange = { v -> persist { it.copy(hardwareDecodePreferred = v) } },
                )
            }

            Spacer(Modifier.height(8.dp))

            // ───────────────────────── 弹幕 ─────────────────────────
            // 消费方: AnimeDanmakuOverlay
            AppPreferenceSectionTitle("弹幕")
            AppPreferenceGroup {
                AppSwitchPreference(
                    title = "默认开启弹幕",
                    subtitle = "进入播放器时自动打开弹幕",
                    checked = settings.danmakuEnabledByDefault,
                    onCheckedChange = { v -> persist { it.copy(danmakuEnabledByDefault = v) } },
                )
                AppPreferenceDivider()
                AppSliderPreference(
                    title = "弹幕不透明度",
                    subtitle = "数值越低越不遮挡画面",
                    value = settings.danmakuOpacityPercent.toFloat(),
                    onValueChange = { v -> persist { it.copy(danmakuOpacityPercent = v.toInt()) } },
                    valueRange = AniSettings.DANMAKU_OPACITY_RANGE.first.toFloat()..
                        AniSettings.DANMAKU_OPACITY_RANGE.last.toFloat(),
                    steps = 7,
                    valueLabel = "${settings.danmakuOpacityPercent}%",
                )
                AppPreferenceDivider()
                AppSliderPreference(
                    title = "弹幕滚动速度",
                    subtitle = "滚动弹幕每秒移动的像素，数值越大越快",
                    value = settings.danmakuSpeedPxPerSecond.toFloat(),
                    onValueChange = { v -> persist { it.copy(danmakuSpeedPxPerSecond = v.toInt()) } },
                    valueRange = AniSettings.DANMAKU_SPEED_RANGE.first.toFloat()..
                        AniSettings.DANMAKU_SPEED_RANGE.last.toFloat(),
                    steps = 7,
                    valueLabel = "${settings.danmakuSpeedPxPerSecond} px/s",
                )
                AppPreferenceDivider()
                AppSliderPreference(
                    title = "弹幕字号",
                    subtitle = "相对默认字号的百分比",
                    value = settings.danmakuTextScalePercent.toFloat(),
                    onValueChange = { v -> persist { it.copy(danmakuTextScalePercent = v.toInt()) } },
                    valueRange = AniSettings.DANMAKU_SCALE_RANGE.first.toFloat()..
                        AniSettings.DANMAKU_SCALE_RANGE.last.toFloat(),
                    steps = 9,
                    valueLabel = "${settings.danmakuTextScalePercent}%",
                )
                AppPreferenceDivider()
                AppSliderPreference(
                    title = "弹幕显示区域",
                    subtitle = "弹幕只占画面上方这么高的区域，避免挡住字幕",
                    value = settings.danmakuDisplayAreaPercent.toFloat(),
                    onValueChange = { v -> persist { it.copy(danmakuDisplayAreaPercent = v.toInt()) } },
                    valueRange = AniSettings.DANMAKU_AREA_RANGE.first.toFloat()..
                        AniSettings.DANMAKU_AREA_RANGE.last.toFloat(),
                    steps = 5,
                    valueLabel = "${settings.danmakuDisplayAreaPercent}%",
                )
                AppPreferenceDivider()
                AppSwitchPreference(
                    title = "屏蔽顶部弹幕",
                    subtitle = "隐藏固定在画面顶部的弹幕",
                    checked = settings.danmakuBlockTop,
                    onCheckedChange = { v -> persist { it.copy(danmakuBlockTop = v) } },
                )
                AppPreferenceDivider()
                AppSwitchPreference(
                    title = "屏蔽底部弹幕",
                    subtitle = "隐藏固定在画面底部的弹幕，避免挡字幕",
                    checked = settings.danmakuBlockBottom,
                    onCheckedChange = { v -> persist { it.copy(danmakuBlockBottom = v) } },
                )
            }

            Spacer(Modifier.height(8.dp))

            // ───────────────────────── 内容 ─────────────────────────
            // 消费方: AnimeViewModel(search/loadDetail) / AnimeDetailScreen
            AppPreferenceSectionTitle("内容")
            AppPreferenceGroup {
                AppSwitchPreference(
                    title = "详情页加载角色",
                    subtitle = "打开番剧详情时一并拉取角色与声优列表",
                    checked = settings.autoLoadCharacters,
                    onCheckedChange = { v -> persist { it.copy(autoLoadCharacters = v) } },
                )
                AppPreferenceDivider()
                AppSwitchPreference(
                    title = "显示制作人员",
                    subtitle = "详情页展示导演 / 脚本 / 作画监督等制作班底（数据来自 Bangumi）",
                    checked = settings.showStaff,
                    enabled = settings.autoLoadCharacters,
                    onCheckedChange = { v -> persist { it.copy(showStaff = v) } },
                )
                AppPreferenceDivider()
                AppSwitchPreference(
                    title = "显示短评",
                    subtitle = "详情页展示 Bangumi 与 animeko 的用户短评",
                    checked = settings.showReviews,
                    onCheckedChange = { v -> persist { it.copy(showReviews = v) } },
                )
                AppPreferenceDivider()
                AppSliderPreference(
                    title = "搜索结果数量",
                    subtitle = "每次搜索向 animeko 请求的条目上限",
                    value = settings.searchResultLimit.toFloat(),
                    onValueChange = { v -> persist { it.copy(searchResultLimit = v.toInt()) } },
                    valueRange = AniSettings.SEARCH_LIMIT_RANGE.first.toFloat()..
                        AniSettings.SEARCH_LIMIT_RANGE.last.toFloat(),
                    steps = (AniSettings.SEARCH_LIMIT_RANGE.last -
                        AniSettings.SEARCH_LIMIT_RANGE.first) / 5 - 1,
                    valueLabel = "${settings.searchResultLimit} 条",
                )
                AppPreferenceDivider()
                AppSwitchPreference(
                    title = "详情页封面高斯模糊",
                    subtitle = "详情页大图封面做模糊处理，观感更柔和",
                    checked = settings.coverBlurEnabled,
                    onCheckedChange = { v -> persist { it.copy(coverBlurEnabled = v) } },
                )
            }

            Spacer(Modifier.height(8.dp))

            // ───────────────────────── 数据 ─────────────────────────
            // 消费方: AnimeViewModel.recordPlayback / AnimeHistoryScreen
            AppPreferenceSectionTitle("数据")
            AppPreferenceGroup {
                AppSwitchPreference(
                    title = "记录追番历史",
                    subtitle = "播放时写入本地历史（只存本机，不上传）",
                    checked = settings.recordWatchHistory,
                    onCheckedChange = { v -> persist { it.copy(recordWatchHistory = v) } },
                )
                AppPreferenceDivider()
                AppSliderPreference(
                    title = "历史条数上限",
                    subtitle = "超出后自动清理最早的记录",
                    value = settings.historyMaxCount.toFloat(),
                    onValueChange = { v -> persist { it.copy(historyMaxCount = v.toInt()) } },
                    valueRange = AniSettings.HISTORY_MAX_RANGE.first.toFloat()..
                        AniSettings.HISTORY_MAX_RANGE.last.toFloat(),
                    steps = 8,
                    valueLabel = "${settings.historyMaxCount} 条",
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showSourceManager) {
        AniSourceManagerSheet(onDismiss = { showSourceManager = false })
    }
}
