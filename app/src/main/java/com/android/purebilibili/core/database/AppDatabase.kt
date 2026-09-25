package com.android.purebilibili.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.android.purebilibili.core.database.dao.SearchHistoryDao
import com.android.purebilibili.core.database.entity.SearchHistory
import com.android.purebilibili.core.database.entity.BlockedUp
import com.android.purebilibili.core.database.dao.BlockedUpDao
import com.android.purebilibili.core.database.dao.CommentFraudDao
import com.android.purebilibili.core.database.entity.CommentFraudRecord
// 追番模块 (animeko 数据源) 的本地表
import com.android.purebilibili.core.database.dao.AniFavoriteDao
import com.android.purebilibili.core.database.dao.AniWatchHistoryDao
import com.android.purebilibili.core.database.dao.AniCacheDao
import com.android.purebilibili.core.database.dao.AniLocalDanmakuDao
import com.android.purebilibili.core.database.entity.AniFavoriteSubject
import com.android.purebilibili.core.database.entity.AniWatchHistory
import com.android.purebilibili.core.database.entity.AniCacheRecord
import com.android.purebilibili.core.database.entity.AniLocalDanmaku

@Database(
    entities = [
        SearchHistory::class,
        BlockedUp::class,
        CommentFraudRecord::class,
        // 追番模块: 收藏 / 历史 / 缓存 / 本地弹幕 全部本地存储
        AniFavoriteSubject::class,
        AniWatchHistory::class,
        AniCacheRecord::class,
        AniLocalDanmaku::class,
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun blockedUpDao(): BlockedUpDao
    abstract fun commentFraudDao(): CommentFraudDao

    // 追番模块
    abstract fun aniFavoriteDao(): AniFavoriteDao
    abstract fun aniWatchHistoryDao(): AniWatchHistoryDao
    abstract fun aniCacheDao(): AniCacheDao
    abstract fun aniLocalDanmakuDao(): AniLocalDanmakuDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN level INTEGER")
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN sign TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN vipLabel TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN officialTitle TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN follower INTEGER")
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN archiveCount INTEGER")
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN lastSyncedAt INTEGER")
            }
        }

        /** 6 -> 7: 新增追番模块的本地收藏 / 历史 / 缓存三张表。 */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ani_favorite_subject` (" +
                        "`subjectId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`nameCn` TEXT NOT NULL, " +
                        "`cover` TEXT NOT NULL, " +
                        "`score` TEXT NOT NULL, " +
                        "`airDate` TEXT NOT NULL, " +
                        "`totalEpisodes` INTEGER NOT NULL, " +
                        "`watchedEpisode` INTEGER NOT NULL, " +
                        "`finished` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`subjectId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ani_watch_history` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`subjectId` INTEGER NOT NULL, " +
                        "`episodeId` INTEGER NOT NULL, " +
                        "`episodeSort` TEXT NOT NULL, " +
                        "`episodeName` TEXT NOT NULL, " +
                        "`subjectName` TEXT NOT NULL, " +
                        "`cover` TEXT NOT NULL, " +
                        "`positionMillis` INTEGER NOT NULL, " +
                        "`durationMillis` INTEGER NOT NULL, " +
                        "`watchedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_ani_watch_history_subjectId_episodeId` " +
                        "ON `ani_watch_history` (`subjectId`, `episodeId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ani_cache_record` (" +
                        "`episodeId` INTEGER NOT NULL, " +
                        "`subjectId` INTEGER NOT NULL, " +
                        "`subjectName` TEXT NOT NULL, " +
                        "`episodeName` TEXT NOT NULL, " +
                        "`episodeSort` TEXT NOT NULL, " +
                        "`cover` TEXT NOT NULL, " +
                        "`filePath` TEXT NOT NULL, " +
                        "`fileSizeBytes` INTEGER NOT NULL, " +
                        "`sourceId` TEXT NOT NULL, " +
                        "`sourceName` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`episodeId`))"
                )
            }
        }
        /** 7 -> 8: 新增番剧本地弹幕表(发送弹幕落本机, animeko 发送接口要登录故不发服务端)。 */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ani_local_danmaku` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`episodeId` INTEGER NOT NULL, " +
                        "`playTimeMillis` INTEGER NOT NULL, " +
                        "`text` TEXT NOT NULL, " +
                        "`color` INTEGER NOT NULL, " +
                        "`location` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ani_local_danmaku_episodeId` " +
                        "ON `ani_local_danmaku` (`episodeId`)"
                )
            }
        }
    }
}
