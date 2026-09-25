// 文件路径: feature/anime/AnimeFollowScreen.kt
//
// 「追番 / 番剧」页 —— 番剧首页。
//
// ★ 顶部搜索框走 animeko 自己的搜索 (/v2/subjects/search?q=), 与 B 站无关。
// ★ 未搜索时按页签浏览:
//     推荐  = animeko 首页推荐 (total=200, 支持翻页加载更多)
//     热榜  = animeko /v1/trends (趋势榜)
//     分类  = 按标签浏览 (搜索接口 tags 参数, 可翻页)
//     新番  = 当季新番时间表 (/v1/schedule/season/{id}, 可切季度)
//   「我的追番」(本地 Room 收藏) 一直显示在页签上方(有收藏才显示)。
package com.android.purebilibili.feature.anime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.ImmersiveAppScaffold as AppScaffold
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.data.model.animeko.AniCategoryCatalog
import com.android.purebilibili.data.model.animeko.displayName

/** 首页页签定义。二级入口(我的→追番)最前面多一个「我的追番」收藏列表。 */
private val MAIN_TABS = listOf("推荐", "热榜", "分类", "新番")
private val SECONDARY_TABS = listOf("我的追番") + MAIN_TABS

/**
 * 番剧页。
 *
 * 同时承担两个入口：
 * - 底部「番剧」主页面（[isMainPage] = true）：不显示返回键。
 * - 「我的 → 番剧 → 追番」二级入口（[isMainPage] = false）：保留返回键。
 *
 * @param onBack 返回（主页面形态下不会展示返回键）
 * @param onSubjectClick 点卡片进详情
 */
@Composable
fun AnimeFollowScreen(
    onBack: () -> Unit,
    onSubjectClick: (Long) -> Unit,
    onSettingsClick: () -> Unit = {},
    isMainPage: Boolean = false,
    viewModel: AnimeViewModel = viewModel(),
) {
    var keyword by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val discoverState by viewModel.discoverState.collectAsStateWithLifecycle()
    val trendsState by viewModel.trendsState.collectAsStateWithLifecycle()
    val categoryState by viewModel.categoryState.collectAsStateWithLifecycle()
    val seasonState by viewModel.seasonState.collectAsStateWithLifecycle()
    val seasonCoverCache by viewModel.seasonCoverCache.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val colorScheme = MaterialTheme.colorScheme

    // 各页签懒加载: 切到哪个 tab 才拉哪个数据源(各有一次缓存)
    LaunchedEffect(isMainPage) {
        if (isMainPage) viewModel.ensureDiscoverLoaded()
    }
    // 新番页签: 番表到达后异步补齐封面
    LaunchedEffect(seasonState) {
        val st = seasonState
        if (st is AniSeasonState.Success && st.items.isNotEmpty()) {
            viewModel.ensureSeasonCovers(st.items.distinctBy { it.bangumiId })
        }
    }
    LaunchedEffect(selectedTab, isMainPage) {
        // 与内容映射一致: 二级形态 tab0 是收藏(本地数据, 无需网络)
        val tabIndex = if (isMainPage) selectedTab else selectedTab - 1
        when (tabIndex) {
            0 -> viewModel.ensureDiscoverLoaded()
            1 -> viewModel.ensureTrendsLoaded()
            3 -> viewModel.ensureSeasonLoaded()
        }
    }

    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = if (isMainPage) "番剧" else "追番",
                navigationIcon = {
                    // 底部主页面是顶层，给返回键会让人误以为还能退；只在二级入口展示。
                    if (!isMainPage) {
                        AppIconButton(onClick = onBack) {
                            AppIcon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "返回",
                            )
                        }
                    }
                },
                actions = {
                    // 番剧自有设置入口（与主 App 设置无关）
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = AppSpacingTokens.Medium,
                    end = AppSpacingTokens.Medium,
                ),
        ) {
            // ---- 顶部搜索框: 走 animeko 自己的搜索 ----
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacingTokens.Small),
                placeholder = {
                    AppText(
                        text = "搜索番剧(animeko 数据源)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    AppIcon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "搜索",
                    )
                },
                trailingIcon = {
                    if (keyword.isNotEmpty()) {
                        AppIconButton(
                            onClick = {
                                keyword = ""
                                viewModel.clearSearch()
                            },
                        ) {
                            AppIcon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "清空",
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboard?.hide()
                        viewModel.search(keyword)
                    },
                ),
            )

            when (val state = searchState) {
                is AniSearchState.Idle -> {
                    // ★ v4.0.0: 「我的 → 番剧 → 追番」二级入口**只显示追番列表**
                    //   (不再出现 推荐/热榜/分类/新番 页签); 底部「番剧」主页面保留页签。
                    if (!isMainPage) {
                        FavoritesTab(
                            favorites = favorites,
                            onSubjectClick = onSubjectClick,
                        )
                    } else {
                    // 页签
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = colorScheme.primary,
                    ) {
                        (if (isMainPage) MAIN_TABS else SECONDARY_TABS).forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    AppText(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(AppSpacingTokens.Small))

                    // 二级形态的 tab0 是「我的追番」, 其余页签整体右移一位
                    val tabIndex = if (isMainPage) selectedTab else selectedTab - 1
                    when (tabIndex) {
                        -1 -> FavoritesTab(
                            favorites = favorites,
                            onSubjectClick = onSubjectClick,
                        )

                        0 -> DiscoverTab(
                            state = discoverState,
                            onSubjectClick = onSubjectClick,
                            onRetry = { viewModel.reloadDiscover() },
                            onLoadMore = { viewModel.loadMoreDiscover() },
                        )

                        1 -> TrendsTab(
                            state = trendsState,
                            onSubjectClick = onSubjectClick,
                            onRetry = { viewModel.reloadTrends() },
                        )

                        2 -> CategoryTab(
                            state = categoryState,
                            onSubjectClick = onSubjectClick,
                            onSelectTag = { viewModel.selectCategory(it) },
                            onLoadMore = { viewModel.loadMoreCategory() },
                        )

                        else -> SeasonTab(
                            state = seasonState,
                            coverCache = seasonCoverCache,
                            onSubjectClick = onSubjectClick,
                            onSelectSeason = { viewModel.selectSeason(it) },
                            onRetry = { viewModel.reloadSeason() },
                        )
                    }
                    }
                }

                is AniSearchState.Loading -> AniEmptyState(text = "搜索中…")

                is AniSearchState.Error -> AniEmptyState(
                    text = "搜索失败",
                    hint = state.message,
                )

                is AniSearchState.Success -> {
                    if (state.items.isEmpty()) {
                        AniEmptyState(text = "没有找到相关番剧", hint = "换个关键词试试")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = AppSpacingTokens.Small,
                                bottom = AppSpacingTokens.Large,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                        ) {
                            items(state.items, key = { it.id }) { item ->
                                AniSubjectCard(
                                    cover = item.imageLarge,
                                    title = item.displayName,
                                    subtitle = item.yearLabel,
                                    score = item.scoreOrNull,
                                    onClick = { onSubjectClick(item.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 「我的追番」页签(二级入口): 本地收藏网格 —— 番剧页点「已追番」后的列表。 */
@Composable
private fun FavoritesTab(
    favorites: List<com.android.purebilibili.core.database.entity.AniFavoriteSubject>,
    onSubjectClick: (Long) -> Unit,
) {
    if (favorites.isEmpty()) {
        AniEmptyState(
            text = "还没有追番",
            hint = "搜索番剧, 进详情页点「已追番」即可加入这里",
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = AppSpacingTokens.Small,
            bottom = AppSpacingTokens.Large,
        ),
        horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
    ) {
        items(favorites, key = { "fav-${it.subjectId}" }) { item ->
            AniSubjectCard(
                cover = item.cover,
                title = item.displayName,
                subtitle = aniFavoriteProgressLabel(item.watchedEpisode, item.totalEpisodes)
                    .ifBlank { item.airDate },
                score = item.score.toFloatOrNull(),
                onClick = { onSubjectClick(item.subjectId) },
            )
        }
    }
}

/** 「推荐」页签: animeko 首页推荐 + 加载更多。 */
@Composable
private fun DiscoverTab(
    state: AniDiscoverState,
    onSubjectClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    when (state) {
        is AniDiscoverState.Loading -> AniEmptyState(text = "加载中…")
        is AniDiscoverState.Error -> AniEmptyState(
            text = "推荐加载失败",
            hint = state.message,
            onRetry = onRetry,
        )

        is AniDiscoverState.Success -> AniPagedGrid(
            items = state.items,
            hasMore = state.hasMore,
            isLoadingMore = state.isLoadingMore,
            error = state.error,
            keyPrefix = "discover",
            cover = { it.cover },
            title = { it.title },
            subtitle = { it.subtitle },
            onSubjectClick = { onSubjectClick(it.id) },
            onLoadMore = onLoadMore,
        )
    }
}

/** 「热榜」页签: animeko /v1/trends。 */
@Composable
private fun TrendsTab(
    state: AniTrendsState,
    onSubjectClick: (Long) -> Unit,
    onRetry: () -> Unit,
) {
    when (val st = state) {
        is AniTrendsState.Loading -> AniEmptyState(text = "热榜加载中…")
        is AniTrendsState.Error -> AniEmptyState(
            text = "热榜加载失败",
            hint = st.message,
            onRetry = onRetry,
        )

        is AniTrendsState.Success -> LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = AppSpacingTokens.Small,
                bottom = AppSpacingTokens.Large,
            ),
            horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AniGridSectionHeader(title = "实时趋势榜", trailing = "${st.items.size} 部")
            }
            itemsIndexed(st.items, key = { _, item -> "trend-${item.id}" }) { index, item ->
                AniSubjectCard(
                    cover = item.cover,
                    title = item.title,
                    subtitle = item.subtitle,
                    badge = "#${index + 1}",
                    onClick = { onSubjectClick(item.id) },
                )
            }
        }
    }
}

/** 「分类」页签: 分类 chips + 该分类的网格(可翻页)。 */
@Composable
private fun CategoryTab(
    state: AniCategoryState,
    onSubjectClick: (Long) -> Unit,
    onSelectTag: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    var showAllCategories by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf(AniCategoryCatalog.primary.first()) }

    // 记住外部选中的标签(点 chip 时同步)
    if (state is AniCategoryState.Success && state.tag != selectedTag) {
        selectedTag = state.tag
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 分类筛选条
        if (showAllCategories) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = AppSpacingTokens.ExtraSmall),
            ) {
                AniCategoryCatalog.all.forEach { group ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AppText(
                            text = group.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    items(group.tags, key = { "cat-$it" }) { tag ->
                        AniCategoryChip(
                            label = tag,
                            selected = tag == selectedTag,
                            onClick = {
                                selectedTag = tag
                                onSelectTag(tag)
                                showAllCategories = false
                            },
                        )
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                item {
                    AniCategoryChip(
                        label = "全部 ▾",
                        selected = false,
                        onClick = { showAllCategories = true },
                    )
                }
                items(AniCategoryCatalog.primary, key = { "primary-$it" }) { tag ->
                    AniCategoryChip(
                        label = tag,
                        selected = tag == selectedTag,
                        onClick = {
                            selectedTag = tag
                            onSelectTag(tag)
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(AppSpacingTokens.ExtraSmall))

        when (val st = state) {
            is AniCategoryState.Idle -> {
                // 首次进入: 自动加载默认分类
                LaunchedEffect(Unit) { onSelectTag(selectedTag) }
                AniEmptyState(text = "选择一个分类开始浏览")
            }

            is AniCategoryState.Loading -> AniEmptyState(text = "「${selectedTag}」加载中…")

            is AniCategoryState.Error -> AniEmptyState(
                text = "「${st.tag}」加载失败",
                hint = st.message,
                onRetry = { onSelectTag(st.tag) },
            )

            is AniCategoryState.Success -> AniPagedGrid(
                items = st.items,
                hasMore = st.hasMore,
                isLoadingMore = st.isLoadingMore,
                error = null,
                keyPrefix = "cat",
                cover = { it.imageLarge },
                title = { it.displayName },
                subtitle = { it.yearLabel.ifBlank { null } },
                scoreOf = { it.scoreOrNull },
                onSubjectClick = { onSubjectClick(it.id) },
                onLoadMore = onLoadMore,
                emptyHint = "「${st.tag}」暂时没有内容",
            )
        }
    }
}

/** 「新番」页签: 季度切换 + 番表。 */
@Composable
private fun SeasonTab(
    state: AniSeasonState,
    coverCache: Map<Long, String>,
    onSubjectClick: (Long) -> Unit,
    onSelectSeason: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when (val st = state) {
        is AniSeasonState.Loading -> AniEmptyState(text = "新番时间表加载中…")
        is AniSeasonState.Error -> AniEmptyState(
            text = "新番时间表加载失败",
            hint = st.message,
            onRetry = onRetry,
        )

        is AniSeasonState.Success -> Column(modifier = Modifier.fillMaxSize()) {
            if (st.seasons.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    items(st.seasons, key = { it.id }) { season ->
                        AniCategoryChip(
                            label = season.displayName,
                            selected = season.id == st.selectedSeasonId,
                            onClick = { onSelectSeason(season.id) },
                        )
                    }
                }
                Spacer(Modifier.height(AppSpacingTokens.ExtraSmall))
            }
            if (st.items.isEmpty()) {
                AniEmptyState(text = "该季度暂无番表")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = AppSpacingTokens.Small,
                        bottom = AppSpacingTokens.Large,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                    verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                ) {
                    itemsIndexed(
                        st.items.distinctBy { it.bangumiId },
                        key = { _, item -> "season-${item.bangumiId}" },
                    ) { _, item ->
                        AniSubjectCard(
                            cover = coverCache[item.bangumiId].orEmpty(),
                            title = item.displayName,
                            subtitle = item.begin.take(10).ifBlank { null },
                            badge = if (item.mikanId != null) "BT" else null,
                            onClick = { onSubjectClick(item.bangumiId) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 「推荐 / 分类」共用的分页网格。
 *
 * 滚到倒数第 6 个时自动触发 [onLoadMore] —— 用户感知不到「加载更多」按钮,
 * 列表就是一直往下划一直有。
 */
@Composable
private fun <T> AniPagedGrid(
    items: List<T>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    error: String?,
    keyPrefix: String,
    cover: (T) -> String,
    title: (T) -> String,
    subtitle: (T) -> String?,
    onSubjectClick: (T) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    scoreOf: ((T) -> Float?)? = null,
    emptyHint: String = "暂无内容",
) {
    if (items.isEmpty()) {
        AniEmptyState(text = emptyHint, hint = error)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = AppSpacingTokens.Small,
            bottom = AppSpacingTokens.Large,
        ),
        horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
    ) {
        itemsIndexed(items, key = { _, item -> "$keyPrefix-${title(item)}-${cover(item).hashCode()}" }) { index, item ->
            AniSubjectCard(
                cover = cover(item),
                title = title(item),
                subtitle = subtitle(item),
                score = scoreOf?.invoke(item),
                onClick = { onSubjectClick(item) },
            )
            // 预加载: 剩 6 个未上屏时就拉下一页
            if (hasMore && index >= items.size - 6 && !isLoadingMore) {
                LaunchedEffect(items.size) { onLoadMore() }
            }
        }
        if (isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacingTokens.Small),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AppText(
                        text = "加载中…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (!hasMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacingTokens.Small),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AppText(
                        text = "已经到底了 (共 ${items.size} 部)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 分类筛选 chip。 */
@Composable
private fun AniCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = container,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            AppText(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 未搜索时的番剧首页内容（旧版两段式, 已被页签替代, 保留给二级「追番」入口退路）。
 */
@Composable
private fun FollowAndDiscoverGrid(
    favorites: List<com.android.purebilibili.core.database.entity.AniFavoriteSubject>,
    discoverState: AniDiscoverState,
    onSubjectClick: (Long) -> Unit,
    onRetryDiscover: () -> Unit,
) {
    val discoverItems = (discoverState as? AniDiscoverState.Success)?.items.orEmpty()

    if (favorites.isEmpty() && discoverItems.isEmpty()) {
        when (discoverState) {
            is AniDiscoverState.Loading -> AniEmptyState(text = "加载中…")

            is AniDiscoverState.Error -> AniEmptyState(
                text = "还没有追番",
                hint = "搜索番剧，进详情页点「追番」即可加入。推荐加载失败：${discoverState.message}",
            )

            is AniDiscoverState.Success -> AniEmptyState(
                text = "还没有追番",
                hint = "搜索番剧，进详情页点「追番」即可加入",
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = AppSpacingTokens.Small,
            bottom = AppSpacingTokens.Large,
        ),
        horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
    ) {
        if (favorites.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AniGridSectionHeader(
                    title = "我的追番",
                    trailing = "${favorites.size} 部",
                )
            }
            items(favorites, key = { "fav-${it.subjectId}" }) { item ->
                AniSubjectCard(
                    cover = item.cover,
                    title = item.displayName,
                    subtitle = aniFavoriteProgressLabel(item.watchedEpisode, item.totalEpisodes)
                        .ifBlank { item.airDate },
                    score = item.score.toFloatOrNull(),
                    onClick = { onSubjectClick(item.subjectId) },
                )
            }
        }

        if (discoverItems.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AniGridSectionHeader(title = "发现 · 推荐")
            }
            items(discoverItems, key = { "discover-${it.id}" }) { item ->
                AniSubjectCard(
                    cover = item.cover,
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = { onSubjectClick(item.id) },
                )
            }
        } else if (discoverState is AniDiscoverState.Error) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AniGridSectionHeader(
                    title = "发现 · 推荐",
                    trailing = "加载失败",
                    onRetry = onRetryDiscover,
                )
            }
        }
    }
}

/** 番剧首页的分段标题（整行占位）。 */
@Composable
private fun AniGridSectionHeader(
    title: String,
    trailing: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppSpacingTokens.Small, bottom = AppSpacingTokens.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(AppSpacingTokens.Small))
            AppText(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onRetry != null) {
            Spacer(modifier = Modifier.weight(1f))
            AppTextButton(onClick = onRetry) {
                AppText(text = "重试", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
