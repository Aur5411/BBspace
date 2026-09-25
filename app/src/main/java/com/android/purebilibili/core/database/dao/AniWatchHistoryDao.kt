// 文件路径: core/database/dao/AniWatchHistoryDao.kt
package com.android.purebilibili.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.purebilibili.core.database.entity.AniWatchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface AniWatchHistoryDao {

    @Query("SELECT * FROM ani_watch_history ORDER BY watchedAt DESC")
    fun observeAll(): Flow<List<AniWatchHistory>>

    @Query("SELECT * FROM ani_watch_history ORDER BY watchedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AniWatchHistory>>

    @Query("SELECT * FROM ani_watch_history WHERE subjectId = :subjectId ORDER BY watchedAt DESC LIMIT 1")
    suspend fun latestForSubject(subjectId: Long): AniWatchHistory?

    @Query("SELECT * FROM ani_watch_history WHERE episodeId = :episodeId LIMIT 1")
    suspend fun forEpisode(episodeId: Long): AniWatchHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AniWatchHistory)

    @Query("DELETE FROM ani_watch_history WHERE episodeId = :episodeId")
    suspend fun deleteEpisode(episodeId: Long)

    @Query("DELETE FROM ani_watch_history WHERE subjectId = :subjectId")
    suspend fun deleteSubject(subjectId: Long)

    @Query("DELETE FROM ani_watch_history")
    suspend fun clearAll()

    /**
     * 番剧设置「历史条数上限」落地用:
     * 只保留 watchedAt 最新的 [keep] 条, 其余删除。
     */
    @Query(
        "DELETE FROM ani_watch_history WHERE id NOT IN " +
            "(SELECT id FROM ani_watch_history ORDER BY watchedAt DESC LIMIT :keep)"
    )
    suspend fun trimToNewest(keep: Int)
}
