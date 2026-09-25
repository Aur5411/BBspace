package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.VideoItem

internal fun resolveWatchLaterAid(video: VideoItem): Long {
    return video.aid.takeIf { it > 0 } ?: video.id
}

