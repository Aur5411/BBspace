package com.android.purebilibili.core.ui.performance

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 当前窗口的实际刷新率（Hz）。
 *
 * Compose 的动画本身是「按时间」而非「按帧」推进的，所以高刷屏上动画天然跑得更顺；
 * 这个值主要用于：
 * 1. 设置页显示「本机最高 xxx Hz / 当前 xxx Hz」，让用户能确认适配是否生效；
 * 2. 少数需要按帧数换算时长或采样间隔的地方（如跟手位移的帧预算）。
 *
 * 拿不到时给 60f，即最保守的假设。
 */
val LocalDisplayRefreshRate = staticCompositionLocalOf { 60f }

/** 设备支持的最高刷新率（Hz），拿不到时为 null。 */
val LocalDisplayMaxRefreshRate = staticCompositionLocalOf<Float?> { null }

/** 提供一个常量值（如预览、测试）。 */
val LocalDisplayRefreshRateOverride = compositionLocalOf<Float?> { null }

@Composable
fun ProvideDisplayRefreshRate(
    refreshRate: Float,
    maxRefreshRate: Float? = null,
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalDisplayRefreshRate provides refreshRate,
        LocalDisplayMaxRefreshRate provides maxRefreshRate,
        LocalDisplayRefreshRateOverride provides refreshRate,
        content = content,
    )
}

/**
 * 监听窗口实际刷新率。系统切换刷新率（如 60↔120 的省电切换、系统设置改动）时会自动更新。
 */
@Composable
fun rememberDisplayRefreshRate(activity: Activity?): Float {
    val initial = remember(activity) { readCurrentRefreshRate(activity) }
    var rate by remember(activity) { mutableFloatStateOf(initial) }

    DisposableEffect(activity) {
        val manager = activity?.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        if (activity == null || manager == null) {
            onDispose { }
        } else {
            val listener = object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) = Unit
                override fun onDisplayRemoved(displayId: Int) = Unit
                override fun onDisplayChanged(displayId: Int) {
                    val updated = readCurrentRefreshRate(activity)
                    if (updated > 0f) rate = updated
                }
            }
            manager.registerDisplayListener(listener, null)
            onDispose { runCatching { manager.unregisterDisplayListener(listener) } }
        }
    }
    return rate
}

private fun readCurrentRefreshRate(activity: Activity?): Float {
    if (activity == null) return 60f
    val display = resolveActivityDisplay(activity) ?: return 60f
    return runCatching { display.refreshRate }
        .getOrNull()
        ?.takeIf { it.isFinite() && it > 0f }
        ?: 60f
}

/** 设备支持的最高刷新率。 */
@Composable
fun rememberDisplayMaxRefreshRate(activity: Activity?): Float? = remember(activity) {
    val display = resolveActivityDisplay(activity) ?: return@remember null
    runCatching { display.supportedModes.maxOfOrNull { it.refreshRate } }
        .getOrNull()
        ?.takeIf { it.isFinite() && it > 0f }
}

private fun resolveActivityDisplay(activity: Activity?): Display? {
    if (activity == null) return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.display
    } else {
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay
    }
}

/** 便捷读取：优先取 override，其次取实际监听值。 */
@Composable
fun currentDisplayRefreshRate(): Float =
    LocalDisplayRefreshRateOverride.current ?: LocalDisplayRefreshRate.current

/** 判断是否跑在高刷（≥90Hz）。 */
@Composable
fun isHighRefreshRateActive(): Boolean = currentDisplayRefreshRate() >= MIN_HIGH_REFRESH_RATE

/** 非 Compose 场景读取当前刷新率。 */
fun readDisplayRefreshRate(context: Context?): Float {
    if (context == null) return 60f
    val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return 60f
    val display = runCatching { manager.getDisplay(Display.DEFAULT_DISPLAY) }.getOrNull()
        ?: return 60f
    return runCatching { display.refreshRate }.getOrNull()?.takeIf { it > 0f } ?: 60f
}
