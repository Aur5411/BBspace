// 文件路径: core/database/entity/AniFavoriteSubject.kt
package com.android.purebilibili.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 本地追番收藏条目。
 *
 * 完全本地存储, 不上报任何服务端 (既不用 B 站, 也不用 ANI 账号)。
 * 只缓存展示所需的最小字段, 点进详情时再按 [subjectId] 拉取最新数据。
 */
@Entity(tableName = "ani_favorite_subject")
data class AniFavoriteSubject(
    @PrimaryKey
    val subjectId: Long,
    val name: String = "",
    val nameCn: String = "",
    val cover: String = "",
    val score: String = "",
    val airDate: String = "",
    val totalEpisodes: Int = 0,
    /** 本地观看进度: 已看到第几话 (对应 episode.sort)。 */
    val watchedEpisode: Int = 0,
    /** 用户是否看完。 */
    val finished: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val displayName: String get() = nameCn.ifBlank { name }
}
