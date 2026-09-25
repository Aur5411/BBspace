package com.android.purebilibili.core.ui.performance

import android.app.Activity
import android.os.Build
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

/**
 * 在窗口层面显式声明高刷新率。
 *
 * 只在 [target] 需要生效、且设备确实支持 ≥90Hz 时才动窗口参数，
 * 否则原样放手（比写一个数值更安全：写错会导致部分 ROM 黑屏或掉回最低档）。
 *
 * 离开时**恢复原值**，避免把宿主 Activity 的窗口配置污染给后续页面。
 */
@Composable
fun ApplyHighRefreshRateEffect(
    activity: Activity?,
    target: HighRefreshRateTarget,
    enabled: Boolean = true,
) {
    DisposableEffect(activity, target, enabled) {
        val host = activity
        if (host == null || !enabled || !target.takesEffect) {
            onDispose { }
        } else {
            val window: Window = host.window
            val modeId = resolveWindowHighRefreshModeId(host, target)
            if (modeId == null) {
                onDispose { }
            } else {
                val originalModeId = window.attributes.preferredDisplayModeId
                if (modeId != originalModeId) {
                    runCatching {
                        window.attributes = window.attributes.apply {
                            preferredDisplayModeId = modeId
                        }
                    }
                }
                onDispose {
                    runCatching {
                        if (window.attributes.preferredDisplayModeId != originalModeId) {
                            window.attributes = window.attributes.apply {
                                preferredDisplayModeId = originalModeId
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 读设备模式表并挑选目标 modeId。抽成非 Composable 以便在别处复用（如诊断日志）。
 */
fun resolveWindowHighRefreshModeId(
    activity: Activity,
    target: HighRefreshRateTarget,
): Int? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.display
    } else {
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay
    } ?: return null
    val modes = runCatching {
        display.supportedModes.map { mode ->
            DisplayRefreshMode(
                modeId = mode.modeId,
                refreshRate = mode.refreshRate,
                width = mode.physicalWidth,
                height = mode.physicalHeight,
            )
        }
    }.getOrDefault(emptyList())
    if (modes.isEmpty()) return null
    val currentModeId = runCatching { display.mode.modeId }.getOrNull() ?: return null
    return resolveHighRefreshModeId(
        currentModeId = currentModeId,
        supportedModes = modes,
        target = target,
    )
}
