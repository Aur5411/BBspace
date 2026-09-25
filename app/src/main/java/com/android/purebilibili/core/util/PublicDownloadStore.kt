// 文件路径: core/util/PublicDownloadStore.kt
//
// 把下载文件落到**手机公共下载目录**: `Download/BBspace/视频/` 与 `Download/BBspace/番剧/`。
//
// ★ 应用户要求:
//   之前下载都落在 App 私有目录(Android/data/<pkg>/files/downloads), 文件管理器里
//   根本看不到。现在改为公共 Download 目录, 并且视频与番剧分开放。
//
// ★ 实现分层(按 Android 版本):
//   - API 29+ (Android 10 起): 走 MediaStore.Downloads, **无需任何存储权限**,
//     用 RELATIVE_PATH 指定子目录;
//   - API < 29: 直接写 Environment.getExternalStoragePublicDirectory(DOWNLOADS),
//     需要 WRITE_EXTERNAL_STORAGE(调用方自行确保或降级到私有目录)。
package com.android.purebilibili.core.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream

object PublicDownloadStore {

    /** 视频下载子目录。 */
    const val FOLDER_VIDEO = "BBspace/视频"

    /** 番剧下载子目录。 */
    const val FOLDER_ANIME = "BBspace/番剧"

    /** 按扩展名猜 MIME。 */
    fun mimeOf(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
        "mp4", "m4v" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "flv" -> "video/x-flv"
        "ts" -> "video/mp2t"
        "m4a" -> "audio/mp4"
        "mp3" -> "audio/mpeg"
        "flac" -> "audio/flac"
        "srt" -> "application/x-subrip"
        "ass" -> "text/x-ssa"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        else -> "application/octet-stream"
    }

    /** 文件名里的非法字符替换掉, 避免 MediaStore 插入失败。 */
    fun sanitizeFileName(raw: String): String {
        val cleaned = raw.replace(Regex("""[\\/:*?"<>|\r\n\t]"""), "_").trim().trimEnd('.')
        val limited = cleaned.take(80).ifBlank { "bbspace_${System.currentTimeMillis()}" }
        return limited
    }

    /**
     * 在公共下载目录里创建一个待写文件。
     *
     * @param folder [FOLDER_VIDEO] / [FOLDER_ANIME]
     * @return 可写入的 Uri 与 OutputStream; 失败返回 null(调用方降级到私有目录)
     */
    fun createFile(
        context: Context,
        folder: String,
        fileName: String,
        mimeType: String = mimeOf(fileName),
    ): Pair<Uri, OutputStream>? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$folder")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching null
            val stream = resolver.openOutputStream(uri) ?: return@runCatching null
            uri to stream
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                folder,
            )
            if (!dir.exists() && !dir.mkdirs()) return@runCatching null
            val file = File(dir, fileName)
            Uri.fromFile(file) to file.outputStream()
        }
    }.getOrNull()

    /** 写入结束后收尾(API 29+ 需要把 IS_PENDING 置 0, 否则相册/文件管理器看不到)。 */
    fun finishFile(context: Context, uri: Uri) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                context.contentResolver.update(uri, values, null, null)
            }
        }
    }

    /** 把已经下载好的临时文件搬进公共下载目录。 */
    fun publishFile(
        context: Context,
        folder: String,
        source: File,
        fileName: String = source.name,
    ): Uri? {
        if (!source.isFile) return null
        val created = createFile(context, folder, fileName) ?: return null
        val (uri, out) = created
        return runCatching {
            source.inputStream().use { input -> out.use { output -> input.copyTo(output) } }
            finishFile(context, uri)
            uri
        }.getOrNull()
    }
}
