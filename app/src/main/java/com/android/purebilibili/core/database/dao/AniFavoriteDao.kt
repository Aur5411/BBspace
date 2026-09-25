// 文件路径: core/database/dao/AniFavoriteDao.kt
package com.android.purebilibili.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.purebilibili.core.database.entity.AniFavoriteSubject
import kotlinx.coroutines.flow.Flow

@Dao
interface AniFavoriteDao {

    @Query("SELECT * FROM ani_favorite_subject ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<AniFavoriteSubject>>

    @Query("SELECT * FROM ani_favorite_subject ORDER BY updatedAt DESC")
    suspend fun getAll(): List<AniFavoriteSubject>

    @Query("SELECT * FROM ani_favorite_subject WHERE subjectId = :subjectId LIMIT 1")
    suspend fun get(subjectId: Long): AniFavoriteSubject?

    @Query("SELECT EXISTS(SELECT 1 FROM ani_favorite_subject WHERE subjectId = :subjectId)")
    fun observeIsFavorite(subjectId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM ani_favorite_subject WHERE subjectId = :subjectId)")
    suspend fun isFavorite(subjectId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AniFavoriteSubject)

    @Query("DELETE FROM ani_favorite_subject WHERE subjectId = :subjectId")
    suspend fun delete(subjectId: Long)

    @Query("UPDATE ani_favorite_subject SET watchedEpisode = :watched, updatedAt = :now WHERE subjectId = :subjectId")
    suspend fun updateProgress(subjectId: Long, watched: Int, now: Long)

    @Query("DELETE FROM ani_favorite_subject")
    suspend fun clearAll()
}
