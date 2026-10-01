package com.android.purebilibili.feature.settings
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable // [New]
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.android.purebilibili.R
import com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState
import com.android.purebilibili.core.util.CacheClearTarget
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.store.DEFAULT_ANALYTICS_ENABLED
import com.android.purebilibili.core.store.DEFAULT_CRASH_TRACKING_ENABLED
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.theme.LocalSettingsLiquidGlassEnabled
import com.android.purebilibili.core.ui.LocalBottomBarVisible
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.AppNavigationSettings
import com.android.purebilibili.core.util.AnalyticsHelper
import com.android.purebilibili.core.util.CacheUtils
import com.android.purebilibili.core.util.CrashReporter
import com.android.purebilibili.core.util.EasterEggs
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.LogCollector
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.core.ui.AdaptiveScaffold
import com.android.purebilibili.core.ui.AdaptiveTopAppBar
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.TopReadabilityChrome
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.plugin.PluginManager

import com.android.purebilibili.core.ui.blur.hazeSourceCompat
import kotlinx.coroutines.launch

import com.android.purebilibili.core.ui.components.AppPreferenceSectionTitle
import com.android.purebilibili.core.ui.animation.EntranceGroup
import com.android.purebilibili.core.ui.animation.entrance
import com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion
import com.android.purebilibili.feature.dynamic.defaultDynamicTabVisibleIds
import com.android.purebilibili.feature.dynamic.resolveDynamicVisibleTabIdsAfterToggle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.feature.settings.screen.CommentFraudHistoryScreen

const val GITHUB_URL = OFFICIAL_GITHUB_URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
    onOpenSourceLicensesClick: () -> Unit,
    onAppearanceClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onAnimationClick: () -> Unit = {},
    onPlaybackClick: () -> Unit = {},
    onPermissionClick: () -> Unit = {},
    onMessageNotificationClick: () -> Unit = {},
    onPluginsClick: () -> Unit = {},
    onSettingsShareClick: () -> Unit = {},
    onWebDavBackupClick: () -> Unit = {},
    onNavigateToBottomBarSettings: () -> Unit = {},
    onAnimeSettingsClick: () -> Unit = {},
    onCreditsClick: () -> Unit = {},
    onTipsClick: () -> Unit = {},
    onReplayOnboardingClick: () -> Unit = {},
    onCategoryClick: (SettingsRootCategory) -> Unit = {},
    onSearchOpen: () -> Unit = {},
    destination: SettingsNavDestination = SettingsNavDestination.Home,
    mainHazeState: dev.chrisbanes.haze.HazeState? = null,
    forceSinglePaneContent: Boolean = false,
    rootEntranceEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val navigationTransitionRunning =
        LocalAnimatedVisibilityScope.current?.transition?.isRunning == true
    val rootEntranceStartWhen = shouldStartSettingsEntrance(
        entranceEnabled = rootEntranceEnabled,
        navigationTransitionRunning = navigationTransitionRunning,
    )
    val versionClickThreshold = EasterEggs.VERSION_EASTER_EGG_THRESHOLD
    
    // State Collection
    val state by viewModel.state.collectAsStateWithLifecycle()
    val privacyModeEnabled by SettingsManager.getPrivacyModeEnabled(context).collectAsStateWithLifecycle(initialValue = false)
    val searchSuggestionsEnabled by SettingsManager.getSearchSuggestionsEnabled(context).collectAsStateWithLifecycle(initialValue = true)
    val privacyContentAuthenticationEnabled by SettingsManager
        .getPrivacyContentAuthenticationEnabled(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val crashTrackingEnabled by SettingsManager.getCrashTrackingEnabled(context)
        .collectAsStateWithLifecycle(initialValue = DEFAULT_CRASH_TRACKING_ENABLED)
    val analyticsEnabled by SettingsManager.getAnalyticsEnabled(context)
        .collectAsStateWithLifecycle(initialValue = DEFAULT_ANALYTICS_ENABLED)
    val enhancedDiagnosticLoggingEnabled by SettingsManager
        .getEnhancedDiagnosticLoggingEnabled(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val easterEggEnabled by SettingsManager.getEasterEggEnabled(context).collectAsStateWithLifecycle(initialValue = true)
    val customDownloadPath by SettingsManager.getDownloadPath(context).collectAsStateWithLifecycle(initialValue = null)
    val downloadExportTreeUri by SettingsManager.getDownloadExportTreeUri(context).collectAsStateWithLifecycle(initialValue = null)
    val imageSaveTreeUri by SettingsManager.getImageSaveTreeUri(context).collectAsStateWithLifecycle(initialValue = null)
    val autoCacheClearInterval by SettingsManager.getAutoCacheClearInterval(context)
        .collectAsStateWithLifecycle(initialValue = SettingsManager.AutoCacheClearInterval.NEVER)
    val autoCacheClearThresholdGb by SettingsManager.getAutoCacheClearThresholdGb(context)
        .collectAsStateWithLifecycle(
            initialValue = SettingsManager.DEFAULT_AUTO_CACHE_CLEAR_THRESHOLD_GB
        )
    val feedApiType by SettingsManager.getFeedApiType(context).collectAsStateWithLifecycle(initialValue = SettingsManager.FeedApiType.WEB
    )
    val incrementalTimelineRefreshEnabled by SettingsManager.getIncrementalTimelineRefresh(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val homeRefreshCount by SettingsManager.getHomeRefreshCount(context)
        .collectAsStateWithLifecycle(initialValue = com.android.purebilibili.core.store.DEFAULT_HOME_REFRESH_COUNT)
    val dynamicVisibleTabIds by SettingsManager.getDynamicTabVisibleTabs(context)
        .collectAsStateWithLifecycle(initialValue = defaultDynamicTabVisibleIds)
    val dynamicImagePreviewTextVisible by SettingsManager.getDynamicImagePreviewTextVisible(context)
        .collectAsStateWithLifecycle(initialValue = true)
    val dynamicAllTabHorizontalUserListVisible by SettingsManager
        .getDynamicAllTabHorizontalUserListVisible(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val dynamicTopBarCollapseOnScroll by SettingsManager
        .getDynamicTopBarCollapseOnScroll(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val dynamicFeedLayoutMode by SettingsManager
        .getDynamicFeedLayoutMode(context)
        .collectAsStateWithLifecycle(initialValue = com.android.purebilibili.core.store.SettingsManager.DynamicFeedLayoutMode.WATERFALL)
    
    // Local UI State
    var showCacheDialog by remember { mutableStateOf(false) }
    var showCacheAnimation by remember { mutableStateOf(false) }
    var cacheProgress by remember { mutableStateOf<CacheClearProgress?>(null) }
    val cacheClearOptions = remember { resolveCacheClearOptions() }
    var selectedCacheClearTargets by remember {
        mutableStateOf(resolveDefaultCacheClearTargets())
    }
    var pendingCacheClearTargets by remember {
        mutableStateOf(resolveDefaultCacheClearTargets())
    }
    val selectedCacheSizeSummary = remember(state.cacheBreakdown, selectedCacheClearTargets) {
        resolveSelectedCacheSizeSummary(
            breakdown = state.cacheBreakdown,
            selectedTargets = selectedCacheClearTargets
        )
    }
    var versionClickCount by remember { mutableIntStateOf(0) }
    var showEasterEggDialog by remember { mutableStateOf(false) }
    var showPathDialog by remember { mutableStateOf(false) }
    var showImageSavePathDialog by remember { mutableStateOf(false) }
    // [新增] 打赏对话框
    var showReleaseDisclaimerDialog by remember { mutableStateOf(false) }
    
    // [新增] 黑名单页面状态
    var showBlockedList by remember { mutableStateOf(false) }
    // [新增] 发评反诈页面状态
    var showCommentFraudHistory by remember { mutableStateOf(false) }

    // Haze State for this screen
    val activeHazeState = mainHazeState ?: rememberRecoverableHazeState()

    // Directory Picker - 使用文件系统 API
    val defaultPath = remember { SettingsManager.getDefaultDownloadPath(context) }
    val downloadFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        }

        scope.launch {
            SettingsManager.setDownloadExportTreeUri(context, uri.toString())
            SettingsManager.setDownloadPath(context, null)
        }
        Toast.makeText(context, "已设置导出目录", Toast.LENGTH_SHORT).show()
    }
    val imageSaveFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        }

        scope.launch {
            SettingsManager.setImageSaveTreeUri(context, uri.toString())
        }
        Toast.makeText(context, "已设置图片保存目录", Toast.LENGTH_SHORT).show()
    }

    // Callbacks
    val onClearCacheAction = {
        selectedCacheClearTargets = resolveDefaultCacheClearTargets()
        showCacheDialog = true
    }
    val onDownloadPathAction = { showPathDialog = true }
    val onImageSavePathAction = { showImageSavePathDialog = true }
    
    // Logic Callbacks
    val onSearchSuggestionsChange: (Boolean) -> Unit = { enabled ->
        scope.launch { SettingsManager.setSearchSuggestionsEnabled(context, enabled) }
    }
    val onPrivacyModeChange: (Boolean) -> Unit = { enabled ->
        scope.launch { SettingsManager.setPrivacyModeEnabled(context, enabled) }
    }
    val onPrivacyContentAuthenticationChange: (Boolean) -> Unit = { enabled ->
        scope.launch { SettingsManager.setPrivacyContentAuthenticationEnabled(context, enabled) }
    }
    val onCrashTrackingChange: (Boolean) -> Unit = { enabled ->
        scope.launch {
            SettingsManager.setCrashTrackingEnabled(context, enabled)
            CrashReporter.setEnabled(enabled)
            com.android.purebilibili.core.performance.Android17Diagnostics.updateEnabled(context, enabled)
        }
    }
    val onAnalyticsChange: (Boolean) -> Unit = { enabled ->
        scope.launch {
            SettingsManager.setAnalyticsEnabled(context, enabled)
            AnalyticsHelper.setEnabled(enabled)
        }
    }
    val onEnhancedDiagnosticLoggingChange: (Boolean) -> Unit = { enabled ->
        scope.launch {
            SettingsManager.setEnhancedDiagnosticLoggingEnabled(context, enabled)
            Logger.configureEnhancedDiagnosticLogging(context, enabled)
        }
    }
    val onEasterEggChange: (Boolean) -> Unit = { enabled ->
        scope.launch { SettingsManager.setEasterEggEnabled(context, enabled) }
    }
    val onVersionClickAction: () -> Unit = {
        versionClickCount++
        val message = EasterEggs.getVersionClickMessage(
            clickCount = versionClickCount,
            threshold = versionClickThreshold
        )
        val remainingClicks = (versionClickThreshold - versionClickCount).coerceAtLeast(0)
        val hapticType = if (remainingClicks <= 1) {
            HapticFeedbackType.LongPress
        } else {
            HapticFeedbackType.TextHandleMove
        }
        hapticFeedback.performHapticFeedback(hapticType)

        if (EasterEggs.isVersionEasterEggTriggered(versionClickCount, versionClickThreshold)) {
            showEasterEggDialog = true
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } else if (versionClickCount >= 2 || remainingClicks <= 3) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    val onExportLogsAction: () -> Unit = { LogCollector.exportAndShare(context) }
    val onTelegramClick: () -> Unit = { uriHandler.openUri(OFFICIAL_TELEGRAM_CHANNEL_URL) }
    val onTelegramGroupClick: () -> Unit = { uriHandler.openUri(OFFICIAL_TELEGRAM_GROUP_URL) }
    val onTwitterClick: () -> Unit = { uriHandler.openUri("https://x.com/YangY_0x00") }
    val onGithubClick: () -> Unit = { uriHandler.openUri(OFFICIAL_GITHUB_URL) }
    val onDisclaimerClick: () -> Unit = { showReleaseDisclaimerDialog = true }
    val onBlockedListClickAction: () -> Unit = { showBlockedList = true }
    val onCommentFraudHistoryClickAction: () -> Unit = { showCommentFraudHistory = true }
    // Effects
    LaunchedEffect(showCacheAnimation) {
        if (showCacheAnimation) {
            val before = CacheUtils.getCacheBreakdown(context)
            val selectedBytesBefore = resolveSelectedCacheBytes(before, pendingCacheClearTargets)
            cacheProgress = CacheClearProgress(
                current = 0L,
                total = selectedBytesBefore,
                isComplete = false
            )
            val clearResult = viewModel.clearCache(pendingCacheClearTargets)
            if (shouldMarkCacheClearAnimationComplete(clearResult.isSuccess)) {
                val after = clearResult.getOrThrow()
                val selectedBytesAfter = resolveSelectedCacheBytes(after, pendingCacheClearTargets)
                val actuallyClearedBytes = (selectedBytesBefore - selectedBytesAfter).coerceAtLeast(0L)
                cacheProgress = CacheClearProgress(
                    current = actuallyClearedBytes,
                    total = selectedBytesBefore,
                    isComplete = true,
                    clearedSize = formatCacheClearBytes(actuallyClearedBytes)
                )
            } else {
                Toast.makeText(
                    context,
                    resolveCacheClearFailureMessage(clearResult.exceptionOrNull()),
                    Toast.LENGTH_SHORT
                ).show()
                showCacheAnimation = false
                cacheProgress = null
            }
        }
    }

    LaunchedEffect(Unit) {
        AnalyticsHelper.logScreenView("SettingsScreen")
    }

    // Dialogs
    if (showCacheDialog) {
        CacheClearConfirmDialog(
            breakdown = state.cacheBreakdown,
            selectedCacheSizeSummary = selectedCacheSizeSummary,
            options = cacheClearOptions,
            selectedTargets = selectedCacheClearTargets,
            onTargetToggle = { target, checked ->
                selectedCacheClearTargets = if (checked) {
                    selectedCacheClearTargets + target
                } else {
                    selectedCacheClearTargets - target
                }
            },
            onConfirm = {
                if (selectedCacheClearTargets.isEmpty()) {
                    Toast.makeText(context, "请至少选择一项要清理的缓存", Toast.LENGTH_SHORT).show()
                } else {
                    pendingCacheClearTargets = selectedCacheClearTargets
                    showCacheDialog = false
                    showCacheAnimation = true
                }
            },
            onDismiss = { showCacheDialog = false }
        )
    }
    
    if (showCacheAnimation && cacheProgress != null) {
        CacheClearAnimationDialog(progress = cacheProgress!!, onDismiss = { showCacheAnimation = false; cacheProgress = null })
    }
    
    if (showPathDialog) {
        com.android.purebilibili.core.ui.AppAlertDialog(
            onDismissRequest = { showPathDialog = false },
            title = { AppText("下载位置", color = MaterialTheme.colorScheme.onSurface) },
            text = { 
                Column {
                    AppText("默认位置（应用私有目录）：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AppText(defaultPath.substringAfterLast("Android/"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))
                    AppText(
                        "可选：通过系统文件夹授权设置导出目录（无需“管理所有文件”权限）",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.android.purebilibili.core.theme.iOSOrange
                    )
                    if (!downloadExportTreeUri.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AppText(
                            "当前导出目录：已设置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                com.android.purebilibili.core.ui.AppDialogAction(onClick = {
                    showPathDialog = false
                    downloadFolderPicker.launch(null)
                }) { AppText("选择导出目录") }
            },
            dismissButton = { 
                com.android.purebilibili.core.ui.AppDialogAction(onClick = {
                    scope.launch {
                        SettingsManager.setDownloadPath(context, null)
                        SettingsManager.setDownloadExportTreeUri(context, null)
                    }
                    showPathDialog = false
                    Toast.makeText(context, "已恢复仅应用内存储", Toast.LENGTH_SHORT).show()
                }) { AppText("仅使用默认") }
            }
        )
    }
    if (showImageSavePathDialog) {
        com.android.purebilibili.core.ui.AppAlertDialog(
            onDismissRequest = { showImageSavePathDialog = false },
            title = { AppText("图片保存位置", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    AppText(
                        "默认保存到系统相册的 BBspace 文件夹。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppText(
                        "可通过系统文件夹授权选择动态图片、头像和评论图片的保存目录。",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.android.purebilibili.core.theme.iOSOrange
                    )
                    if (!imageSaveTreeUri.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AppText(
                            "当前图片目录：已选择",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                com.android.purebilibili.core.ui.AppDialogAction(onClick = {
                    showImageSavePathDialog = false
                    imageSaveFolderPicker.launch(null)
                }) { AppText("选择图片目录") }
            },
            dismissButton = {
                com.android.purebilibili.core.ui.AppDialogAction(onClick = {
                    scope.launch {
                        SettingsManager.setImageSaveTreeUri(context, null)
                    }
                    showImageSavePathDialog = false
                    Toast.makeText(context, "已恢复默认图片保存位置", Toast.LENGTH_SHORT).show()
                }) { AppText("恢复默认") }
            }
        )
    }
    
    if (showEasterEggDialog) {
        com.android.purebilibili.core.ui.AppAlertDialog(
            onDismissRequest = { showEasterEggDialog = false; versionClickCount = 0 },
            title = { AppText(" 你发现了彩蛋！", fontWeight = FontWeight.Bold) },
            text = { AppText("感谢你使用 BBspace！这是一个用爱发电的开源项目。") },
            confirmButton = { com.android.purebilibili.core.ui.AppDialogAction(onClick = { showEasterEggDialog = false; versionClickCount = 0 }) { AppText("我知道了！") } }
        )
    }


    if (showReleaseDisclaimerDialog) {
        ReleaseChannelDisclaimerDialog(
            onDismiss = { showReleaseDisclaimerDialog = false },
            onOpenGithub = onGithubClick,
            onOpenTelegram = onTelegramClick
        )
    }

    val onOpenLinksAction: () -> Unit = {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开设置", Toast.LENGTH_SHORT).show()
        }
    }
    val settingsBackTarget = resolveSettingsBackTarget(
        showCacheAnimation = showCacheAnimation,
        showCacheDialog = showCacheDialog,
        showPathDialog = showPathDialog,
        showImageSavePathDialog = showImageSavePathDialog,
        showEasterEggDialog = showEasterEggDialog,
        showReleaseDisclaimerDialog = showReleaseDisclaimerDialog,
        showBlockedList = showBlockedList,
    )
    SettingsLocalBackHandler(enabled = settingsBackTarget != SettingsBackTarget.NONE) {
        when (settingsBackTarget) {
            SettingsBackTarget.NONE -> Unit
            SettingsBackTarget.CACHE_ANIMATION -> {
                showCacheAnimation = false
                cacheProgress = null
            }
            SettingsBackTarget.CACHE_DIALOG -> showCacheDialog = false
            SettingsBackTarget.PATH_DIALOG -> showPathDialog = false
            SettingsBackTarget.IMAGE_SAVE_PATH_DIALOG -> showImageSavePathDialog = false
            SettingsBackTarget.EASTER_EGG_DIALOG -> {
                showEasterEggDialog = false
                versionClickCount = 0
            }
            SettingsBackTarget.RELEASE_DISCLAIMER_DIALOG -> showReleaseDisclaimerDialog = false
            SettingsBackTarget.BLOCKED_LIST -> showBlockedList = false
        }
    }

    // 页面跳转逻辑
    CompositionLocalProvider(LocalSettingsLiquidGlassEnabled provides state.isLiquidGlassEnabled) {
        if (showBlockedList) {
            BlockedListScreen(onBack = { showBlockedList = false })
        } else if (showCommentFraudHistory) {
            CommentFraudHistoryScreen(onBack = { showCommentFraudHistory = false })
        } else {
        // Layout Switching
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceCompat(state = activeHazeState)
        ) {
            if (shouldRenderSettingsSinglePaneContent(
                    widthDp = configuration.screenWidthDp,
                    forceSinglePaneContent = forceSinglePaneContent
                )
            ) {
                MobileSettingsNavLayout(
                    destination = destination,
                    rootEntranceEnabled = rootEntranceEnabled,
                    rootEntranceStartWhen = rootEntranceStartWhen,
                    onBack = onBack,
                    onCategoryClick = onCategoryClick,
                    onSearchOpen = onSearchOpen,
                    onAppearanceClick = onAppearanceClick,
                    onHomeClick = onHomeClick,
                    onAnimationClick = onAnimationClick,
                    onPlaybackClick = onPlaybackClick,
                    onPermissionClick = onPermissionClick,
                    onMessageNotificationClick = onMessageNotificationClick,
                    onNavigateToBottomBarSettings = onNavigateToBottomBarSettings,
                    onAnimeSettingsClick = onAnimeSettingsClick,
                    onTipsClick = onTipsClick,
                    onPluginsClick = onPluginsClick,
                    onExportLogsClick = onExportLogsAction,
                    onLicenseClick = onOpenSourceLicensesClick,
                    onDisclaimerClick = onDisclaimerClick,
                    onGithubClick = onGithubClick,
                    onVersionClick = onVersionClickAction,
                    onReplayOnboardingClick = onReplayOnboardingClick,
                    onTelegramClick = onTelegramClick,
                    onTelegramGroupClick = onTelegramGroupClick,
                    onTwitterClick = onTwitterClick,
                    onSettingsShareClick = onSettingsShareClick,
                    onWebDavBackupClick = onWebDavBackupClick,
                    onDownloadPathClick = onDownloadPathAction,
                    onImageSavePathClick = onImageSavePathAction,
                    onClearCacheClick = onClearCacheAction,
                    onAutoCacheClearIntervalChange = { interval ->
                        scope.launch { SettingsManager.setAutoCacheClearInterval(context, interval) }
                    },
                    onAutoCacheClearThresholdChange = { thresholdGb ->
                        scope.launch {
                            SettingsManager.setAutoCacheClearThresholdGb(context, thresholdGb)
                        }
                    },
                    onCreditsClick = onCreditsClick,
                    onOpenLinksClick = onOpenLinksAction,
                    onBlockedListClick = onBlockedListClickAction,
                    onCommentFraudHistoryClick = onCommentFraudHistoryClickAction,
                    onPrivacyModeChange = onPrivacyModeChange,
                    onSearchSuggestionsChange = onSearchSuggestionsChange,
                    onPrivacyContentAuthenticationChange = onPrivacyContentAuthenticationChange,
                    onCrashTrackingChange = onCrashTrackingChange,
                    onAnalyticsChange = onAnalyticsChange,
                    onEnhancedDiagnosticLoggingChange = onEnhancedDiagnosticLoggingChange,
                    onEasterEggChange = onEasterEggChange,
                    privacyModeEnabled = privacyModeEnabled,
                    searchSuggestionsEnabled = searchSuggestionsEnabled,
                    customDownloadPath = downloadExportTreeUri ?: customDownloadPath,
                    customImageSavePath = imageSaveTreeUri,
                    cacheSize = state.cacheSize,
                    autoCacheClearInterval = autoCacheClearInterval,
                    autoCacheClearThresholdGb = autoCacheClearThresholdGb,
                    crashTrackingEnabled = crashTrackingEnabled,
                    analyticsEnabled = analyticsEnabled,
                    enhancedDiagnosticLoggingEnabled = enhancedDiagnosticLoggingEnabled,
                    pluginCount = PluginManager.getEnabledCount(),
                    versionName = com.android.purebilibili.BuildConfig.VERSION_NAME,
                    appIcon = state.appIcon,
                    versionClickCount = versionClickCount,
                    versionClickThreshold = versionClickThreshold,
                    easterEggEnabled = easterEggEnabled,
                    privacyContentAuthenticationEnabled = privacyContentAuthenticationEnabled,
                    cardAnimationEnabled = state.cardAnimationEnabled,
                    isBottomBarFloating = state.isBottomBarFloating,
                    bottomBarLabelMode = state.bottomBarLabelMode,
                    feedApiType = feedApiType,
                    onFeedApiTypeChange = { type ->
                        scope.launch {
                            SettingsManager.setFeedApiType(context, type)
                            android.widget.Toast.makeText(
                                context,
                                "已切换为${type.label}，下拉刷新生效",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    incrementalTimelineRefreshEnabled = incrementalTimelineRefreshEnabled,
                    onIncrementalTimelineRefreshChange = { enabled ->
                        scope.launch {
                            SettingsManager.setIncrementalTimelineRefresh(context, enabled)
                        }
                    },
                    dynamicImagePreviewTextVisible = dynamicImagePreviewTextVisible,
                    onDynamicImagePreviewTextVisibleChange = { visible ->
                        scope.launch {
                            SettingsManager.setDynamicImagePreviewTextVisible(context, visible)
                        }
                    },
                    dynamicAllTabHorizontalUserListVisible = dynamicAllTabHorizontalUserListVisible,
                    onDynamicAllTabHorizontalUserListVisibleChange = { visible ->
                        scope.launch {
                            SettingsManager.setDynamicAllTabHorizontalUserListVisible(context, visible)
                        }
                    },
                    dynamicTopBarCollapseOnScroll = dynamicTopBarCollapseOnScroll,
                    onDynamicTopBarCollapseOnScrollChange = { enabled ->
                        scope.launch {
                            SettingsManager.setDynamicTopBarCollapseOnScroll(context, enabled)
                        }
                    },
                    dynamicFeedLayoutMode = dynamicFeedLayoutMode,
                    onDynamicFeedLayoutModeChange = { mode ->
                        scope.launch {
                            SettingsManager.setDynamicFeedLayoutMode(context, mode)
                        }
                    },
                    dynamicVisibleTabIds = dynamicVisibleTabIds,
                    onDynamicTabVisibilityChange = { tabId ->
                        scope.launch {
                            SettingsManager.setDynamicTabVisibleTabs(
                                context,
                                resolveDynamicVisibleTabIdsAfterToggle(dynamicVisibleTabIds, tabId)
                            )
                        }
                    },
                    homeRefreshCount = homeRefreshCount,
                    onHomeRefreshCountChange = { count ->
                        scope.launch {
                            SettingsManager.setHomeRefreshCount(context, count)
                        }
                    },
                )
            }
        }
        }
    }
}

internal fun shouldMarkCacheClearAnimationComplete(clearSucceeded: Boolean): Boolean = clearSucceeded

internal fun resolveCacheClearFailureMessage(error: Throwable?): String {
    return error?.message?.takeIf { it.isNotBlank() } ?: "清理缓存失败，请稍后重试"
}

@Composable
internal fun SettingsCategoryHeader(title: String) {
    AppText(
        text = title,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = AppSpacingTokens.Small)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileSettingsNavLayout(
    destination: SettingsNavDestination,
    rootEntranceEnabled: Boolean,
    rootEntranceStartWhen: Boolean,
    onBack: () -> Unit,
    onCategoryClick: (SettingsRootCategory) -> Unit,
    onSearchOpen: () -> Unit,
    onAppearanceClick: () -> Unit,
    onHomeClick: () -> Unit,
    onAnimationClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onPermissionClick: () -> Unit,
    onMessageNotificationClick: () -> Unit,
    onNavigateToBottomBarSettings: () -> Unit,
    onAnimeSettingsClick: () -> Unit = {},
    onTipsClick: () -> Unit,
    onPluginsClick: () -> Unit,
    onExportLogsClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onDisclaimerClick: () -> Unit,
    onGithubClick: () -> Unit,
    onVersionClick: () -> Unit,
    onReplayOnboardingClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onTelegramGroupClick: () -> Unit = {},
    onTwitterClick: () -> Unit,
    onSettingsShareClick: () -> Unit,
    onWebDavBackupClick: () -> Unit,
    onDownloadPathClick: () -> Unit,
    onImageSavePathClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onAutoCacheClearIntervalChange: (SettingsManager.AutoCacheClearInterval) -> Unit,
    onAutoCacheClearThresholdChange: (Int) -> Unit,
    onCreditsClick: () -> Unit = {},
    onOpenLinksClick: () -> Unit,
    onBlockedListClick: () -> Unit,
    onCommentFraudHistoryClick: () -> Unit,
    onPrivacyModeChange: (Boolean) -> Unit,
    onSearchSuggestionsChange: (Boolean) -> Unit,
    onPrivacyContentAuthenticationChange: (Boolean) -> Unit,
    onCrashTrackingChange: (Boolean) -> Unit,
    onAnalyticsChange: (Boolean) -> Unit,
    onEnhancedDiagnosticLoggingChange: (Boolean) -> Unit,
    onEasterEggChange: (Boolean) -> Unit,
    privacyModeEnabled: Boolean,
    searchSuggestionsEnabled: Boolean,
    privacyContentAuthenticationEnabled: Boolean,
    customDownloadPath: String?,
    customImageSavePath: String?,
    cacheSize: String,
    autoCacheClearInterval: SettingsManager.AutoCacheClearInterval,
    autoCacheClearThresholdGb: Int,
    crashTrackingEnabled: Boolean,
    analyticsEnabled: Boolean,
    enhancedDiagnosticLoggingEnabled: Boolean,
    pluginCount: Int,
    versionName: String,
    appIcon: String,
    versionClickCount: Int,
    versionClickThreshold: Int,
    easterEggEnabled: Boolean,
    cardAnimationEnabled: Boolean,
    isBottomBarFloating: Boolean,
    bottomBarLabelMode: Int,
    feedApiType: SettingsManager.FeedApiType,
    onFeedApiTypeChange: (SettingsManager.FeedApiType) -> Unit,
    incrementalTimelineRefreshEnabled: Boolean,
    onIncrementalTimelineRefreshChange: (Boolean) -> Unit,
    dynamicImagePreviewTextVisible: Boolean,
    onDynamicImagePreviewTextVisibleChange: (Boolean) -> Unit,
    dynamicAllTabHorizontalUserListVisible: Boolean,
    onDynamicAllTabHorizontalUserListVisibleChange: (Boolean) -> Unit,
    dynamicTopBarCollapseOnScroll: Boolean,
    onDynamicTopBarCollapseOnScrollChange: (Boolean) -> Unit,
    dynamicFeedLayoutMode: com.android.purebilibili.core.store.SettingsManager.DynamicFeedLayoutMode,
    onDynamicFeedLayoutModeChange: (com.android.purebilibili.core.store.SettingsManager.DynamicFeedLayoutMode) -> Unit,
    dynamicVisibleTabIds: Set<String>,
    onDynamicTabVisibilityChange: (String) -> Unit,
    homeRefreshCount: Int,
    onHomeRefreshCountChange: (Int) -> Unit,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val sectionOrder = remember { resolveSettingsRootCategoryOrder() }
    val bottomBarVisible = LocalBottomBarVisible.current
    val bottomInset = resolveSettingsContentBottomPadding(
        navigationBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        bottomBarVisible = bottomBarVisible,
        isBottomBarFloating = isBottomBarFloating,
        bottomBarLabelMode = bottomBarLabelMode,
        isTablet = windowSizeClass.isTablet,
    )
    val screenTitle = resolveSettingsNavDestinationTitle(destination)
    val backLabel = stringResource(R.string.common_back)
    val rootCategoryActions = SettingsRootCategoryActions(
        onAppearanceClick = onAppearanceClick,
        onHomeClick = onHomeClick,
        onAnimationClick = onAnimationClick,
        onPlaybackClick = onPlaybackClick,
        onBottomBarClick = onNavigateToBottomBarSettings,
        onPermissionClick = onPermissionClick,
        onMessageNotificationClick = onMessageNotificationClick,
        onBlockedListClick = onBlockedListClick,
        onCommentFraudHistoryClick = onCommentFraudHistoryClick,
        onPluginsClick = onPluginsClick,
        onExportLogsClick = onExportLogsClick,
        onSettingsShareClick = onSettingsShareClick,
        onWebDavBackupClick = onWebDavBackupClick,
        onDownloadPathClick = onDownloadPathClick,
        onImageSavePathClick = onImageSavePathClick,
        onClearCacheClick = onClearCacheClick,
        onAutoCacheClearIntervalChange = onAutoCacheClearIntervalChange,
        onAutoCacheClearThresholdChange = onAutoCacheClearThresholdChange,
        onGithubClick = onGithubClick,
        onTelegramClick = onTelegramClick,
        onTelegramGroupClick = onTelegramGroupClick,
        onTwitterClick = onTwitterClick,
        onCreditsClick = onCreditsClick,
        onDisclaimerClick = onDisclaimerClick,
        onLicenseClick = onLicenseClick,
        onVersionClick = onVersionClick,
        onReplayOnboardingClick = onReplayOnboardingClick,
        onTipsClick = onTipsClick,
        onOpenLinksClick = onOpenLinksClick,
        onPrivacyModeChange = onPrivacyModeChange,
        onSearchSuggestionsChange = onSearchSuggestionsChange,
        onPrivacyContentAuthenticationChange = onPrivacyContentAuthenticationChange,
        onCrashTrackingChange = onCrashTrackingChange,
        onAnalyticsChange = onAnalyticsChange,
        onEnhancedDiagnosticLoggingChange = onEnhancedDiagnosticLoggingChange,
        onEasterEggChange = onEasterEggChange,
        onFeedApiTypeChange = onFeedApiTypeChange,
        onIncrementalTimelineRefreshChange = onIncrementalTimelineRefreshChange,
        onDynamicImagePreviewTextVisibleChange = onDynamicImagePreviewTextVisibleChange,
        onDynamicAllTabHorizontalUserListVisibleChange = onDynamicAllTabHorizontalUserListVisibleChange,
        onDynamicTopBarCollapseOnScrollChange = onDynamicTopBarCollapseOnScrollChange,
        onDynamicFeedLayoutModeChange = onDynamicFeedLayoutModeChange,
        onDynamicTabVisibilityChange = onDynamicTabVisibilityChange,
        onHomeRefreshCountChange = onHomeRefreshCountChange,
    )
    val rootCategoryState = SettingsRootCategoryState(
        privacyModeEnabled = privacyModeEnabled,
        searchSuggestionsEnabled = searchSuggestionsEnabled,
        privacyContentAuthenticationEnabled = privacyContentAuthenticationEnabled,
        crashTrackingEnabled = crashTrackingEnabled,
        analyticsEnabled = analyticsEnabled,
        enhancedDiagnosticLoggingEnabled = enhancedDiagnosticLoggingEnabled,
        pluginCount = pluginCount,
        customDownloadPath = customDownloadPath,
        customImageSavePath = customImageSavePath,
        cacheSize = cacheSize,
        autoCacheClearInterval = autoCacheClearInterval,
        autoCacheClearThresholdGb = autoCacheClearThresholdGb,
        versionName = versionName,
        appIcon = appIcon,
        easterEggEnabled = easterEggEnabled,
        versionClickCount = versionClickCount,
        versionClickThreshold = versionClickThreshold,
        feedApiType = feedApiType,
        incrementalTimelineRefreshEnabled = incrementalTimelineRefreshEnabled,
        dynamicImagePreviewTextVisible = dynamicImagePreviewTextVisible,
        dynamicAllTabHorizontalUserListVisible = dynamicAllTabHorizontalUserListVisible,
        dynamicTopBarCollapseOnScroll = dynamicTopBarCollapseOnScroll,
        dynamicFeedLayoutMode = dynamicFeedLayoutMode,
        dynamicVisibleTabIds = dynamicVisibleTabIds,
        homeRefreshCount = homeRefreshCount,
    )

    @Composable
    fun SettingsRootContent() {
        when (destination) {
            SettingsNavDestination.Home -> {
                Column {
                    SettingsHomeSearchEntry(onClick = onSearchOpen)
                    Box(modifier = Modifier.padding(top = 8.dp).entrance()) {
                        SettingsRootCategoryListSection(
                            categories = sectionOrder,
                            onCategoryClick = onCategoryClick,
                        )
                    }
                    // ★ 番剧设置入口：番剧页用独立存储(ani_settings)，与上面的主设置互不影响
                    Box(modifier = Modifier.padding(top = 8.dp).entrance()) {
                        SettingsDetailEntrySection(
                            entries = listOf(
                                SettingsDetailEntry(
                                    target = SettingsSearchTarget.PLAYBACK,
                                    title = "番剧设置",
                                    value = "追番播放、弹幕与详情页偏好，仅作用于番剧页",
                                    onClick = onAnimeSettingsClick,
                                ),
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            is SettingsNavDestination.Category -> {
                Box(modifier = Modifier.padding(top = 12.dp)) {
                    SettingsRootCategoryContent(
                        category = destination.category,
                        actions = rootCategoryActions,
                        state = rootCategoryState,
                    )
                }
            }
            SettingsNavDestination.Search -> Unit
        }
    }

    com.android.purebilibili.feature.settings.ui.SettingsPageScaffold(
        title = screenTitle,
        onBack = onBack,
        backContentDescription = backLabel,
        bottomContentPadding = bottomInset,
        topBarStyle = when {
            destination == SettingsNavDestination.Home &&
                com.android.purebilibili.core.ui.isMiuixNonGlassEnabled() -> {
                com.android.purebilibili.core.ui.AppTopBarStyle.LARGE
            }
            destination == SettingsNavDestination.Home -> {
                com.android.purebilibili.core.ui.AppTopBarStyle.SMALL
            }
            else -> {
                com.android.purebilibili.core.ui.AppTopBarStyle.CENTERED
            }
        },
    ) {
        if (rootEntranceEnabled) {
            EntranceGroup(startWhen = rootEntranceStartWhen) {
                SettingsRootContent()
            }
        } else {
            SettingsRootContent()
        }
    }
}
