// 文件路径: feature/anime/AniSettingsStore.kt
//
// ★★ 番剧模块「自有设置」存储 ★★
//
// 设计原则（用户明确要求）:
//   1. 番剧播放器 / 弹幕 / 数据源完全独立，**不受主 App 设置影响**；
//   2. 因此这里不复用 SettingsManager、不读主 App 的 SharedPreferences 键；
//   3. 用番剧自己的 SharedPreferences 文件 ("ani_settings")，只服务番剧页；
//   4. 只保留「番剧页真的用得上」的项 —— 每一项都有明确的消费方，不做摆设开关。
//
// 与主 App 的边界: 本文件只有 feature/anime 下的代码读取，其他模块一律不引用。
package com.android.purebilibili.feature.anime

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** 番剧播放器自适应模式（对应 media3 AspectRatioFrameLayout 的三种 RESIZE_MODE）。 */
enum class AniResizeMode(val label: String) {
    FIT("适应"),
    FILL("填充"),
    ZOOM("裁切"),
    ;

    /** 映射到 androidx.media3.ui.AspectRatioFrameLayout 的常量值。 */
    fun toMedia3ResizeMode(): Int = when (this) {
        FIT -> 0 // RESIZE_MODE_FIT
        FILL -> 3 // RESIZE_MODE_FILL
        ZOOM -> 4 // RESIZE_MODE_ZOOM
    }

    /** 播放器内点击切换时的下一档。 */
    fun next(): AniResizeMode = entries[(ordinal + 1) % entries.size]
}

/** 番剧偏好设置快照。 */
data class AniSettings(
    // ── 播放 ──
    val autoPlayNextEpisode: Boolean = true,
    val rememberPlaybackPosition: Boolean = true,
    val defaultResizeMode: AniResizeMode = AniResizeMode.FIT,
    val seekStepSeconds: Int = 10,
    val hardwareDecodePreferred: Boolean = true,
    // ── 弹幕 ──
    val danmakuEnabledByDefault: Boolean = true,
    val danmakuOpacityPercent: Int = 85,
    val danmakuSpeedPxPerSecond: Int = 180,
    val danmakuTextScalePercent: Int = 100,
    /** 弹幕显示区域：只占画面顶部这个百分比的高度。 */
    val danmakuDisplayAreaPercent: Int = 100,
    val danmakuBlockTop: Boolean = false,
    val danmakuBlockBottom: Boolean = false,
    // ── 内容 ──
    val autoLoadCharacters: Boolean = true,
    /** 详情页是否展示「制作人员」区块。 */
    val showStaff: Boolean = true,
    /** 详情页是否展示「短评」区块。 */
    val showReviews: Boolean = true,
    val searchResultLimit: Int = 30,
    val coverBlurEnabled: Boolean = false,
    // ── 数据 ──
    val recordWatchHistory: Boolean = true,
    val historyMaxCount: Int = 200,
) {
    companion object {
        /** 各字段合法区间，UI 与读档都过这一层，避免脏值导致崩溃。 */
        val SEEK_STEP_RANGE = 5..60
        val DANMAKU_OPACITY_RANGE = 20..100
        val DANMAKU_SPEED_RANGE = 80..400
        val DANMAKU_SCALE_RANGE = 60..160
        val DANMAKU_AREA_RANGE = 40..100
        val SEARCH_LIMIT_RANGE = 10..50
        val HISTORY_MAX_RANGE = 50..1000

        /** 弹幕字号基准值（px），乘以用户百分比后传给弹幕层。 */
        const val DANMAKU_BASE_TEXT_PX = 40f
    }

    /** 弹幕层实际使用的字号（px）。 */
    val danmakuTextPx: Float
        get() = DANMAKU_BASE_TEXT_PX * danmakuTextScalePercent / 100f

    /** 弹幕层实际使用的不透明度 0..1。 */
    val danmakuAlpha: Float
        get() = danmakuOpacityPercent / 100f
}

/**
 * 番剧设置的持久化实现。
 *
 * 用 `object` + 独立 prefs 文件，保证和主 App 设置**物理隔离**。
 */
object AniSettingsStore {

    private const val PREFS_NAME = "ani_settings"

    private const val KEY_AUTO_NEXT = "auto_play_next_episode"
    private const val KEY_REMEMBER_POS = "remember_playback_position"
    private const val KEY_RESIZE_MODE = "default_resize_mode"
    private const val KEY_SEEK_STEP = "seek_step_seconds"
    private const val KEY_HW_DECODE = "hardware_decode_preferred"
    private const val KEY_DANMAKU_ENABLED = "danmaku_enabled_by_default"
    private const val KEY_DANMAKU_OPACITY = "danmaku_opacity_percent"
    private const val KEY_DANMAKU_SPEED = "danmaku_speed_px_per_second"
    private const val KEY_DANMAKU_SCALE = "danmaku_text_scale_percent"
    private const val KEY_DANMAKU_AREA = "danmaku_display_area_percent"
    private const val KEY_DANMAKU_BLOCK_TOP = "danmaku_block_top"
    private const val KEY_DANMAKU_BLOCK_BOTTOM = "danmaku_block_bottom"
    private const val KEY_AUTO_CHARACTERS = "auto_load_characters"
    private const val KEY_SHOW_STAFF = "show_staff_section"
    private const val KEY_SHOW_REVIEWS = "show_reviews_section"
    private const val KEY_SEARCH_LIMIT = "search_result_limit"
    private const val KEY_COVER_BLUR = "cover_blur_enabled"
    private const val KEY_RECORD_HISTORY = "record_watch_history"
    private const val KEY_HISTORY_MAX = "history_max_count"

    private val _settings = MutableStateFlow(AniSettings())
    /** 供非 Compose 场景（如播放器）直接读取的实时流。 */
    val settings: StateFlow<AniSettings> = _settings.asStateFlow()

    @Volatile
    private var prefs: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences {
        val cached = prefs
        if (cached != null) return cached
        synchronized(this) {
            val existing = prefs
            if (existing != null) return existing
            val created = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = created
            _settings.value = readFrom(created)
            return created
        }
    }

    /** 首次进入番剧任一页面时调用一次即可（幂等）。 */
    fun ensureLoaded(context: Context) {
        prefs(context)
    }

    fun current(context: Context): AniSettings {
        prefs(context)
        return _settings.value
    }

    /** 同步取一次快照（非 Compose 场景，如 ViewModel）。 */
    fun snapshot(context: Context): AniSettings = current(context)

    /** 读盘 + 区间裁剪。 */
    private fun readFrom(p: SharedPreferences): AniSettings {
        val defaults = AniSettings()
        return AniSettings(
            autoPlayNextEpisode = p.getBoolean(KEY_AUTO_NEXT, defaults.autoPlayNextEpisode),
            rememberPlaybackPosition = p.getBoolean(KEY_REMEMBER_POS, defaults.rememberPlaybackPosition),
            defaultResizeMode = enumOrDefault(
                raw = p.getString(KEY_RESIZE_MODE, null),
                fallback = defaults.defaultResizeMode,
            ),
            seekStepSeconds = p.getInt(KEY_SEEK_STEP, defaults.seekStepSeconds)
                .coerceIn(AniSettings.SEEK_STEP_RANGE),
            hardwareDecodePreferred = p.getBoolean(KEY_HW_DECODE, defaults.hardwareDecodePreferred),
            danmakuEnabledByDefault = p.getBoolean(KEY_DANMAKU_ENABLED, defaults.danmakuEnabledByDefault),
            danmakuOpacityPercent = p.getInt(KEY_DANMAKU_OPACITY, defaults.danmakuOpacityPercent)
                .coerceIn(AniSettings.DANMAKU_OPACITY_RANGE),
            danmakuSpeedPxPerSecond = p.getInt(KEY_DANMAKU_SPEED, defaults.danmakuSpeedPxPerSecond)
                .coerceIn(AniSettings.DANMAKU_SPEED_RANGE),
            danmakuTextScalePercent = p.getInt(KEY_DANMAKU_SCALE, defaults.danmakuTextScalePercent)
                .coerceIn(AniSettings.DANMAKU_SCALE_RANGE),
            danmakuDisplayAreaPercent = p.getInt(KEY_DANMAKU_AREA, defaults.danmakuDisplayAreaPercent)
                .coerceIn(AniSettings.DANMAKU_AREA_RANGE),
            danmakuBlockTop = p.getBoolean(KEY_DANMAKU_BLOCK_TOP, defaults.danmakuBlockTop),
            danmakuBlockBottom = p.getBoolean(KEY_DANMAKU_BLOCK_BOTTOM, defaults.danmakuBlockBottom),
            autoLoadCharacters = p.getBoolean(KEY_AUTO_CHARACTERS, defaults.autoLoadCharacters),
            showStaff = p.getBoolean(KEY_SHOW_STAFF, defaults.showStaff),
            showReviews = p.getBoolean(KEY_SHOW_REVIEWS, defaults.showReviews),
            searchResultLimit = p.getInt(KEY_SEARCH_LIMIT, defaults.searchResultLimit)
                .coerceIn(AniSettings.SEARCH_LIMIT_RANGE),
            coverBlurEnabled = p.getBoolean(KEY_COVER_BLUR, defaults.coverBlurEnabled),
            recordWatchHistory = p.getBoolean(KEY_RECORD_HISTORY, defaults.recordWatchHistory),
            historyMaxCount = p.getInt(KEY_HISTORY_MAX, defaults.historyMaxCount)
                .coerceIn(AniSettings.HISTORY_MAX_RANGE),
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T =
        runCatching { if (raw.isNullOrBlank()) fallback else enumValueOf<T>(raw) }
            .getOrDefault(fallback)

    /** 整体写回（原子：一次 commit 写全部键）。 */
    private fun write(context: Context, value: AniSettings) {
        prefs(context).edit()
            .putBoolean(KEY_AUTO_NEXT, value.autoPlayNextEpisode)
            .putBoolean(KEY_REMEMBER_POS, value.rememberPlaybackPosition)
            .putString(KEY_RESIZE_MODE, value.defaultResizeMode.name)
            .putInt(KEY_SEEK_STEP, value.seekStepSeconds)
            .putBoolean(KEY_HW_DECODE, value.hardwareDecodePreferred)
            .putBoolean(KEY_DANMAKU_ENABLED, value.danmakuEnabledByDefault)
            .putInt(KEY_DANMAKU_OPACITY, value.danmakuOpacityPercent)
            .putInt(KEY_DANMAKU_SPEED, value.danmakuSpeedPxPerSecond)
            .putInt(KEY_DANMAKU_SCALE, value.danmakuTextScalePercent)
            .putInt(KEY_DANMAKU_AREA, value.danmakuDisplayAreaPercent)
            .putBoolean(KEY_DANMAKU_BLOCK_TOP, value.danmakuBlockTop)
            .putBoolean(KEY_DANMAKU_BLOCK_BOTTOM, value.danmakuBlockBottom)
            .putBoolean(KEY_AUTO_CHARACTERS, value.autoLoadCharacters)
            .putBoolean(KEY_SHOW_STAFF, value.showStaff)
            .putBoolean(KEY_SHOW_REVIEWS, value.showReviews)
            .putInt(KEY_SEARCH_LIMIT, value.searchResultLimit)
            .putBoolean(KEY_COVER_BLUR, value.coverBlurEnabled)
            .putBoolean(KEY_RECORD_HISTORY, value.recordWatchHistory)
            .putInt(KEY_HISTORY_MAX, value.historyMaxCount)
            .apply()
        _settings.value = value
    }

    /** 局部更新（UI 调这个）。 */
    fun update(context: Context, transform: (AniSettings) -> AniSettings) {
        write(context, transform(current(context)))
    }

    /** 恢复番剧默认设置。 */
    fun reset(context: Context) {
        write(context, AniSettings())
    }
}

/**
 * Compose 侧读取 + 写入番剧设置。
 *
 * 用法：
 * ```
 * val (aniSettings, updateAniSettings) = rememberAniSettings()
 * ```
 */
@Composable
fun rememberAniSettings(): Pair<AniSettings, ((AniSettings) -> AniSettings) -> Unit> {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf(AniSettingsStore.current(context)) }
    androidx.compose.runtime.LaunchedEffect(context) {
        AniSettingsStore.ensureLoaded(context)
        AniSettingsStore.settings.collect { latest ->
            snapshot = latest
        }
    }
    val updater: ((AniSettings) -> AniSettings) -> Unit = remember(context) {
        { transform -> AniSettingsStore.update(context, transform) }
    }
    return snapshot to updater
}

/** 只读订阅（播放器内部用，避免误写）。 */
@Composable
fun rememberAniSettingsState(): AniSettings {
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(context) { AniSettingsStore.ensureLoaded(context) }
    return AniSettingsStore.settings
        .map { it }
        .collectAsState(initial = AniSettingsStore.current(context))
        .value
}
