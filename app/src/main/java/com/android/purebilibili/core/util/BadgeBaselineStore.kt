// 文件路径: core/util/BadgeBaselineStore.kt
//
// 「未读红点基线」本地存储。
//
// ★ 为什么需要它(用户实测反馈):
//   主页右上角消息数、底部动态 tab 的数字经常「一直显示去不掉, 但点进去没有内容」。
//   原因是这些数字直接来自服务端未读接口 —— 服务端把一些 App 内看不到的
//   (如系统推送、已过期内容、其它端已读) 也算进未读数, 于是数字永远消不掉。
//
//   本类记录「用户已经确认过的最大值」: 界面只显示
//   `服务端计数 - 已确认基线`, 且进入对应页面时把当前计数记为新的基线。
//   这样数字既能清掉, 又不会漏掉真正的新增。
package com.android.purebilibili.core.util

import android.content.Context
import android.content.SharedPreferences

object BadgeBaselineStore {

    private const val PREFS_NAME = "badge_baseline"

    const val KEY_MESSAGE = "message"
    const val KEY_DYNAMIC = "dynamic"

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
            return created
        }
    }

    /** 读取已确认基线(默认 0)。 */
    fun get(context: Context, key: String): Int = prefs(context).getInt(key, 0)

    /**
     * 把基线推进到 [serverCount](只增不减), 返回新的基线。
     *
     * 只增不减: 服务端计数偶尔回退(如已读同步)时, 基线不跟着降, 避免红点反复。
     */
    fun acknowledge(context: Context, key: String, serverCount: Int): Int {
        val current = get(context, key)
        val next = maxOf(current, serverCount.coerceAtLeast(0))
        if (next != current) {
            prefs(context).edit().putInt(key, next).apply()
        }
        return next
    }

    /** 服务端计数换算成界面应显示的数字。 */
    fun displayCount(context: Context, key: String, serverCount: Int): Int =
        (serverCount - get(context, key)).coerceAtLeast(0)
}
