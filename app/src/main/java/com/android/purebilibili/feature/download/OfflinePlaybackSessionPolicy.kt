package com.android.purebilibili.feature.download

internal data class OfflinePlaybackSessionMetadata(
    val title: String,
    val artist: String,
    val coverUrl: String
)

internal data class OfflineMiniPlayerPayload(
    val bvid: String,
    val cid: Long,
    val title: String,
    val owner: String,
    val coverUrl: String
)

internal fun shouldRegisterOfflinePlaybackSession(
    fileExists: Boolean,
    filePath: String?
): Boolean {
    return fileExists && !filePath.isNullOrBlank()
}

internal fun resolveOfflinePlaybackSessionMetadata(
    task: DownloadTask
): OfflinePlaybackSessionMetadata {
    return OfflinePlaybackSessionMetadata(
        title = task.title.ifBlank { "离线视频" },
        artist = task.ownerName.ifBlank { if (task.isAudioOnly) "离线音频" else "离线视频" },
        coverUrl = task.cover
    )
}

internal fun resolveOfflineMiniPlayerPayload(
    task: DownloadTask
): OfflineMiniPlayerPayload {
    val metadata = resolveOfflinePlaybackSessionMetadata(task)
    return OfflineMiniPlayerPayload(
        bvid = task.bvid,
        cid = task.cid,
        title = metadata.title,
        owner = metadata.artist,
        coverUrl = metadata.coverUrl
    )
}
