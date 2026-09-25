// 文件路径: core/database/dao/AniLocalDanmakuDao.kt
package com.android.purebilibili.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.android.purebilibili.core.database.entity.AniLocalDanmaku
import kotlinx.coroutines.flow.Flow

@Dao
interface AniLocalDanmakuDao {

    /** 某一话的全部本地弹幕, 按时间轴排序(直接喂给弹幕层)。 */
    @Query("SELECT * FROM ani_local_danmaku WHERE episodeId = :episodeId ORDER BY playTimeMillis ASC")
    suspend fun forEpisode(episodeId: Long): List<AniLocalDanmaku>

    /** 供播放器实时订阅, 发一条就自动多一条。 */
    @Query("SELECT * FROM ani_local_danmaku WHERE episodeId = :episodeId ORDER BY playTimeMillis ASC")
    fun observeForEpisode(episodeId: Long): Flow<List<AniLocalDanmaku>>

    @Query("SELECT COUNT(*) FROM ani_local_danmaku WHERE episodeId = :episodeId")
    suspend fun countForEpisode(episodeId: Long): Int

    @Insert
    suspend fun insert(item: AniLocalDanmaku): Long

    @Query("DELETE FROM ani_local_danmaku WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM ani_local_danmaku WHERE episodeId = :episodeId")
    suspend fun deleteEpisode(episodeId: Long)

    @Query("DELETE FROM ani_local_danmaku")
    suspend fun clearAll()
}
