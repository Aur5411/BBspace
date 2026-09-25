// 文件路径: core/database/entity/AniWatchHistory.kt
package com.android.purebilibili.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 本地追番观看历史。
 *
 * 只记录「看了哪一话」, 用于追番页的「继续观看」与「追番历史」入口。
 * 不上报任何服务端。
 */
@Entity(
    tableName = "ani_watch_history",
    indices = [Index(value = ["subjectId", "episodeId"], unique = true)],
)
data class AniWatchHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val subjectId: Long,
    val episodeId: Long,
    val episodeSort: String = "",
    val episodeName: String = "",
    val subjectName: String = "",
    val cover: String = "",
    /** 播放进度毫秒, 用于「继续观看」。 */
    val positionMillis: Long = 0L,
    /** 该话总时长毫秒, 0 表示未知。 */
    val durationMillis: Long = 0L,
    val watchedAt: Long = System.currentTimeMillis(),
)
