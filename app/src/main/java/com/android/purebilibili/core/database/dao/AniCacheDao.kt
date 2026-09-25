// 文件路径: core/database/dao/AniCacheDao.kt
package com.android.purebilibili.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.purebilibili.core.database.entity.AniCacheRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AniCacheDao {

    @Query("SELECT * FROM ani_cache_record ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AniCacheRecord>>

    @Query("SELECT * FROM ani_cache_record WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun observeForSubject(subjectId: Long): Flow<List<AniCacheRecord>>

    @Query("SELECT * FROM ani_cache_record WHERE episodeId = :episodeId LIMIT 1")
    suspend fun forEpisode(episodeId: Long): AniCacheRecord?

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM ani_cache_record")
    fun observeTotalSize(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AniCacheRecord)

    @Query("DELETE FROM ani_cache_record WHERE episodeId = :episodeId")
    suspend fun deleteEpisode(episodeId: Long)

    @Query("DELETE FROM ani_cache_record WHERE subjectId = :subjectId")
    suspend fun deleteSubject(subjectId: Long)

    @Query("DELETE FROM ani_cache_record")
    suspend fun clearAll()
}
