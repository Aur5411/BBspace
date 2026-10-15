// 文件路径: feature/anime/BangumiEpisodeCommentSection.kt
//
// 播放页「本集吐槽」区块（纯展示）。
//
// 数据来自 animeko 服务端 `GET /v2/episodes/{episodeId}/comments`（与详情页短评同源,
// 国内可达稳定）。★ 用户要求: 只做「看」——展示作者/时间/正文/图片/已有回复,
// 不做点赞、贴贴、发表评论、回复等交互。
package com.android.purebilibili.feature.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.data.model.animeko.AniEpisodeComment
import com.android.purebilibili.data.model.animeko.AniEpisodeCommentPage
import com.android.purebilibili.data.model.animeko.AniEpisodeReply

/** 本集吐槽的加载状态。 */
sealed interface BangumiEpisodeCommentState {
    data object Idle : BangumiEpisodeCommentState
    data object Loading : BangumiEpisodeCommentState
    data class Error(val message: String = "") : BangumiEpisodeCommentState
    data class Success(val page: AniEpisodeCommentPage) : BangumiEpisodeCommentState
}

/**
 * 播放页「本集吐槽」区块（纯展示）。
 */
@Composable
fun BangumiEpisodeCommentSection(
    state: BangumiEpisodeCommentState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is BangumiEpisodeCommentState.Idle) return
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth()) {
        AniSectionHeader(
            title = "本集吐槽",
            action = {
                if (state is BangumiEpisodeCommentState.Success && state.page.items.isNotEmpty()) {
                    AppText(
                        text = "来自 Bangumi · ${state.page.total} 条",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
        Spacer(Modifier.height(8.dp))

        when (state) {
            is BangumiEpisodeCommentState.Idle -> Unit

            is BangumiEpisodeCommentState.Loading -> AppText(
                text = "加载中…",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            is BangumiEpisodeCommentState.Error -> Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppText(
                    text = "本集吐槽加载失败" +
                        state.message.takeIf { it.isNotBlank() }?.let { "（$it）" }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
                AppText(
                    text = "点此重试",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.primary,
                    modifier = Modifier.clickable { onRetry() },
                )
            }

            is BangumiEpisodeCommentState.Success -> {
                if (state.page.items.isEmpty()) {
                    // ★ 站点不可用时默认隐藏, 不显示任何提示; 仅正常空态提示"还没有人吐槽"
                    if (!state.page.bangumiUnavailable) {
                        AppText(
                            text = "这一集还没有人吐槽",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.page.items.forEach { comment ->
                            BangumiEpisodeCommentCard(comment = comment)
                        }
                    }
                }
            }
        }
    }
}

/** 单条吐槽卡片（纯展示）。 */
@Composable
private fun BangumiEpisodeCommentCard(comment: AniEpisodeComment) {
    AppSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AniAvatar(
                    url = comment.author.avatarUrl,
                    contentDescription = comment.displayAuthor,
                    size = 32.dp,
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    AppText(
                        text = comment.displayAuthor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                    AppText(
                        text = formatCommentTime(comment.createdAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (comment.displayContent.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                // ★ 正文里的 (bgmN) 表情代码内联渲染成图片
                BangumiEmoteText(
                    text = comment.displayContent,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (comment.images.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                CommentImages(urls = comment.images)
            }

            if (comment.hasReplies) {
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    comment.briefReplies.forEach { reply ->
                        BangumiEpisodeReplyRow(reply = reply)
                    }
                }
            }
        }
    }
}

/** 楼中楼的一条回复（纯展示）。 */
@Composable
private fun BangumiEpisodeReplyRow(reply: AniEpisodeReply) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AniAvatar(
                url = reply.author.avatarUrl,
                contentDescription = reply.displayAuthor,
                size = 20.dp,
            )
            Spacer(Modifier.width(6.dp))
            AppText(
                text = reply.displayAuthor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
            AppText(
                text = formatCommentTime(reply.createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val content = reply.contentBbcode
            .let { Regex("\\[/?img[^\\]]*\\]").replace(it, "") }
            .let { Regex("\\[/?url[^\\]]*\\]").replace(it, "") }
            .replace("[br]", "\n").trim()
        if (content.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            BangumiEmoteText(text = content, style = MaterialTheme.typography.bodySmall)
        }
        val imgs = Regex("\\[img\\]\\s*(https?://[^\\[\\]]+?)\\s*\\[/img\\]", RegexOption.IGNORE_CASE)
            .findAll(reply.contentBbcode).map { it.groupValues[1].trim() }
            .filter { it.startsWith("http") }.distinct().toList()
        if (imgs.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            CommentImages(urls = imgs)
        }
    }
}

/** 吐槽配图（单张铺满 / 多张两列，纯展示）。 */
@Composable
private fun CommentImages(urls: List<String>) {
    if (urls.isEmpty()) return
    if (urls.size == 1) {
        AsyncImage(
            model = urls.first(),
            contentDescription = "吐槽配图",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        urls.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { url ->
                    Box(modifier = Modifier.weight(1f)) {
                        AsyncImage(
                            model = url,
                            contentDescription = "吐槽配图",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** 时间戳 -> 相对文案(今天 / N 天前 / 具体日期)。 */
private fun formatCommentTime(millis: Long): String {
    if (millis <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - millis
    val day = 24 * 60 * 60_000L
    return when {
        diff < 60_000L -> "刚刚"
        diff < 60 * 60_000L -> "${diff / 60_000L} 分钟前"
        diff < day -> "${diff / (60 * 60_000L)} 小时前"
        diff < 30 * day -> "${diff / day} 天前"
        else -> {
            val dt = java.time.Instant.ofEpochMilli(millis)
                .atZone(java.time.ZoneId.systemDefault())
            "${dt.year}-${dt.monthValue.toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')}"
        }
    }
}
