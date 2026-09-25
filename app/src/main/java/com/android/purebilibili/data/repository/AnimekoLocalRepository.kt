// 文件路径: data/repository/AnimekoLocalRepository.kt
//
// 追番模块「本地」数据仓库: 收藏 / 历史 / 缓存。
// ★ 全部落在本机 Room 数据库, 不上报 B 站, 也不上报 ANI。
package com.android.purebilibili.data.repository

import android.content.Context
import com.android.purebilibili.core.database.AppDatabase
import com.android.purebilibili.core.database.entity.AniCacheRecord
import com.android.purebilibili.core.database.entity.AniFavoriteSubject
import com.android.purebilibili.core.database.entity.AniLocalDanmaku
import com.android.purebilibili.core.database.entity.AniWatchHistory
import com.android.purebilibili.data.model.animeko.AniEpisode
import com.android.purebilibili.data.model.animeko.AniSubjectDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

object AnimekoLocalRepository {

    private fun db(context: Context) = AppDatabase.getDatabase(context)

    // ---------------------------------------------------------------
    // 本地收藏
    // ---------------------------------------------------------------

    fun observeFavorites(context: Context): Flow<List<AniFavoriteSubject>> =
        db(context).aniFavoriteDao().observeAll()

    fun observeIsFavorite(context: Context, subjectId: Long): Flow<Boolean> =
        db(context).aniFavoriteDao().observeIsFavorite(subjectId)

    suspend fun isFavorite(context: Context, subjectId: Long): Boolean =
        withContext(Dispatchers.IO) { db(context).aniFavoriteDao().isFavorite(subjectId) }

    /** 加入本地收藏 (从详情页调用, 顺带缓存展示字段)。 */
    suspend fun addFavorite(context: Context, detail: AniSubjectDetail) =
        withContext(Dispatchers.IO) {
            val dao = db(context).aniFavoriteDao()
            val existing = dao.get(detail.id)
            dao.upsert(
                AniFavoriteSubject(
                    subjectId = detail.id,
                    name = detail.name,
                    nameCn = detail.nameCn,
                    cover = detail.cover,
                    score = detail.score,
                    airDate = detail.airDate,
                    totalEpisodes = detail.mainEpisodes.size,
                    watchedEpisode = existing?.watchedEpisode ?: 0,
                    finished = existing?.finished ?: false,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }

    suspend fun removeFavorite(context: Context, subjectId: Long) =
        withContext(Dispatchers.IO) { db(context).aniFavoriteDao().delete(subjectId) }

    suspend fun toggleFavorite(context: Context, detail: AniSubjectDetail): Boolean =
        withContext(Dispatchers.IO) {
            val dao = db(context).aniFavoriteDao()
            if (dao.isFavorite(detail.id)) {
                dao.delete(detail.id)
                false
            } else {
                dao.upsert(
                    AniFavoriteSubject(
                        subjectId = detail.id,
                        name = detail.name,
                        nameCn = detail.nameCn,
                        cover = detail.cover,
                        score = detail.score,
                        airDate = detail.airDate,
                        totalEpisodes = detail.mainEpisodes.size,
                    )
                )
                true
            }
        }

    /** 更新本地观看进度。 */
    suspend fun updateProgress(context: Context, subjectId: Long, watchedEpisode: Int) =
        withContext(Dispatchers.IO) {
            db(context).aniFavoriteDao().updateProgress(
                subjectId = subjectId,
                watched = watchedEpisode,
                now = System.currentTimeMillis(),
            )
        }

    // ---------------------------------------------------------------
    // 本地历史
    // ---------------------------------------------------------------

    fun observeHistory(context: Context): Flow<List<AniWatchHistory>> =
        db(context).aniWatchHistoryDao().observeAll()

    fun observeRecentHistory(context: Context, limit: Int = 20): Flow<List<AniWatchHistory>> =
        db(context).aniWatchHistoryDao().observeRecent(limit)

    suspend fun latestHistoryForSubject(context: Context, subjectId: Long): AniWatchHistory? =
        withContext(Dispatchers.IO) {
            db(context).aniWatchHistoryDao().latestForSubject(subjectId)
        }

    /** 播放某话时记一笔本地历史。 */
    suspend fun recordPlayback(
        context: Context,
        subject: AniSubjectDetail,
        episode: AniEpisode,
        positionMillis: Long = 0L,
        durationMillis: Long = 0L,
    ) = withContext(Dispatchers.IO) {
        val dao = db(context).aniWatchHistoryDao()
        val existing = dao.forEpisode(episode.episodeId)
        dao.upsert(
            AniWatchHistory(
                id = existing?.id ?: 0L,
                subjectId = subject.id,
                episodeId = episode.episodeId,
                episodeSort = episode.displayEp,
                episodeName = episode.displayName,
                subjectName = subject.displayName,
                cover = subject.cover,
                positionMillis = positionMillis,
                durationMillis = durationMillis,
                watchedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun deleteHistoryEpisode(context: Context, episodeId: Long) =
        withContext(Dispatchers.IO) { db(context).aniWatchHistoryDao().deleteEpisode(episodeId) }

    /** 取某一话的历史记录（用于「记住播放进度」续播）。 */
    suspend fun historyForEpisode(context: Context, episodeId: Long): AniWatchHistory? =
        withContext(Dispatchers.IO) { db(context).aniWatchHistoryDao().forEpisode(episodeId) }

    /** 按「历史条数上限」裁剪，只保留最新的 keep 条。 */
    suspend fun trimHistory(context: Context, keep: Int) =
        withContext(Dispatchers.IO) {
            db(context).aniWatchHistoryDao().trimToNewest(keep.coerceAtLeast(1))
        }

    suspend fun deleteHistorySubject(context: Context, subjectId: Long) =
        withContext(Dispatchers.IO) { db(context).aniWatchHistoryDao().deleteSubject(subjectId) }

    suspend fun clearHistory(context: Context) =
        withContext(Dispatchers.IO) { db(context).aniWatchHistoryDao().clearAll() }

    // ---------------------------------------------------------------
    // 番剧缓存
    // ---------------------------------------------------------------

    fun observeCaches(context: Context): Flow<List<AniCacheRecord>> =
        db(context).aniCacheDao().observeAll()

    fun observeCachesForSubject(context: Context, subjectId: Long): Flow<List<AniCacheRecord>> =
        db(context).aniCacheDao().observeForSubject(subjectId)

    fun observeCacheTotalSize(context: Context): Flow<Long> =
        db(context).aniCacheDao().observeTotalSize()

    suspend fun cacheForEpisode(context: Context, episodeId: Long): AniCacheRecord? =
        withContext(Dispatchers.IO) { db(context).aniCacheDao().forEpisode(episodeId) }

    suspend fun upsertCache(context: Context, record: AniCacheRecord) =
        withContext(Dispatchers.IO) { db(context).aniCacheDao().upsert(record) }

    suspend fun deleteCache(context: Context, episodeId: Long) =
        withContext(Dispatchers.IO) { db(context).aniCacheDao().deleteEpisode(episodeId) }

    suspend fun deleteCachesForSubject(context: Context, subjectId: Long) =
        withContext(Dispatchers.IO) { db(context).aniCacheDao().deleteSubject(subjectId) }

    suspend fun clearCaches(context: Context) =
        withContext(Dispatchers.IO) { db(context).aniCacheDao().clearAll() }

    // ---------------------------------------------------------------
    // 本地弹幕 (发送弹幕落本机)
    // ---------------------------------------------------------------

    /** 某一话的全部本地弹幕, 按时间轴排序。 */
    suspend fun localDanmakuForEpisode(context: Context, episodeId: Long): List<AniLocalDanmaku> =
        withContext(Dispatchers.IO) {
            db(context).aniLocalDanmakuDao().forEpisode(episodeId)
        }

    /** 新增一条本地弹幕。 */
    suspend fun addLocalDanmaku(context: Context, item: AniLocalDanmaku) =
        withContext(Dispatchers.IO) { db(context).aniLocalDanmakuDao().insert(item) }

    suspend fun deleteLocalDanmaku(context: Context, id: Long) =
        withContext(Dispatchers.IO) { db(context).aniLocalDanmakuDao().deleteById(id) }

    suspend fun clearLocalDanmaku(context: Context) =
        withContext(Dispatchers.IO) { db(context).aniLocalDanmakuDao().clearAll() }
}
