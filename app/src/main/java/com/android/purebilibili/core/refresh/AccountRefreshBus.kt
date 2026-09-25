package com.android.purebilibili.core.refresh

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 登录态变更（登录 / 退出 / 切换账号）的全局刷新信号。
 *
 * 登录成功后，所有依赖账号态的页面（首页推荐、动态、我的、收藏、历史、
 * 稍后再看、消息等）都需要重新拉取数据。这里只广播一个失效信号，
 * 各页面订阅后自行决定刷新策略，避免登录模块反向依赖各业务模块。
 */
object AccountRefreshBus {
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, replay = 0)
    val changes = _changes.asSharedFlow()

    /** 登录态发生变化，通知所有依赖账号的页面刷新。 */
    fun notifyAccountChanged() {
        _changes.tryEmit(Unit)
    }
}
