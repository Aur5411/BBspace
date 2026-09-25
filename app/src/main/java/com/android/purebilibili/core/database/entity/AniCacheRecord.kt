// 文件路径: core/database/entity/AniCacheRecord.kt
package com.android.purebilibili.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 番剧缓存记录 (本地元数据)。
 *
 * 只登记元数据与文件大小, 实际文件由下载器写到应用私有目录。
 * 全部本地, 不上报服务端。
 */
@Entity(tableName = "ani_cache_record")
data class AniCacheRecord(
    @PrimaryKey
    val episodeId: Long,
    val subjectId: Long,
    val subjectName: String = "",
    val episodeName: String = "",
    val episodeSort: String = "",
    val cover: String = "",
    /** 本地文件绝对路径。 */
    val filePath: String = "",
    val fileSizeBytes: Long = 0L,
    /** 资源来源标识, 对应「换源」里选中的那一项。 */
    val sourceId: String = "",
    val sourceName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
