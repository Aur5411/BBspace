// 文件路径: feature/anime/AnimePlayerScreen.kt
//
// 「追番播放器」—— 与首页视频播放器「完全分离、独立实现」。
//
// 为什么不复用首页播放器: 追番多了首页没有的「换源」能力, 而且片源是
// BT/在线直链 (非 B 站 playurl), 播放地址在播放过程中可能整条替换。
// 因此这里自带一套精简的 ExoPlayer 宿主 + 换源面板。
//
// 能力:
//   1. 播放 (media3 ExoPlayer + PlayerView)
//   2. 换源   (AniMediaSourceRepository -> AniSourceSheetContent)
//   4. 本地历史 (AnimekoLocalRepository.recordPlayback)
//
// ★ 本页全部可调行为都读「番剧设置」AniSettingsStore（独立 prefs "ani_settings"）:
//     画面缩放 / 快进步长 / 硬件解码 / 自动连播 / 记住进度
//   全程不引用 SettingsManager，因此主 App 设置改动**不会**影响番剧页。
package com.android.purebilibili.feature.anime

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.PlayerView
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.ImmersiveAppScaffold as AppScaffold
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.data.model.animeko.AniEpisode
import com.android.purebilibili.data.model.animeko.AniMediaCandidate
import com.android.purebilibili.data.model.animeko.AniMediaQuery
import com.android.purebilibili.data.model.animeko.AniMediaSourceKind
import com.android.purebilibili.data.model.animeko.AniSubjectDetail

/**
 * 追番播放页。
 *
 * @param subjectId 条目 id
 * @param episodeId 初始播放的剧集 id
 * @param onSettingsClick 打开「番剧设置」（番剧自有设置，与主设置隔离）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimePlayerScreen(
    subjectId: Long,
    episodeId: Long,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: AnimeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val sourceState by viewModel.sourceState.collectAsStateWithLifecycle()

    // ★ 番剧自有设置（实时订阅，改完立即生效，且只影响本页）
    val aniSettings = rememberAniSettingsState()

    var currentEpisodeId by remember { mutableLongStateOf(episodeId) }
    var currentUrl by remember { mutableStateOf<String?>(null) }
    // 当前正在播放的来源名，展示在播放信息行里，便于确认换源是否生效
    var currentSourceName by remember { mutableStateOf<String?>(null) }
    // ---- 自动换源 ----
    // 可直链播放的候选（按换源结果顺序）与当前用到的下标。
    // 用 MutableState 持有，保证 Player.Listener 里读到的一直是最新值。
    val failoverCandidates = remember { mutableStateOf<List<AniMediaCandidate>>(emptyList()) }
    val failoverIndex = remember { mutableIntStateOf(0) }
    // 自动换源后短暂提示，让用户知道发生了什么
    var failoverNotice by remember { mutableStateOf<String?>(null) }
    var showSourceSheet by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var positionMillis by remember { mutableLongStateOf(0L) }
    var durationMillis by remember { mutableLongStateOf(0L) }
    // 画面缩放：默认取设置，播放中可点按钮临时切换
    var resizeMode by remember { mutableIntStateOf(aniSettings.defaultResizeMode.toMedia3ResizeMode()) }
    var autoNextTriggered by remember { mutableStateOf(false) }
    // ---- 播放器控制条 ----
    // 全屏: 隐藏顶栏/信息区, 播放器铺满整个屏幕
    var isFullscreen by remember { mutableStateOf(false) }
    // 控制条显隐(全屏/半屏共用, 点播放器画面切换)
    var controlsVisible by remember { mutableStateOf(true) }
    // 进度条拖动中: true 时进度条完全由手指控制, 不被播放进度回写覆盖
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableLongStateOf(0L) }
    // ---- 横向滑动快进/快退 ----
    var isSeekDragging by remember { mutableStateOf(false) }
    var seekPreviewMs by remember { mutableLongStateOf(0L) }
    var seekDeltaMs by remember { mutableLongStateOf(0L) }
    // ---- 磁力/种子源面板 ----
    var showMagnetSheet by remember { mutableStateOf(false) }
    // 全屏 = 隐藏系统栏 + 旋转到横屏。
    // ★ 播放器实例已挪进 ViewModel, 旋转触发的 Activity 重建不再丢播放
    //   状态(此前「一点全屏就黑屏」的根因), 可以放心转屏了。
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) break
            ctx = ctx.baseContext
        }
        ctx as? android.app.Activity
    }
    DisposableEffect(isFullscreen) {
        val window = activity?.window
        val originalOrientation = activity?.requestedOrientation
        if (isFullscreen) {
            activity?.requestedOrientation =
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            window?.let { w ->
                val controller = androidx.core.view.WindowCompat.getInsetsController(w, w.decorView)
                controller.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            window?.let { w ->
                val controller = androidx.core.view.WindowCompat.getInsetsController(w, w.decorView)
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
            // ★ 无条件恢复竖屏：退出全屏时 onDispose 执行时 isFullscreen 已经
            //   变成 false，之前用 if(isFullscreen) 守卫导致恢复代码永远不跑，
            //   表现为「退出全屏后整个 App 卡在横屏」
            activity?.requestedOrientation = originalOrientation
                ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // 全屏时系统返回手势 = 退出全屏(而不是退出播放页)
    androidx.activity.compose.BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }


    // ---- 加载条目详情 (标题/封面/剧集列表) ----
    LaunchedEffect(subjectId) {
        if (detailState !is AniDetailState.Success) {
            viewModel.loadDetail(subjectId)
        }
    }
    val detail = (detailState as? AniDetailState.Success)?.detail
    val episode: AniEpisode? = detail?.episodes?.firstOrNull { it.episodeId == currentEpisodeId }


    // ---- 设置变化 -> 同步到已存在的播放器 ----
    LaunchedEffect(aniSettings.defaultResizeMode) {
        resizeMode = aniSettings.defaultResizeMode.toMedia3ResizeMode()
    }

    // ---- ExoPlayer 宿主 ----
    // ★ 播放器实例在 ViewModel 里: 全屏旋转触发 Activity 重建时,
    //   remember{} 的播放器会连状态一起丢(黑屏根因), ViewModel 则无缝存活
    val exoPlayer = viewModel.exoPlayer

    // ---- 下一集: 优先复用当前源 ----
    // 记住当前正在播的源 id; 切下一集时先只搜这个源, 命中即播(省掉全量搜索)
    var preferredSourceId by remember { mutableStateOf<String?>(null) }
    // 当前可看的正片剧集(去重), 供「下一集」定位
    val playableEpisodes = remember(detail) {
        val all = detail?.episodes.orEmpty()
        all.filter { it.isMain }.distinctBy { it.displayEp }
            .ifEmpty { all.distinctBy { it.displayEp } }
    }
    val hasNextEpisode = remember(playableEpisodes, currentEpisodeId) {
        val idx = playableEpisodes.indexOfFirst { it.episodeId == currentEpisodeId }
        idx >= 0 && idx + 1 < playableEpisodes.size
    }

    /** 切到下一集(复用当前源; 找不到再回退全量搜索)。 */
    fun goToNextEpisode() {
        val idx = playableEpisodes.indexOfFirst { it.episodeId == currentEpisodeId }
        val next = playableEpisodes.getOrNull(idx + 1) ?: return
        currentEpisodeId = next.episodeId
        currentUrl = null
        currentSourceName = null
        failoverIndex.intValue = 0
        autoNextTriggered = false
        viewModel.resetMediaSources()
    }

    // ---- 自动播放门控 ----
    // 换源/切集后等「画面首帧渲染」再开播, 否则会出现「还没出画面就有声音」
    val readyToPlay = remember { mutableStateOf(false) }
    val pendingAutoPlay = remember { mutableStateOf(false) }

    // ---- 画质面板 ----
    var showQualitySheet by remember { mutableStateOf(false) }
    // 候选源 -> 分辨率高度(懒加载探测, 0=未知)
    val qualityMap = remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    // ---- 倍速播放 0.5x ~ 2x ----
    val playbackSpeeds = remember { listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f) }
    var speedIndex by rememberSaveable { mutableIntStateOf(2) }
    LaunchedEffect(speedIndex) {
        exoPlayer.playbackParameters = exoPlayer.playbackParameters
            .withSpeed(playbackSpeeds[speedIndex])
    }

    // ★ 退出播放页彻底停播:
    //   播放器实例在 ViewModel 里跨页面存活, 不主动停就会出现「退出后仍在放声音」。
    //   用 composition 的 onDispose(页面被销毁时触发) —— 比 LocalLifecycleOwner 可靠,
    //   后者在本项目里绑定的是 Activity(navigation3 host), 退出页面时并不会销毁。
    //   转屏因 manifest 声明了 configChanges, 不会重建 composition, 因此不会误停。
    DisposableEffect(Unit) {
        // 进入页面先确保是暂停态, 避免残留上一集的音频
        exoPlayer.pause()
        onDispose {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }


    /**
     * 自动换源：当前源播不了（解码/网络报错，或起播超时）时切到下一个可直链播放的候选。
     *
     * 只在**在线直链**候选之间切换；BT/磁力无法直接播放，不参与。
     * 全部试完仍失败才把错误显示出来。
     */
    fun switchToNextSource(reason: String) {
        val list = failoverCandidates.value
        val nextIndex = failoverIndex.intValue + 1
        val next = list.getOrNull(nextIndex)
        if (next == null) {
            errorMessage = when {
                list.isEmpty() -> "没有可直连播放的源（$reason）"
                list.size == 1 -> "当前源无法播放：$reason"
                else -> "已尝试 ${list.size} 个源均无法播放（$reason）"
            }
            return
        }
        failoverIndex.intValue = nextIndex
        currentSourceName = next.sourceName
        errorMessage = null
        failoverNotice = "已自动切换到「${next.sourceName}」"
        currentUrl = next.url
    }

    // 播放器事件 -> UI 状态
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // 当前源播不了 -> 自动换下一个源，全部失败才报错
                switchToNextSource("播放失败 ${error.errorCodeName}")
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        errorMessage = null
                        durationMillis = exoPlayer.duration.coerceAtLeast(0L)
                        readyToPlay.value = true
                        // 纯音频(或首帧回调迟迟不来)时直接开播, 避免卡住不播
                        val hasVideo = exoPlayer.currentTracks.groups.any {
                            it.type == androidx.media3.common.C.TRACK_TYPE_VIDEO && it.length > 0
                        }
                        if (pendingAutoPlay.value && !hasVideo) {
                            pendingAutoPlay.value = false
                            exoPlayer.play()
                        }
                    }

                    Player.STATE_ENDED, Player.STATE_IDLE -> readyToPlay.value = false
                }
            }

            override fun onRenderedFirstFrame() {
                // ★ 画面真正渲染出来后才开声音
                if (pendingAutoPlay.value) {
                    pendingAutoPlay.value = false
                    exoPlayer.play()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            // 不 release: 播放器由 ViewModel 持有, 转屏/重建后继续用
            exoPlayer.removeListener(listener)
        }
    }

    // 记录本地历史（受「记录追番历史」开关控制，逻辑在 ViewModel 内判定）
    val latestDetail by rememberUpdatedState(detail)
    val latestEpisode by rememberUpdatedState(episode)
    // 每 500ms 回写一次播放进度 (给进度条与历史记录用)，同时处理「自动连播」
    // ★ v4.2.1: 观看历史改为**播放中周期落库**(5 秒节流) ——
    //   旧版只在页面退出(onDispose)时写一次, 而退出瞬间 viewModelScope 被取消,
    //   写入协程没跑完就没了, 结果历史页永远是空的。
    var lastHistoryWriteAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(exoPlayer, detail) {
        val episodes = detail?.episodes.orEmpty()
        while (true) {
            positionMillis = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            // 观看历史: 播放中每 5 秒记一笔(upsert 同一集, 不会重复)
            if (
                exoPlayer.isPlaying &&
                latestDetail != null && latestEpisode != null &&
                System.currentTimeMillis() - lastHistoryWriteAt >= 5_000L
            ) {
                lastHistoryWriteAt = System.currentTimeMillis()
                latestDetail?.let { d ->
                    latestEpisode?.let { ep ->
                        viewModel.recordPlayback(
                            detail = d,
                            episode = ep,
                            positionMillis = exoPlayer.currentPosition,
                            durationMillis = duration.coerceAtLeast(0L),
                        )
                    }
                }
            }
            // 自动连播: 本条播放结束 + 开关打开 + 不是最后一话
            if (
                aniSettings.autoPlayNextEpisode &&
                !autoNextTriggered &&
                duration > 0L &&
                exoPlayer.playbackState == Player.STATE_ENDED
            ) {
                val idx = episodes.indexOfFirst { it.episodeId == currentEpisodeId }
                val next = if (idx >= 0 && idx + 1 < episodes.size) episodes[idx + 1] else null
                if (next != null) {
                    autoNextTriggered = true
                    currentEpisodeId = next.episodeId
                    currentUrl = null
                    currentSourceName = null
                    failoverIndex.intValue = 0
                    viewModel.resetMediaSources()
                }
            }
            kotlinx.coroutines.delay(500)
        }
    }

    // 退出播放页时的兜底落库（配合上面的周期写入; NonCancellable 保证能写完）
    DisposableEffect(Unit) {
        onDispose {
            val d = latestDetail
            val ep = latestEpisode
            if (d != null && ep != null) {
                viewModel.recordPlayback(
                    detail = d,
                    episode = ep,
                    positionMillis = exoPlayer.currentPosition,
                    durationMillis = exoPlayer.duration.coerceAtLeast(0L),
                )
            }
        }
    }

    // 拉取可用源。
    // ★ 必须把 detail 也作为 key：首帧 detailState 还是 Loading（detail 为 null），
    //   若不带 detail 作 key，这个 effect 会在 detail 到达前就 return 且永不再触发 ——
    //   结果就是「从来没发起过换源请求」，番剧自然播不了、换源面板也永远是空的。
    LaunchedEffect(subjectId, currentEpisodeId, detail) {
        val d = detail ?: return@LaunchedEffect
        // 目标集找不到时退回第一集正片，避免 episodeId 对不上就整条链路不跑
        val ep = d.episodes.firstOrNull { it.episodeId == currentEpisodeId }
            ?: d.episodes.firstOrNull { it.isMain }
            ?: d.episodes.firstOrNull()
            ?: return@LaunchedEffect
        viewModel.loadMediaSources(
            AniMediaQuery(
                bangumiId = subjectId,
                subjectName = d.displayName,
                episodeSort = ep.displayEp,
                episodeName = ep.displayName,
                episodeId = ep.episodeId,
            ),
            // 有当前源时优先只搜它
            onlySourceId = preferredSourceId,
        )
    }

    // 优先源没搜到时回退全量检索(只回退一次, 避免循环)
    LaunchedEffect(sourceState) {
        val st = sourceState
        if (st is AniSourceState.Success && st.result.isEmpty && preferredSourceId != null) {
            preferredSourceId = null
            detail?.let { d ->
                val ep = d.episodes.firstOrNull { it.episodeId == currentEpisodeId }
                    ?: d.episodes.firstOrNull { it.isMain }
                if (ep != null) {
                    viewModel.loadMediaSources(
                        AniMediaQuery(
                            bangumiId = subjectId,
                            subjectName = d.displayName,
                            episodeSort = ep.displayEp,
                            episodeName = ep.displayName,
                            episodeId = ep.episodeId,
                        )
                    )
                }
            }
        }
    }

    // 收下所有「可直链播放」的候选作为自动换源队列；未起播时自动播第一条。
    // BT/磁力（.torrent / magnet）不能被播放器直接播放，不参与自动播放，
    // 需要用户到「换源」面板里查看，并交给外部 BT 客户端。
    LaunchedEffect(sourceState) {
        val st = sourceState
        if (st is AniSourceState.Success) {
            val playable = st.result.candidates.filter { it.playableDirectly }
            failoverCandidates.value = playable
            failoverIndex.intValue = 0
            if (currentUrl == null) {
                // ★ v4.1.0: 已下载的集数**默认播本地缓存** —— local_cache 候选
                //   在 loadMediaSources 里已插到最前, 这里再显式优先一次,
                //   避免任何排序变化导致在线源抢在缓存前面。
                val first = playable.firstOrNull { it.kind == AniMediaSourceKind.LOCAL.name }
                    ?: playable.firstOrNull()
                if (first != null) {
                    currentSourceName = first.sourceName
                    preferredSourceId = first.sourceId.takeIf { it.isNotBlank() }
                    currentUrl = first.url
                }
            }
        }
    }

    // 地址变化 -> 换源/切集。
    // ★ 必须先把旧源彻底停掉: 否则「切换新视频源后旧源声音还在放」
    LaunchedEffect(currentUrl) {
        val url = currentUrl ?: return@LaunchedEffect
        autoNextTriggered = false
        pendingAutoPlay.value = false
        readyToPlay.value = false
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        val resumeAt = viewModel.resumePositionFor(currentEpisodeId)
        if (resumeAt > 0L) {
            exoPlayer.seekTo(resumeAt)
        }
        // 不立即 play: 等 onRenderedFirstFrame(画面出来)后再播
        pendingAutoPlay.value = true
    }

    // 兜底: 首帧回调 700ms 内没来也开播, 避免个别片源卡在暂停
    LaunchedEffect(pendingAutoPlay.value) {
        if (pendingAutoPlay.value) {
            kotlinx.coroutines.delay(700)
            if (pendingAutoPlay.value) {
                pendingAutoPlay.value = false
                exoPlayer.play()
            }
        }
    }

    // 起播看门狗：某些源不会回播放错误，只是一直转圈。
    // 设定时间内没能进入 READY 就判定这个源不可用，自动换下一个。
    LaunchedEffect(currentUrl) {
        if (currentUrl == null) return@LaunchedEffect
        kotlinx.coroutines.delay(SOURCE_FAILOVER_WATCHDOG_MS)
        if (exoPlayer.playbackState != Player.STATE_READY) {
            switchToNextSource("起播超时")
        }
    }

    // 自动换源提示 3 秒后自动消失
    LaunchedEffect(failoverNotice) {
        if (failoverNotice != null) {
            kotlinx.coroutines.delay(3000)
            failoverNotice = null
        }
    }

    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // 全屏时隐藏顶栏, 只留播放画面
            if (!isFullscreen) {
                AppTopBar(
                    title = detail?.displayName ?: "播放",
                    subtitle = episode?.let { "第${it.displayEp}话 ${it.displayName}".trim() },
                    navigationIcon = {
                        AppIconButton(onClick = onBack) {
                            AppIcon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "返回",
                            )
                        }
                    },
                    actions = {
                        AppIconButton(
                            onClick = {
                                val next = AniResizeMode.entries.firstOrNull { it.toMedia3ResizeMode() == resizeMode }
                                    ?.next() ?: AniResizeMode.FIT
                                resizeMode = next.toMedia3ResizeMode()
                            },
                        ) {
                            AppIcon(
                                imageVector = Icons.Outlined.AspectRatio,
                                contentDescription = "画面缩放",
                            )
                        }
                        // 番剧设置入口（番剧自有设置，与主设置隔离）
                        AppIconButton(onClick = onSettingsClick) {
                            AppIcon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "番剧设置",
                            )
                        }
                        AppIconButton(
                            onClick = {
                                errorMessage = null
                                detail?.let { d ->
                                    val ep = d.episodes.firstOrNull { it.episodeId == currentEpisodeId }
                                    if (ep != null) {
                                        viewModel.loadMediaSources(
                                            AniMediaQuery(
                                                bangumiId = subjectId,
                                                subjectName = d.displayName,
                                                episodeSort = ep.displayEp,
                                                episodeName = ep.displayName,
                                                episodeId = ep.episodeId,
                                            )
                                        )
                                    }
                                }
                            },
                        ) {
                            AppIcon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "重新检索源",
                            )
                        }
                    },
                )
            }
        },
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (isFullscreen) 0.dp else innerPadding.calculateTopPadding()),
        ) {
            // ---- 播放器 + 控制条 ----
            Box(
                modifier = Modifier
                    .then(
                        if (isFullscreen) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        }
                    )
                    .background(Color.Black)
                    // ★ 横向滑动调节进度: 必须放在 tap 检测**之前**,
                    //   拖动越阈后事件被 drag 消费, tap 自然不会误触发。
                    .pointerInput(detail, currentEpisodeId) {
                        val fullDragMs = 90_000f // 满屏滑一次 = ±90 秒
                        detectHorizontalDragGestures(
                            onDragStart = {
                                if (exoPlayer.duration > 0L) {
                                    isSeekDragging = true
                                    seekPreviewMs = exoPlayer.currentPosition
                                    seekDeltaMs = 0L
                                    controlsVisible = false
                                }
                            },
                            onDragEnd = {
                                if (isSeekDragging) {
                                    val target = seekPreviewMs.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
                                    exoPlayer.seekTo(target)
                                    positionMillis = target
                                    isSeekDragging = false
                                }
                                controlsVisible = true
                            },
                            onDragCancel = {
                                isSeekDragging = false
                                controlsVisible = true
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            if (!isSeekDragging) return@detectHorizontalDragGestures
                            val duration = exoPlayer.duration
                            if (duration <= 0L) return@detectHorizontalDragGestures
                            val delta = (dragAmount * fullDragMs / size.width.coerceAtLeast(1)).toLong()
                            seekDeltaMs += delta
                            seekPreviewMs = (seekPreviewMs + delta).coerceIn(0L, duration)
                        }
                    }
                    .pointerInput(aniSettings.seekStepSeconds, detail) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                            },
                            onDoubleTap = { offset ->
                                // 双击左半屏后退、右半屏前进，步长取番剧设置
                                val stepMs = aniSettings.seekStepSeconds * 1000L
                                val forward = offset.x >= size.width / 2f
                                val target = if (forward) {
                                    (exoPlayer.currentPosition + stepMs)
                                } else {
                                    (exoPlayer.currentPosition - stepMs)
                                }.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
                                exoPlayer.seekTo(target)
                            },
                        )
                    },
            ) {
                AndroidView(
                    factory = { ctx ->
                        // ★ 用 TextureView 渲染（与主站视频播放器同款布局）：
                        //   SurfaceView 是「挖洞」合成，进入全屏容器从 16:9 变满屏、
                        //   再叠加旋转时洞经常对不上位，表现为黑屏有声音；
                        //   TextureView 是普通 View 参与正常合成，转屏/改尺寸都不掉画面。
                        val view = android.view.LayoutInflater.from(ctx).inflate(
                            com.android.purebilibili.R.layout.view_player_texture, null, false,
                        ) as PlayerView
                        view.apply {
                            player = exoPlayer
                            useController = false
                            keepScreenOn = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setKeepContentOnPlayerReset(true)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        }
                    },
                    update = { view ->
                        // 「画面缩放」设置实时同步（播放中切换按钮也走这里）
                        view.resizeMode = resizeMode
                        // 兜底补绑：任何重建/重排后都保证播放器还接在这个 View 上
                        if (view.player != exoPlayer) view.player = exoPlayer
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                if (currentUrl == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AppIcon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                            )
                            AppText(
                                text = when (val st = sourceState) {
                                    is AniSourceState.Loading -> "正在检索可用源…"
                                    is AniSourceState.Success ->
                                        if (st.result.isEmpty) {
                                            // 把每个源的具体结果直接显示出来：
                                            // 「0 条」= 搜到了但没这一集；「HTTP xxx」= 源不可达/被拦。
                                            val perSource = st.result.sourceStatus.joinToString("\n") { s ->
                                                if (s.ok) {
                                                    "${s.sourceName}：${s.count} 条"
                                                } else {
                                                    "${s.sourceName}：${s.message.take(30)}"
                                                }
                                            }
                                            "没有找到可直链播放的源\n" + perSource +
                                                "\n可换一集重试，或点下方「换源」查看"
                                        } else {
                                            "准备播放…"
                                        }
                                    else -> "正在准备…"
                                },
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppText(
                            text = errorMessage ?: "",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                // 自动换源提示（短暂显示，不挡播放）
                failoverNotice?.let { notice ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        AppText(
                            text = notice,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.55f),
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                // ---- 横向滑动进度的中央预览 ----
                if (isSeekDragging) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                        ) {
                            AppText(
                                text = when {
                                    seekDeltaMs > 0 -> "快进 ${seekDeltaMs / 1000} 秒"
                                    seekDeltaMs < 0 -> "快退 ${-seekDeltaMs / 1000} 秒"
                                    else -> "左右滑动调节进度"
                                },
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            AppText(
                                text = "${formatDuration(seekPreviewMs)} / ${formatDuration(durationMillis)}",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }

                // ---- 播放器控制条(点画面显隐) ----
                androidx.compose.animation.AnimatedVisibility(
                    visible = controlsVisible,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    AniPlayerControls(
                        isPlaying = exoPlayer.isPlaying,
                        positionMillis = if (isScrubbing) scrubPosition else positionMillis,
                        durationMillis = durationMillis,
                        isScrubbing = isScrubbing,
                        isFullscreen = isFullscreen,
                        speedLabel = String.format("%.2fx", playbackSpeeds[speedIndex]).let {
                            it.removeSuffix("0x") + if (it.endsWith("x")) "" else "x"
                        },
                        onCycleSpeed = {
                            speedIndex = (speedIndex + 1) % playbackSpeeds.size
                        },
                        onOpenQuality = { showQualitySheet = true },
                        hasNextEpisode = hasNextEpisode,
                        onNextEpisode = { goToNextEpisode() },
                        onPlayPause = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        onScrubStart = {
                            isScrubbing = true
                            scrubPosition = positionMillis
                        },
                        onScrub = { scrubPosition = it },
                        onScrubEnd = { target ->
                            exoPlayer.seekTo(target)
                            // 立刻回写: 轮询 500ms 才更新, 不回写会出现
                            // 「拖完滑块又弹回原处」的迟滞观感
                            positionMillis = target
                            isScrubbing = false
                        },
                        onToggleFullscreen = { isFullscreen = !isFullscreen },
                    )
                }
            }

            // ---- 播放信息 / 换源入口 (全屏时隐藏) ----
            if (!isFullscreen) {
                AppSurface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colorScheme.background,
                ) {
                    Column(modifier = Modifier.padding(AppSpacingTokens.Medium)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val epLabel = episode?.let { "第${it.displayEp}话" } ?: "未选择剧集"
                                AppText(
                                    text = epLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val playingSource = currentSourceName
                                if (playingSource != null) {
                                    AppText(
                                        text = "源：$playingSource",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            AppButton(
                                onClick = { showMagnetSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                containerColor = colorScheme.secondaryContainer,
                                contentColor = colorScheme.onSecondaryContainer,
                            ) {
                                AppIcon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = null,
                                )
                                Spacer(Modifier.width(4.dp))
                                AppText(
                                    text = "磁力源",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            AppButton(
                                onClick = { showSourceSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary,
                            ) {
                                AppIcon(
                                    imageVector = Icons.Outlined.SwapHoriz,
                                    contentDescription = null,
                                )
                                Spacer(Modifier.width(4.dp))
                                AppText(
                                    text = "换源",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }

                        Spacer(Modifier.height(AppSpacingTokens.Medium))

                        // 选集列表
                        // ★ 只显示正片并按集数去重: 服务端的 episodes 会同时带
                        //   MAIN/SP 等类型, 甚至同一集数出现两条(不同 ep/sort 变体),
                        //   造成「两个第一集」的观感
                        val allEpisodes = detail?.episodes.orEmpty()
                            .filter { it.isMain }
                            .distinctBy { it.displayEp }
                            .ifEmpty { detail?.episodes.orEmpty().distinctBy { it.displayEp } }
                        if (allEpisodes.isNotEmpty()) {
                            AniSectionHeader(title = "选集")
                            Spacer(Modifier.height(6.dp))
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(
                                    count = allEpisodes.size,
                                    key = { idx -> allEpisodes[idx].episodeId },
                                ) { idx ->
                                    val ep = allEpisodes[idx]
                                    AniEpisodeChip(
                                        label = ep.displayEp,
                                        selected = ep.episodeId == currentEpisodeId,
                                        onClick = {
                                            currentEpisodeId = ep.episodeId
                                            currentUrl = null
                                            currentSourceName = null
                                            failoverIndex.intValue = 0
                                            autoNextTriggered = false
                                            viewModel.resetMediaSources()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- 画质面板 ----
    if (showQualitySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        // 打开时并发探测其它在线源的分辨率
        LaunchedEffect(sourceState) {
            val cands = (sourceState as? AniSourceState.Success)?.result?.candidates
                ?.filter { it.playableDirectly && it.url != currentUrl }
                .orEmpty()
            cands.forEach { c ->
                if (qualityMap.value[c.url] == null) {
                    qualityMap.value = qualityMap.value + (c.url to AniQualityProbe.probeMaxHeight(c.url))
                }
            }
        }
        ModalBottomSheet(
            onDismissRequest = { showQualitySheet = false },
            sheetState = sheetState,
        ) {
            AniQualitySheetContent(
                currentHeights = exoPlayer.currentTracks.groups
                    .filter { it.type == androidx.media3.common.C.TRACK_TYPE_VIDEO }
                    .flatMap { g -> (0 until g.length).map { g.getTrackFormat(it).height } }
                    .filter { it > 0 }
                    .distinct()
                    .sortedDescending(),
                candidates = (sourceState as? AniSourceState.Success)?.result?.candidates
                    ?.filter { it.playableDirectly }
                    .orEmpty(),
                currentUrl = currentUrl,
                qualityMap = qualityMap.value,
                onSetMaxHeight = { height ->
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .apply {
                            if (height > 0) setMaxVideoSize(Int.MAX_VALUE, height)
                            else clearVideoSizeConstraints()
                        }
                        .build()
                    showQualitySheet = false
                },
                onSelectCandidate = { candidate ->
                    currentSourceName = candidate.sourceName
                    val pickedIndex = failoverCandidates.value.indexOf(candidate)
                    if (pickedIndex >= 0) failoverIndex.intValue = pickedIndex
                    errorMessage = null
                    currentUrl = candidate.url
                    showQualitySheet = false
                },
                onDismiss = { showQualitySheet = false },
            )
        }
    }

    // ---- 换源面板 ----
    if (showSourceSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val st = sourceState
        ModalBottomSheet(
            onDismissRequest = { showSourceSheet = false },
            sheetState = sheetState,
        ) {
            AppText(
                text = "选择播放源",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    start = AppSpacingTokens.Medium,
                    end = AppSpacingTokens.Medium,
                    bottom = AppSpacingTokens.Small,
                ),
            )
            AniSourceSheetContent(
                candidates = (st as? AniSourceState.Success)?.result?.candidates.orEmpty(),
                sourceStatus = (st as? AniSourceState.Success)?.result?.sourceStatus.orEmpty(),
                selectedUrl = currentUrl,
                loading = st is AniSourceState.Loading,
                modifier = Modifier.height(420.dp),
                onSelect = { candidate: AniMediaCandidate ->
                    if (candidate.playableDirectly) {
                        currentSourceName = candidate.sourceName
                        preferredSourceId = candidate.sourceId.takeIf { it.isNotBlank() }
                        // 同步换源游标：之后若这个源也播不了，从用户选的这个位置继续往后切
                        val pickedIndex = failoverCandidates.value.indexOf(candidate)
                        if (pickedIndex >= 0) failoverIndex.intValue = pickedIndex
                        errorMessage = null
                        currentUrl = candidate.url
                    } else {
                        // 种子/磁力不能交给播放器，直接说明原因，避免切过去黑屏
                        errorMessage = "该来源是 BT/磁力资源，本播放器无法直接播放；" +
                            "请复制链接交给外部 BT 客户端下载后再观看"
                    }
                    showSourceSheet = false
                },
            )
        }
    }

    // ---- 磁力/种子源面板(只提供搜索与复制链接, 本 App 不做下载) ----
    if (showMagnetSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val btState by viewModel.downloadSourceState.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = { showMagnetSheet = false },
            sheetState = sheetState,
        ) {
            AniMagnetSheetContent(
                btState = btState,
                onLoadBt = {
                    detail?.let { d ->
                        val ep = d.episodes.firstOrNull { it.episodeId == currentEpisodeId }
                            ?: d.episodes.firstOrNull { it.isMain }
                            ?: d.episodes.firstOrNull()
                        if (ep != null) {
                            viewModel.loadDownloadSources(
                                AniMediaQuery(
                                    bangumiId = subjectId,
                                    subjectName = d.displayName,
                                    episodeSort = ep.displayEp,
                                    episodeName = ep.displayName,
                                    episodeId = ep.episodeId,
                                    mikanId = null,
                                )
                            )
                        }
                    }
                },
            )
        }
    }
}

/**
 * 播放器进度条。
 *
 * ★ 应用户要求: 点击轨道任意位置**直接跳转**(Material3 的 Slider 只能拖 thumb,
 *   点轨道不动), 同时保留按住拖动的连续调节。
 * 交互: 按下即 seek 到该点 → 拖动持续更新 → 松手提交并回写进度。
 */
@Composable
private fun AniSeekBar(
    positionMillis: Long,
    durationMillis: Long,
    onSeekStart: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekEnd: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = durationMillis > 0L
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableLongStateOf(0L) }

    fun msAt(x: Float, width: Int): Long {
        if (width <= 0) return 0L
        return ((x / width).coerceIn(0f, 1f) * durationMillis).toLong()
    }

    Box(
        modifier = modifier
            .height(28.dp)
            .pointerInput(enabled, durationMillis) {
                if (!enabled) return@pointerInput
                // 按下即跳转(点击轨道也能生效)
                detectTapGestures(
                    onPress = { offset ->
                        scrubbing = true
                        scrubValue = msAt(offset.x, size.width)
                        onSeekStart()
                        onSeek(scrubValue)
                        tryAwaitRelease()
                        onSeekEnd(scrubValue)
                        scrubbing = false
                    },
                )
            }
            .pointerInput(enabled, durationMillis) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        scrubbing = true
                        scrubValue = msAt(offset.x, size.width)
                        onSeekStart()
                        onSeek(scrubValue)
                    },
                    onDragEnd = {
                        onSeekEnd(scrubValue)
                        scrubbing = false
                    },
                    onDragCancel = {
                        onSeekEnd(scrubValue)
                        scrubbing = false
                    },
                ) { change, _ ->
                    change.consume()
                    scrubValue = msAt(change.position.x, size.width)
                    onSeek(scrubValue)
                }
            },
    ) {
        val shown = if (scrubbing) scrubValue else positionMillis
        val fraction = if (durationMillis > 0L) {
            (shown.toFloat() / durationMillis).coerceIn(0f, 1f)
        } else {
            0f
        }
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val trackH = 4.dp.toPx()
            val thumbR = if (scrubbing) 9.dp.toPx() else 6.5f.dp.toPx()
            // 底轨
            drawRoundRect(
                color = Color.White.copy(alpha = 0.28f),
                topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - trackH / 2f),
                size = androidx.compose.ui.geometry.Size(size.width, trackH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2f),
            )
            // 已播进度
            val played = size.width * fraction
            if (played > 0f) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - trackH / 2f),
                    size = androidx.compose.ui.geometry.Size(played, trackH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2f),
                )
            }
            // 圆点
            drawCircle(
                color = Color.White,
                radius = thumbR,
                center = androidx.compose.ui.geometry.Offset(played.coerceIn(0f, size.width), centerY),
            )
        }
    }
}

/** 无涟漪点击(控制条上的倍速按钮用)。 */
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
            indication = null,
            onClick = onClick,
        )
    )

/** 下载面板的可折叠区块标题(默认收起, 点标题展开)。 */
@Composable
private fun AniDownloadSectionHeader(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick)
            .padding(horizontal = AppSpacingTokens.Medium, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        AppText(
            text = if (expanded) "收起 ▾" else "展开 ▸",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * 「画质」面板:
 *   1. 当前流是多档 HLS master 时, 列出各档分辨率直接切(TrackSelection 限高);
 *   2. 单码率源(实测 maccms 大多如此)则列出其它在线源的实测分辨率,
 *      点选即换源 —— 跨源切换清晰度。
 */
@Composable
private fun AniQualitySheetContent(
    currentHeights: List<Int>,
    candidates: List<AniMediaCandidate>,
    currentUrl: String?,
    qualityMap: Map<String, Int>,
    onSetMaxHeight: (Int) -> Unit,
    onSelectCandidate: (AniMediaCandidate) -> Unit,
    onDismiss: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        AppText(
            text = "当前画面档位",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = AppSpacingTokens.Medium),
        )
        Spacer(Modifier.height(4.dp))
        if (currentHeights.size > 1) {
            // 多档 master playlist: 自动 + 各档
            Row(
                modifier = Modifier.padding(horizontal = AppSpacingTokens.Medium),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppButton(
                    onClick = { onSetMaxHeight(0) },
                    shape = RoundedCornerShape(50),
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                ) {
                    AppText(text = "自动", style = MaterialTheme.typography.labelMedium)
                }
                currentHeights.forEach { h ->
                    AppButton(
                        onClick = { onSetMaxHeight(h) },
                        shape = RoundedCornerShape(50),
                        containerColor = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer,
                    ) {
                        AppText(text = "${h}P", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        } else {
            AppText(
                text = "当前源是单码率流(" +
                    (currentHeights.firstOrNull()?.let { "${it}P" } ?: "分辨率未知") +
                    "), 无档位可切。想要更高/更低画质可换下面的源。",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppSpacingTokens.Medium),
            )
        }

        Spacer(Modifier.height(AppSpacingTokens.Small))
        AppText(
            text = "其它源画质 (点选即换源)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = AppSpacingTokens.Medium),
        )
        Spacer(Modifier.height(4.dp))
        val others = candidates.filter { it.url != currentUrl }
        if (others.isEmpty()) {
            AppText(
                text = "没有其它可切换的在线源",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppSpacingTokens.Medium),
            )
        } else {
            others.forEach { candidate ->
                val h = qualityMap[candidate.url]
                val tier = AniVideoTier(width = 0, height = h ?: 0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacingTokens.Medium, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AppText(
                            text = candidate.sourceName,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AppText(
                            text = "画质: " + when {
                                h == null -> "探测中…"
                                h > 0 -> tier.label
                                else -> "未知"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                    AppTextButton(
                        onClick = { onSelectCandidate(candidate) },
                        enabled = h == null || h > 0,
                    ) {
                        AppText(text = "切换")
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** 起播看门狗：URL 生效后多久仍未进入 READY 就判定该源不可用并自动换源。 */
private const val SOURCE_FAILOVER_WATCHDOG_MS = 20_000L

/**
 * 「磁力/种子源」面板内容。
 *
 * ★ v4.2.0: 番剧缓存下载功能已移除 —— 本 App 不再做下载,
 *   这里只检索 BT/磁力源, 供用户**复制链接**交给外部 BT 客户端。
 */
@Composable
private fun AniMagnetSheetContent(
    btState: AniSourceState,
    onLoadBt: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
        as? android.content.ClipboardManager
    var btSourceExpanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        AniDownloadSectionHeader(
            title = "BT / 磁力源 (复制链接, 交给外部 BT 客户端)",
            expanded = btSourceExpanded,
            onClick = { btSourceExpanded = !btSourceExpanded },
        )
        if (btSourceExpanded) {

            when (val bt = btState) {
                is AniSourceState.Idle -> {
                    AppText(
                        text = "磁力/种子源不参与在线播放, 仅供复制链接",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = AppSpacingTokens.Medium),
                    )
                    AppTextButton(
                        onClick = onLoadBt,
                        modifier = Modifier.padding(horizontal = AppSpacingTokens.Small),
                    ) {
                        AppText(text = "搜索 BT / 磁力源")
                    }
                }

                is AniSourceState.Loading -> AppText(
                    text = "正在检索 BT 源…",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(AppSpacingTokens.Medium),
                )

                is AniSourceState.Error -> AppText(
                    text = "BT 源检索失败：${bt.message}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.error,
                    modifier = Modifier.padding(AppSpacingTokens.Medium),
                )

                is AniSourceState.Success -> {
                    if (bt.result.isEmpty) {
                        AppText(
                            text = "BT 源没有搜到这一集",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(AppSpacingTokens.Medium),
                        )
                    } else {
                        bt.result.candidates.forEach { candidate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppSpacingTokens.Medium, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    // ★ 完整显示标题 —— 分辨率/字幕组信息就在标题里,
                                    //   截断后用户无法判断该下哪一个
                                    AppText(
                                        text = candidate.title,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    AppText(
                                        text = listOfNotNull(
                                            candidate.quality.ifBlank { null },
                                            candidate.subtitleGroup.ifBlank { null },
                                            candidate.sizeLabel.ifBlank { null },
                                            if (candidate.seeders >= 0) "做种 ${candidate.seeders}" else null,
                                        ).joinToString(" · ").ifBlank { candidate.sourceName },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant,
                                    )
                                }
                                AppTextButton(onClick = {
                                    clipboard?.setPrimaryClip(
                                        android.content.ClipData.newPlainText("url", candidate.url)
                                    )
                                    android.widget.Toast.makeText(
                                        context, "已复制链接", android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                }) {
                                    AppText(text = "复制")
                                }
                                AppTextButton(onClick = {
                                    runCatching {
                                        context.startActivity(
                                            android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(candidate.url),
                                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }.onFailure {
                                        android.widget.Toast.makeText(
                                            context, "没有可处理磁力的应用", android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }) {
                                    AppText(text = "外部下载")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * 播放器底部控制条: 播放/暂停 + 可拖动进度条 + 全屏。
 *
 * 全屏与半屏共用; [isScrubbing] 为 true 时进度条完全由手指驱动,
 * 播放进度的回写不会把滑块拽走。
 */
@Composable
private fun AniPlayerControls(
    isPlaying: Boolean,
    positionMillis: Long,
    durationMillis: Long,
    isScrubbing: Boolean,
    isFullscreen: Boolean,
    speedLabel: String,
    onCycleSpeed: () -> Unit,
    onOpenQuality: () -> Unit,
    hasNextEpisode: Boolean,
    onNextEpisode: () -> Unit,
    onPlayPause: () -> Unit,
    onScrubStart: () -> Unit,
    onScrub: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    val safeDuration = durationMillis.coerceAtLeast(0L)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                )
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        // 进度条: 自绘, 点击任意位置直接跳转 + 按住拖动(对齐主流播放器交互)
        AniSeekBar(
            positionMillis = positionMillis,
            durationMillis = safeDuration,
            onSeekStart = onScrubStart,
            onSeek = onScrub,
            onSeekEnd = onScrubEnd,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 播放/暂停
            AppIconButton(onClick = onPlayPause) {
                AppIcon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                )
            }
            // 时间
            AppText(
                text = "${formatDuration(positionMillis)} / ${formatDuration(safeDuration)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 2.dp),
            )
            Spacer(Modifier.weight(1f))
            // 倍速: 点一下切到下一档(0.5 -> 0.75 -> 1 -> 1.25 -> 1.5 -> 2 -> 0.5)
            AppText(
                text = speedLabel,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickableNoRipple(onClick = onCycleSpeed)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(6.dp))
            // 下一集(复用当前源, 不必重新搜索)
            if (hasNextEpisode) {
                AppIconButton(onClick = onNextEpisode) {
                    AppIcon(
                        imageVector = Icons.Outlined.SkipNext,
                        contentDescription = "下一集",
                        tint = Color.White,
                    )
                }
            }
            // 画质(多档 HLS 可切档; 否则列出其它源画质点选换源)
            AppText(
                text = "画质",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickableNoRipple(onClick = onOpenQuality)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(6.dp))
            // 全屏 / 退出全屏
            AppIconButton(onClick = onToggleFullscreen) {
                AppIcon(
                    imageVector = if (isFullscreen) {
                        Icons.Outlined.FullscreenExit
                    } else {
                        Icons.Outlined.Fullscreen
                    },
                    contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                    tint = Color.White,
                )
            }
        }
    }
}

/** 直链播放的 UA; 不用 D 站 UA, 用通用播放器 UA。 */
private const val MEDIA_SOURCE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

/**
 * 番剧播放器专用解码器选择器 —— 对应「优先硬件解码」设置。
 *
 * 开启时用 media3 默认策略（硬解优先，失败自动回退软解）；
 * 关闭时改用 media3 内置的 PREFER_SOFTWARE，纯软解跑，
 * 某些硬解兼容性差的片源可以靠它救回来。
 *
 * 该设置在使用「番剧设置」页改完后，下次进入播放器生效。
 */
class AniMediaCodecSelector(
    private val preferHardware: Boolean,
) : MediaCodecSelector {

    @Throws(MediaCodecUtil.DecoderQueryException::class)
    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean,
    ): List<MediaCodecInfo> {
        if (preferHardware) {
            return MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder,
            )
        }
        val software = MediaCodecSelector.PREFER_SOFTWARE.getDecoderInfos(
            mimeType,
            requiresSecureDecoder,
            requiresTunnelingDecoder,
        )
        // 没有软解可用时退回完整列表，避免直接播不了
        return software.ifEmpty {
            MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder,
            )
        }
    }
}

/** 毫秒 -> mm:ss。 */
private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
