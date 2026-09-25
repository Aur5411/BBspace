// 文件路径: core/database/entity/AniLocalDanmaku.kt
//
// 「番剧播放器 → 发送弹幕」的本地存储。
//
// ★ 为什么不直接发到服务端:
//   animeko 的发送接口实测是 POST /v1/danmaku/{episodeId},
//   无 token 时返回 401 "Token is not valid or has expired" —— 必须登录 ANI 账号。
//   而本 App 的追番模块设计原则是「不登录任何非 B 站账号」,
//   所以用户发出的弹幕改为**本地弹幕**:
//     1. 立刻在本机播放器的弹幕层上屏(无需等待任何网络往返);
//     2. 落到本表, 下次播同一话时自动一起加载出来。
//   这样「发送弹幕」的交互闭环是完整的, 只是不对外广播。
package com.android.purebilibili.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ani_local_danmaku",
    indices = [Index(value = ["episodeId"])],
)
data class AniLocalDanmaku(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** 所属剧集 id (animeko episodeId)。 */
    val episodeId: Long = 0L,
    /** 发送时的播放进度(毫秒), 决定它挂在时间轴哪个位置。 */
    val playTimeMillis: Long = 0L,
    val text: String = "",
    /** ARGB 十进制; -1 表示默认白。 */
    val color: Int = -1,
    /** NORMAL(滚动) / TOP(顶部) / BOTTOM(底部)。 */
    val location: String = "NORMAL",
    val createdAt: Long = System.currentTimeMillis(),
)
