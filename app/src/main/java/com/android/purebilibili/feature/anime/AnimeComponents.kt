// 文件路径: feature/anime/AnimeComponents.kt
//
// 「追番」模块共享 UI 组件。
//
// 设计取向: 照搬 animeko 详情页的信息层级 (封面 + 标题 + 评分 + 标签 + 信息表),
// 但绘制层统一走 BB空间的 design-system / Miuix, 保证与全局风格一致。
package com.android.purebilibili.feature.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText

/** animeko 的封面比例: 849 x 1200。详情页封面与卡片封面共用一个常量。 */
const val ANI_COVER_RATIO = 849f / 1200f

/** 空态提示。 */
@Composable
fun AniEmptyState(
    text: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppText(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!hint.isNullOrBlank()) {
                AppText(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            if (onRetry != null) {
                AppSurface(
                    onClick = onRetry,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        AppText(
                            text = "重试",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

/** 区块标题 —— 对应 animeko 的 SectionHeader(title + 右侧 action)。 */
@Composable
fun AniSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

/** 评分徽章。 */
@Composable
fun AniScoreBadge(
    score: Float?,
    modifier: Modifier = Modifier,
) {
    val text = if (score == null || score <= 0f) "暂无评分" else String.format("%.1f", score)
    val container = if (score != null && score > 0f) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        AppText(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/**
 * 番剧卡片 (竖版封面)。
 *
 * @param width 用来控制卡片宽度, 双列与横向滚动两种场景共用。
 */
@Composable
fun AniSubjectCard(
    cover: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    score: Float? = null,
    badge: String? = null,
    onClick: () -> Unit,
) {
    AppSurface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ANI_COVER_RATIO)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (cover.isNotBlank()) {
                    AsyncImage(
                        model = cover,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(ANI_COVER_RATIO),
                    )
                }
                if (!badge.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        AppText(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                }
                if (score != null && score > 0f) {
                    Box(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)) {
                        AniScoreBadge(score = score)
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                AppText(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    AppText(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** 横向滚动的番剧小卡 (用于推荐 / 关联)。 */
@Composable
fun AniSubjectCardCompact(
    cover: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    AniSubjectCard(
        cover = cover,
        title = title,
        subtitle = subtitle,
        modifier = modifier.width(104.dp),
        onClick = onClick,
    )
}

/** 标签 chip。 */
@Composable
fun AniTagChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    AppSurface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
            AppText(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/**
 * 信息表 —— 对应 animeko 的 SubjectInfoTable。
 *
 * @param labelWidth 标签列宽, animeko 采用 78.dp。
 */
@Composable
fun AniInfoTable(
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    labelWidth: androidx.compose.ui.unit.Dp = 78.dp,
    rowSpacing: androidx.compose.ui.unit.Dp = 12.dp,
) {
    val visible = rows.filter { it.second.isNotBlank() }
    if (visible.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(rowSpacing),
    ) {
        visible.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                AppText(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(labelWidth),
                )
                AppText(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 选集按钮 (animeko 的 FlowRow 选集单元)。 */
@Composable
fun AniEpisodeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    watched: Boolean = false,
    onClick: () -> Unit,
) {
    val container = when {
        selected -> MaterialTheme.colorScheme.primary
        watched -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        watched -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    AppSurface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = container,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = content,
                maxLines = 1,
            )
        }
    }
}

/** 顶部渐变遮罩 (详情页 hero 图与内容之间的过渡)。 */
@Composable
fun AniHeroScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                )
            ),
    )
}

/** 文件大小格式化: 1536 -> 1.5 KB。 */
fun formatAniSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var idx = 0
    while (value >= 1024.0 && idx < units.lastIndex) {
        value /= 1024.0
        idx++
    }
    return if (idx == 0) "${bytes} B" else String.format("%.1f %s", value, units[idx])
}

/** 由收藏条目推导进度文案, 形如 "看到 第12话"。 */
fun aniFavoriteProgressLabel(watched: Int, total: Int): String = when {
    total <= 0 -> ""
    watched <= 0 -> "未开始"
    watched >= total -> "已看完"
    else -> "看到 $watched / $total"
}

/** 圆形头像（角色 / 声优 / 评论作者通用）。 */
@Composable
fun AniAvatar(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 36.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                // ★ 顶部对齐: 人物头像裁圆形时保住头部(默认居中会裁掉头顶)
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // 无头像时给个占位字，避免出现空洞
            AppText(
                text = contentDescription?.take(1).orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 角色卡片 —— 图上、角色名、声优名。
 *
 * animeko 的原版是「角色图 + 角色名 + 声优名 + 声优头像」四段式，
 * 手机端宽度有限，这里把声优头像缩小后放在声优名前，保持信息完整。
 */
@Composable
fun AniCharacterCard(
    contentDescription: String,
    imageUrl: String,
    characterName: String,
    actorName: String?,
    actorAvatarUrl: String? = null,
    modifier: Modifier = Modifier,
    roleLabel: String? = null,
) {
    Column(
        modifier = modifier.width(84.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ANI_COVER_RATIO)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = contentDescription,
                    // ★ 顶部对齐: 角色立绘是竖长图, 默认居中裁剪会把头部裁掉
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (!roleLabel.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    AppText(
                        text = roleLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        AppText(
            text = characterName,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!actorName.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!actorAvatarUrl.isNullOrBlank()) {
                    AniAvatar(
                        url = actorAvatarUrl,
                        contentDescription = actorName,
                        size = 14.dp,
                    )
                    Spacer(Modifier.width(3.dp))
                }
                AppText(
                    text = actorName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 制作人员行：职位标签 + 人名（可多人）。 */
@Composable
fun AniStaffRow(
    positionLabel: String,
    names: List<String>,
    modifier: Modifier = Modifier,
    labelWidth: androidx.compose.ui.unit.Dp = 78.dp,
) {
    if (names.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        AppText(
            text = positionLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(labelWidth),
        )
        AppText(
            text = names.joinToString(" / "),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 单条短评卡片：头像 + 昵称 + 评分 + 时间 + 正文。 */
@Composable
fun AniReviewCard(
    authorName: String,
    avatarUrl: String,
    content: String,
    rating: Int,
    sourceLabel: String,
    timeLabel: String,
    likeCount: Int,
    modifier: Modifier = Modifier,
) {
    AppSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AniAvatar(
                    url = avatarUrl,
                    contentDescription = authorName,
                    size = 32.dp,
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    AppText(
                        text = authorName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (sourceLabel.isNotBlank()) {
                            AppText(
                                text = sourceLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (timeLabel.isNotBlank()) {
                            if (sourceLabel.isNotBlank()) {
                                AppText(
                                    text = " · ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            AppText(
                                text = timeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (rating in 1..10) {
                    Spacer(Modifier.width(8.dp))
                    AniScoreBadge(score = rating.toFloat())
                }
            }
            if (content.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                AppText(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (likeCount > 0) {
                Spacer(Modifier.height(6.dp))
                AppText(
                    text = "👍 $likeCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 把 ISO-8601 UTC 时间转成「今天 / 3 天前 / 2026-08-01」这种相对文案。 */
fun aniRelativeTime(iso: String, nowMs: Long = System.currentTimeMillis()): String {
    if (iso.isBlank()) return ""
    val parsed = runCatching {
        java.time.Instant.parse(iso).toEpochMilli()
    }.getOrNull() ?: return ""
    val diff = nowMs - parsed
    if (diff < 0) return "刚刚"
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        diff < minute -> "刚刚"
        diff < hour -> "${diff / minute} 分钟前"
        diff < day -> "${diff / hour} 小时前"
        diff < 30 * day -> "${diff / day} 天前"
        else -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(parsed))
    }
}
