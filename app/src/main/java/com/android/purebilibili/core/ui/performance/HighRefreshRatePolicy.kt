package com.android.purebilibili.core.ui.performance

/**
 * 全局高刷新率适配策略。
 *
 * 背景：Android 默认按「系统设置 + 应用内容」来决定刷新率。很多 ROM 会在
 * 普通列表/滑动场景把刷新率压回 60Hz，即使屏幕是 120Hz —— 结果就是
 * 滑动、转场、跟手动画全都只有 60 帧。要让整个 App 真正跑满高刷，
 * 必须在窗口层面显式声明 preferredDisplayModeId。
 *
 * 这里把「选哪个 mode」抽成纯函数，便于单测覆盖各家 ROM 的模式表形状。
 */

/**
 * 目标刷新率档位。
 *
 * - [SYSTEM]：完全不干预窗口，交回系统决定（省电优先/老设备兜底）
 * - [HZ90]：锁定 90Hz
 * - [HZ120]：锁定 120Hz（设备没有 120 时退化为可用最高档）
 * - [MAX]：不指定具体数值，直接选设备支持的最高档（≥ [MIN_HIGH_REFRESH_RATE]）
 */
enum class HighRefreshRateTarget(
    val storageValue: String,
    val requestedRefreshRate: Float?,
) {
    SYSTEM("system", null),
    HZ90("90", 90f),
    HZ120("120", 120f),
    MAX("max", null),
    ;

    /** 是否需要真的去改窗口参数。 */
    val takesEffect: Boolean get() = this != SYSTEM

    /** 设置页副标题里展示的档位名。 */
    val label: String
        get() = when (this) {
            SYSTEM -> "跟随系统"
            HZ90 -> "90Hz"
            HZ120 -> "120Hz"
            MAX -> "最高可用"
        }

    companion object {
        /**
         * 默认「最高可用」：用户装完就是满血高刷，不需要自己去设置里翻。
         * 不支持高刷的设备上 [resolveHighRefreshModeId] 会返回 null，等于没开。
         */
        val DEFAULT: HighRefreshRateTarget = MAX

        fun fromStorage(value: String?): HighRefreshRateTarget =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

/** 一个显示模式（分辨率 + 刷新率）的抽象，方便脱离 Android framework 单测。 */
data class DisplayRefreshMode(
    val modeId: Int,
    val refreshRate: Float,
    val width: Int,
    val height: Int,
)

/** 低于这个刷新率就不算「高刷」，避免把 75Hz 当卖点。 */
const val MIN_HIGH_REFRESH_RATE: Float = 90f

/** 判定「精确命中」某个目标刷新率时的容差；厂商上报值常见 119.99 / 120.00001。 */
private const val REFRESH_RATE_EPSILON: Float = 1.5f

/**
 * 在设备支持的模式表里挑出应当写入 `preferredDisplayModeId` 的 modeId。
 *
 * 返回 null 表示「不要动窗口」，调用方应保持系统当前行为。
 *
 * 挑选顺序（全部限制在同一分辨率内，避免改刷新率时顺带把画面缩放/裁切）：
 * 1. 只在和当前模式**分辨率相同**的候选里挑 —— 换分辨率会导致重排版甚至缩放模糊。
 * 2. 明确档位（90/120）：优先精确命中该刷新率；没有则退化为「≥90 里最高」，
 *    这样 90Hz 屏选 120Hz 也能拿到它最好的档，而不是直接摆烂回 60。
 * 3. 最高档（MAX）：直接取 ≥90 里最高。
 * 4. 同分时优先复用当前 modeId，避免无谓的窗口重配触发一次闪烁。
 */
fun resolveHighRefreshModeId(
    currentModeId: Int,
    supportedModes: List<DisplayRefreshMode>,
    target: HighRefreshRateTarget,
    minRefreshRate: Float = MIN_HIGH_REFRESH_RATE,
): Int? {
    if (!target.takesEffect) return null
    if (supportedModes.isEmpty()) return null

    val currentMode = supportedModes.firstOrNull { it.modeId == currentModeId }
        // 拿不到当前模式时退化为全表挑选：总比什么都不做强。
        ?: return supportedModes
            .filter { it.refreshRate >= minRefreshRate }
            .let(::pickHighest)
            ?.modeId

    val sameResolution = supportedModes.filter {
        it.width == currentMode.width && it.height == currentMode.height
    }
    val pool = sameResolution.ifEmpty { supportedModes }

    val requested = target.requestedRefreshRate
    if (requested != null) {
        val exact = pool.filter { kotlin.math.abs(it.refreshRate - requested) <= REFRESH_RATE_EPSILON }
        pickHighest(exact)?.let { return it.modeId }
    }

    val highTier = pool.filter { it.refreshRate >= minRefreshRate }
    return pickHighest(highTier)?.modeId
}

/**
 * 当前模式下能拿到的实际刷新率，用于设置页展示「实际生效」。
 * 返回 null 表示拿不到（模式表为空或当前模式不在表内）。
 */
fun resolveCurrentDisplayRefreshRate(
    currentModeId: Int,
    supportedModes: List<DisplayRefreshMode>,
): Float? = supportedModes.firstOrNull { it.modeId == currentModeId }?.refreshRate

private fun pickHighest(candidates: List<DisplayRefreshMode>): DisplayRefreshMode? =
    if (candidates.isEmpty()) null
    else candidates.maxWithOrNull(
        compareBy<DisplayRefreshMode> { it.refreshRate }
            .thenBy { it.width * it.height }
    )
