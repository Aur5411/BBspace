// 文件路径: feature/anime/AnimeDetailScreen.kt
//
// 「番剧详情」页 —— 版式照搬 animeko 的 SubjectDetailsPage。
//
// 对应关系 (animeko -> 本文件):
//   SubjectDetailsHeader         -> AnimeDetailHeader      (hero + 封面 + 标题 + 评分 + 追番按钮)
//   SubjectInfoTable             -> AniInfoTable           (labelWidth 78.dp)
//   SubjectSummarySection        -> AnimeSummarySection    (5 行折叠 + 显示更多)
//   SubjectTagsSection           -> AnimeTagsSection       (FlowRow, 最多 8 个)
//   SubjectDetailsSections 选集  -> AnimeEpisodesSection   (FlowRow 选集)
//   3 档响应式断点                -> AnimeDetailLayoutParams
package com.android.purebilibili.feature.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.ImmersiveAppScaffold as AppScaffold
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.data.model.animeko.AniEpisode
import com.android.purebilibili.data.model.animeko.AniSubjectDetail
import com.android.purebilibili.data.model.animeko.roleLabel

/**
 * 详情页三档响应式参数 —— 对应 animeko 的 SubjectDetailsLayoutParams。
 * COMPACT(<600dp 单列) / MEDIUM(600~1600dp) / EXPANDED(>=1600dp)。
 */
data class AnimeDetailLayoutParams(
    val contentHorizontalPadding: androidx.compose.ui.unit.Dp,
    val contentTopPadding: androidx.compose.ui.unit.Dp,
    val sectionSpacing: androidx.compose.ui.unit.Dp,
    val coverWidth: androidx.compose.ui.unit.Dp,
) {
    companion object {
        const val MEDIUM_WIDTH_DP = 600
        const val EXPANDED_WIDTH_DP = 1600

        fun forWidth(widthDp: Int): AnimeDetailLayoutParams = when {
            widthDp >= EXPANDED_WIDTH_DP -> AnimeDetailLayoutParams(48.dp, 12.dp, 28.dp, 220.dp)
            widthDp >= MEDIUM_WIDTH_DP -> AnimeDetailLayoutParams(40.dp, 12.dp, 28.dp, 180.dp)
            else -> AnimeDetailLayoutParams(16.dp, 16.dp, 20.dp, 140.dp)
        }
    }
}

@Composable
fun AnimeDetailScreen(
    subjectId: Long,
    onBack: () -> Unit,
    onPlay: (Long, Long) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: AnimeViewModel = viewModel(),
) {
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val staff by viewModel.staff.collectAsStateWithLifecycle()
    val reviewsState by viewModel.reviews.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    // ★ 番剧自有设置（封面模糊等，仅作用于番剧页）
    val aniSettings = rememberAniSettingsState()

    LaunchedEffect(subjectId) {
        viewModel.loadDetail(subjectId)
    }

    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "番剧详情",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    if (detailState is AniDetailState.Error) {
                        AppIconButton(onClick = { viewModel.loadDetail(subjectId) }) {
                            AppIcon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "重试",
                            )
                        }
                    }
                    // 番剧自有设置入口
                    AppIconButton(onClick = onSettingsClick) {
                        AppIcon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "番剧设置",
                        )
                    }
                },
            )
        },
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        when (val state = detailState) {
            is AniDetailState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                AniEmptyState(text = "加载中…")
            }

            is AniDetailState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                AniEmptyState(text = "加载失败", hint = state.message)
            }

            is AniDetailState.Success -> AnimeDetailContent(
                detail = state.detail,
                characters = characters,
                staffRows = remember(staff) { buildAniStaffRows(staff) },
                reviewsState = reviewsState,
                isFavorite = isFavorite,
                coverBlurEnabled = aniSettings.coverBlurEnabled,
                topPadding = innerPadding.calculateTopPadding(),
                onToggleFavorite = { viewModel.toggleFavorite(state.detail) },
                onPlay = onPlay,
                onRetryReviews = { viewModel.retryReviews(subjectId) },
                onLoadMoreReviews = { viewModel.loadMoreReviews(subjectId) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimeDetailContent(
    detail: AniSubjectDetail,
    characters: List<com.android.purebilibili.data.model.animeko.AniRelatedCharacter>,
    staffRows: List<Pair<String, List<String>>>,
    reviewsState: AniReviewsState,
    isFavorite: Boolean,
    coverBlurEnabled: Boolean,
    topPadding: androidx.compose.ui.unit.Dp,
    onToggleFavorite: () -> Unit,
    onPlay: (Long, Long) -> Unit,
    onRetryReviews: () -> Unit,
    onLoadMoreReviews: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    // 手机端一律走 COMPACT 档
    val params = remember { AnimeDetailLayoutParams.forWidth(ANIME_DETAIL_PHONE_WIDTH_DP) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = topPadding),
    ) {
        // ---- hero 背景图 + 封面 (animeko SubjectDetailsHeader) ----
        AnimeDetailHeader(
            detail = detail,
            params = params,
            isFavorite = isFavorite,
            coverBlurEnabled = coverBlurEnabled,
            onToggleFavorite = onToggleFavorite,
            onPlay = onPlay,
        )

        Column(
            modifier = Modifier.padding(horizontal = params.contentHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(params.sectionSpacing),
        ) {
            Spacer(Modifier.height(params.contentTopPadding))

            // ---- 简介: 5 行折叠 ----
            AnimeSummarySection(summary = detail.summary)

            // ---- 标签 ----
            AnimeTagsSection(tags = detail.tags.map { it.name })

            // ---- 信息表 ----
            val infoRows = buildList {
                add("放送开始" to detail.airDate)
                add("话数" to detail.mainEpisodes.size.takeIf { it > 0 }?.let { "$it 话" }.orEmpty())
                detail.infoOf("动画制作").takeIf { it.isNotBlank() }?.let { add("动画制作" to it) }
                detail.infoOf("原作").takeIf { it.isNotBlank() }?.let { add("原作" to it) }
                detail.infoOf("导演").takeIf { it.isNotBlank() }?.let { add("导演" to it) }
                detail.infoOf("脚本").takeIf { it.isNotBlank() }?.let { add("脚本" to it) }
                detail.infoOf("音乐").takeIf { it.isNotBlank() }?.let { add("音乐" to it) }
                if (detail.aliases.isNotEmpty()) {
                    add("别名" to detail.aliases.joinToString(" / "))
                }
            }
            AniSectionHeader(title = "详细信息")
            AniInfoTable(rows = infoRows, labelWidth = 78.dp, rowSpacing = 12.dp)

            // ---- 选集 ----
            if (detail.mainEpisodes.isNotEmpty()) {
                AnimeEpisodesSection(
                    episodes = detail.mainEpisodes,
                    onPlay = { ep -> onPlay(detail.id, ep.episodeId) },
                )
            }

            // ---- 角色与声优 ----
            if (characters.isNotEmpty()) {
                AniSectionHeader(
                    title = "角色与声优",
                    action = {
                        AppText(
                            text = "${characters.size} 位",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    },
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                ) {
                    // ★ 主角排在前面: role 1=主角 / 2=配角 / 4=客串(其它),
                    //   同级保持服务端原始顺序(index)
                    items(
                        characters.sortedWith(
                            compareBy(
                                { aniCharacterRoleRank(it.role) },
                                { it.index },
                            )
                        ),
                        key = { it.character.id },
                    ) { rel ->
                        val c = rel.character
                        val actor = c.actors.firstOrNull()
                        AniCharacterCard(
                            contentDescription = c.displayName,
                            imageUrl = c.imageLarge.ifBlank { c.imageMedium },
                            characterName = c.displayName,
                            actorName = actor?.displayName,
                            actorAvatarUrl = actor?.imageMedium?.ifBlank { actor.imageLarge },
                            roleLabel = rel.roleLabel.takeIf { it.isNotBlank() },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            // ---- 制作人员 ----
            if (staffRows.isNotEmpty()) {
                var staffExpanded by remember { mutableStateOf(false) }
                val visibleStaff = if (staffExpanded) staffRows else staffRows.take(STAFF_COLLAPSED_ROWS)
                AniSectionHeader(
                    title = "制作人员",
                    action = {
                        if (staffRows.size > STAFF_COLLAPSED_ROWS) {
                            AppText(
                                text = if (staffExpanded) "收起" else "全部 ${staffRows.size} 项",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.primary,
                                modifier = Modifier.clickableNoRipple {
                                    staffExpanded = !staffExpanded
                                },
                            )
                        }
                    },
                )
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visibleStaff.forEach { (label, names) ->
                        AniStaffRow(positionLabel = label, names = names)
                    }
                }
            }

            // ---- 短评 (Bangumi) ----
            AniReviewsSection(
                state = reviewsState,
                onRetry = onRetryReviews,
                onLoadMore = onLoadMoreReviews,
            )

            Spacer(Modifier.height(AppSpacingTokens.Large))
        }
    }
}

/** 手机端固定走 COMPACT 档; 平板/桌面由调用方改这个值。 */
private const val ANIME_DETAIL_PHONE_WIDTH_DP = 411

/** 详情页头部 —— hero 图 + 封面 + 标题 + 评分 + 追番按钮。 */
@Composable
private fun AnimeDetailHeader(
    detail: AniSubjectDetail,
    params: AnimeDetailLayoutParams,
    isFavorite: Boolean,
    coverBlurEnabled: Boolean,
    onToggleFavorite: () -> Unit,
    onPlay: (Long, Long) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val hero = detail.hero

    Box(modifier = Modifier.fillMaxWidth()) {
        // hero 背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(colorScheme.surfaceVariant),
        ) {
            if (hero.isNotBlank()) {
                AsyncImage(
                    model = hero,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = if (coverBlurEnabled) {
                        Modifier.fillMaxSize().blur(24.dp)
                    } else {
                        Modifier.fillMaxSize()
                    },
                )
            }
            // 从上到下的暗化 + 到底部过渡到背景色
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f),
                                androidx.compose.ui.graphics.Color.Transparent,
                                colorScheme.background,
                            )
                        )
                    ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = params.contentHorizontalPadding,
                    end = params.contentHorizontalPadding,
                    top = 120.dp,
                ),
            verticalAlignment = Alignment.Bottom,
        ) {
            // 封面
            Box(
                modifier = Modifier
                    .width(params.coverWidth)
                    .aspectRatio(ANI_COVER_RATIO)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colorScheme.surfaceContainerHigh),
            ) {
                if (detail.cover.isNotBlank()) {
                    AsyncImage(
                        model = detail.cover,
                        contentDescription = detail.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(Modifier.width(AppSpacingTokens.Medium))

            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = detail.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (detail.name.isNotBlank() && detail.name != detail.displayName) {
                    Spacer(Modifier.height(2.dp))
                    AppText(
                        text = detail.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AniScoreBadge(score = detail.scoreOrNull)
                    Spacer(Modifier.width(8.dp))
                    if (detail.rank > 0) {
                        AppText(
                            text = "Rank #${detail.rank}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppButton(
                        onClick = onToggleFavorite,
                        shape = RoundedCornerShape(10.dp),
                        containerColor = if (isFavorite) {
                            colorScheme.secondaryContainer
                        } else {
                            colorScheme.primary
                        },
                        contentColor = if (isFavorite) {
                            colorScheme.onSecondaryContainer
                        } else {
                            colorScheme.onPrimary
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        AppIcon(
                            imageVector = if (isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(4.dp))
                        AppText(
                            text = if (isFavorite) "已追番" else "追番",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    val firstEp = detail.mainEpisodes.firstOrNull()
                    if (firstEp != null) {
                        AppButton(
                            onClick = { onPlay(detail.id, firstEp.episodeId) },
                            shape = RoundedCornerShape(10.dp),
                            containerColor = colorScheme.surfaceContainerHigh,
                            contentColor = colorScheme.onSurface,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            AppText(
                                text = "开始观看",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 简介区块 —— 5 行折叠, 对应 animeko 的 SubjectSummarySection。 */
@Composable
private fun AnimeSummarySection(summary: String) {
    if (summary.isBlank()) return
    var expanded by remember { mutableStateOf(false) }
    Column {
        AniSectionHeader(title = "简介")
        Spacer(Modifier.height(6.dp))
        AppText(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
        )
        if (summary.length > 120) {
            Spacer(Modifier.height(4.dp))
            AppText(
                text = if (expanded) "收起" else "显示更多",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickableNoRipple { expanded = !expanded },
            )
        }
    }
}

/** 标签区块 —— 对应 animeko 的 SubjectTagsSection (最多 8 个)。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimeTagsSection(tags: List<String>) {
    val visible = remember(tags) {
        if (tags.size <= 6) tags else tags.take(8)
    }
    if (visible.isEmpty()) return
    Column {
        AniSectionHeader(title = "标签")
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            visible.forEach { tag ->
                AniTagChip(text = tag)
            }
        }
    }
}

/** 选集区块 —— 对应 animeko 的 FlowRow 选集。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimeEpisodesSection(
    episodes: List<AniEpisode>,
    onPlay: (AniEpisode) -> Unit,
) {
    Column {
        AniSectionHeader(title = "选集")
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ★ 按集数去重: 服务端同一集数可能出现两条(不同 ep/sort 变体),
            //   造成「两个第一集」的观感
            episodes.distinctBy { it.displayEp }.forEach { ep ->
                AniEpisodeChip(
                    label = ep.displayEp,
                    selected = false,
                    onClick = { onPlay(ep) },
                )
            }
        }
    }
}

/** 无涟漪点击, 用于「显示更多」这类纯文本操作。 */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this
        .padding(2.dp)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}

/** 制作人员折叠时默认展示的行数（原画/补间动辄上百人，全展开会淹没页面）。 */
private const val STAFF_COLLAPSED_ROWS = 8

/**
 * 把 staff 原始列表聚合成「职位 -> 人名列表」，并按重要度排序。
 *
 * 服务端会把同一职位的多人拆成多条记录（原画 66 人、补间 66 人…），
 * 直接逐条渲染既重复又太长，因此这里先按 position 归并。
 *
 * 排序策略：只保留能准确命名的职位（过滤掉一大片「其他」），
 * 再按「出现人数少的优先」——导演/脚本这类关键岗位通常就 1~3 人，
 * 而原画/补间动辄几十人，放最后更符合阅读习惯。
 */
private fun buildAniStaffRows(
    staff: List<com.android.purebilibili.data.model.animeko.AniStaffMember>,
): List<Pair<String, List<String>>> {
    if (staff.isEmpty()) return emptyList()
    return staff
        .filter { com.android.purebilibili.data.model.animeko.isKnownAniStaffPosition(it.position) }
        .groupBy { it.position }
        .map { (position, members) ->
            val label = com.android.purebilibili.data.model.animeko.aniStaffPositionLabel(position)
            val names = members
                .mapNotNull { m ->
                    m.person.displayName.takeIf { it.isNotBlank() }
                }
                .distinct()
            label to names
        }
        .filter { it.second.isNotEmpty() }
        .sortedWith(compareBy({ it.second.size }, { it.first }))
}

/**
 * 短评区块（Bangumi + animeko 聚合）。
 *
 * 三种状态：加载中 / 失败可重试 / 成功（含空态与「加载更多」）。
 */
@Composable
private fun AniReviewsSection(
    state: AniReviewsState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    // Idle = 区块被设置关掉，或还没开始加载 —— 整块不渲染
    if (state is AniReviewsState.Idle) return
    val colorScheme = MaterialTheme.colorScheme
    AniSectionHeader(
        title = "短评",
        action = {
            if (state is AniReviewsState.Success && state.items.isNotEmpty()) {
                AppText(
                    text = "来自 Bangumi",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        },
    )
    Spacer(Modifier.height(8.dp))

    when (state) {
        is AniReviewsState.Idle -> Unit

        is AniReviewsState.Loading -> {
            AppText(
                text = "加载中…",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }

        is AniReviewsState.Error -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppText(
                    text = "短评加载失败",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
                AppText(
                    text = "点此重试",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.primary,
                    modifier = Modifier.clickableNoRipple(onRetry),
                )
            }
        }

        is AniReviewsState.Success -> {
            if (state.items.isEmpty()) {
                AppText(
                    text = "还没有短评",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
                return
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.items.forEach { review ->
                    AniReviewCard(
                        authorName = review.displayAuthor,
                        avatarUrl = review.author.avatarUrl,
                        content = review.displayContent,
                        rating = review.rating,
                        sourceLabel = review.sourceLabel,
                        timeLabel = aniRelativeTime(review.updatedAt),
                        likeCount = review.likeCount,
                    )
                }
                if (state.hasMore) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickableNoRippleIf(!state.isLoadingMore, onLoadMore)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppText(
                            text = if (state.isLoadingMore) "加载中…" else "加载更多短评",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.primary,
                        )
                    }
                } else {
                    AppText(
                        text = "— 已显示全部短评 —",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/** 条件版无涟漪点击：条件不满足时点击无效（用于「加载更多」防重复触发）。 */
@Composable
private fun Modifier.clickableNoRippleIf(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) clickableNoRipple(onClick) else this

/**
 * 角色定位排序权重: 主角(1) -> 配角(2) -> 客串(4) -> 未知。
 * 用于详情页把主角排在声优列表最前面。
 */
private fun aniCharacterRoleRank(role: Int): Int = when (role) {
    1 -> 0
    2 -> 1
    4 -> 2
    else -> 3
}
