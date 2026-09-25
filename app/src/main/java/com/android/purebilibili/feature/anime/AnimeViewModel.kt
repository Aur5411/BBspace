// 文件路径: feature/anime/AnimeViewModel.kt
//
// 「追番」模块 ViewModel。
//
// ★ 数据来源二分为:
//   - 网络: AnimekoRepository (api.animeko.org, 免登录, 不碰 B 站)
//   - 本地: AnimekoLocalRepository (Room: 收藏 / 历史 / 缓存)
package com.android.purebilibili.feature.anime

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.database.entity.AniFavoriteSubject
import com.android.purebilibili.core.database.entity.AniLocalDanmaku
import com.android.purebilibili.core.database.entity.AniWatchHistory
import com.android.purebilibili.data.model.animeko.AniDanmaku
import com.android.purebilibili.data.model.animeko.AniDanmakuInfo
import com.android.purebilibili.data.model.animeko.AniDanmakuItem
import com.android.purebilibili.data.model.animeko.AniEpisode
import com.android.purebilibili.data.model.animeko.AniMediaSourceKind
import com.android.purebilibili.data.model.animeko.AniRelatedCharacter
import com.android.purebilibili.data.model.animeko.AniReview
import com.android.purebilibili.data.model.animeko.AniStaffMember
import com.android.purebilibili.data.model.animeko.AniSubjectBrief
import com.android.purebilibili.data.model.animeko.AniSubjectDetail
import com.android.purebilibili.data.model.animeko.AniMediaQuery
import com.android.purebilibili.data.repository.AniMediaSearchResult
import com.android.purebilibili.data.repository.AniMediaSourceRepository
import com.android.purebilibili.data.repository.ANI_BROWSE_PAGE_SIZE
import com.android.purebilibili.data.repository.ANI_REVIEW_PAGE_SIZE
import com.android.purebilibili.data.repository.AniResult
import com.android.purebilibili.data.repository.AnimekoLocalRepository
import com.android.purebilibili.data.repository.AnimekoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 番剧首页各页签的单页条数(服务端单页上限 100, 取 30 兼顾首屏速度)。 */
private const val ANI_BROWSE_PAGE_SIZE = 30

/** 搜索结果状态。 */
sealed interface AniSearchState {
    data object Idle : AniSearchState
    data object Loading : AniSearchState
    data class Success(val items: List<AniSubjectBrief>) : AniSearchState
    data class Error(val message: String) : AniSearchState
}

/** 番剧首页「发现」区块的数据。 */
sealed interface AniDiscoverState {
    data object Loading : AniDiscoverState
    data class Success(
        val items: List<AniDiscoverItem>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        val error: String? = null,
    ) : AniDiscoverState

    data class Error(val message: String) : AniDiscoverState
}

/** 「热榜」(animeko /v1/trends) 数据。 */
sealed interface AniTrendsState {
    data object Loading : AniTrendsState
    data class Success(val items: List<AniDiscoverItem>) : AniTrendsState
    data class Error(val message: String) : AniTrendsState
}

/** 「分类」页签数据: 按标签浏览, 可翻页。 */
sealed interface AniCategoryState {
    data object Idle : AniCategoryState
    data object Loading : AniCategoryState
    data class Success(
        val tag: String,
        val items: List<AniSubjectBrief>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
    ) : AniCategoryState

    data class Error(val tag: String, val message: String) : AniCategoryState
}

/** 「新番」页签: 季度列表 + 当前选中季度的番表。 */
sealed interface AniSeasonState {
    data object Loading : AniSeasonState
    data class Success(
        val seasons: List<com.android.purebilibili.data.model.animeko.AniSeasonId>,
        val selectedSeasonId: String,
        val items: List<com.android.purebilibili.data.model.animeko.AniScheduleItem>,
        /** 番表条目按 subjectId 去重后的封面缓存(番表接口不带图, 点进详情才有)。 */
    ) : AniSeasonState

    data class Error(val message: String) : AniSeasonState
}

/** 番剧首页卡片的最小展示模型。 */
data class AniDiscoverItem(
    val id: Long,
    val title: String,
    val cover: String,
    val subtitle: String? = null,
)

/** 详情状态。 */
sealed interface AniDetailState {
    data object Loading : AniDetailState
    data class Success(val detail: AniSubjectDetail) : AniDetailState
    data class Error(val message: String) : AniDetailState
}

/** 换源状态。 */
sealed interface AniSourceState {
    data object Idle : AniSourceState
    data object Loading : AniSourceState
    data class Success(val result: AniMediaSearchResult) : AniSourceState
    data class Error(val message: String) : AniSourceState
}

/** 短评区块状态。 */
sealed interface AniReviewsState {
    data object Idle : AniReviewsState
    data object Loading : AniReviewsState

    data class Success(
        val items: List<AniReview>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
    ) : AniReviewsState

    data class Error(val message: String) : AniReviewsState
}

class AnimeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext get() = getApplication<Application>()

    /**
     * 追番播放器实例。
     *
     * ★ 放在 ViewModel 里而不是 remember{}: 全屏旋转会触发 Activity 重建,
     *   remember 的播放器会连着播放状态一起丢掉 —— 表现就是「一点全屏就黑屏」。
     *   ViewModel 横跨配置变化存活, 转屏后继续无缝播。
     */
    val exoPlayer: androidx.media3.exoplayer.ExoPlayer by lazy {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(appContext)
            .setEnableDecoderFallback(true)
            .setMediaCodecSelector(
                AniMediaCodecSelector(AniSettingsStore.snapshot(appContext).hardwareDecodePreferred)
            )
        androidx.media3.exoplayer.ExoPlayer.Builder(appContext, renderersFactory)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                    androidx.media3.datasource.DefaultDataSource.Factory(
                        appContext,
                        androidx.media3.datasource.DefaultHttpDataSource.Factory()
                            .setUserAgent(ANI_PLAYER_UA)
                            .setAllowCrossProtocolRedirects(true)
                            .setConnectTimeoutMs(15_000)
                            .setReadTimeoutMs(20_000)
                    )
                )
            )
            .build()
            // playWhenReady 交给页面控制: 画面就绪后再播, 避免「只有声音没画面」
            .apply { playWhenReady = false }
    }

    /** 与播放器同生命周期的 UA 常量。 */
    private companion object {
        const val ANI_PLAYER_UA =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    // -----------------------------------------------------------------
    // 本地数据 (Room)
    // -----------------------------------------------------------------

    /** 本地追番收藏。 */
    val favorites: StateFlow<List<AniFavoriteSubject>> =
        AnimekoLocalRepository.observeFavorites(appContext)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 本地追番历史。 */
    val history: StateFlow<List<AniWatchHistory>> =
        AnimekoLocalRepository.observeHistory(appContext)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // -----------------------------------------------------------------
    // 搜索 (animeko 自己的搜索, 与 B 站无关)
    // -----------------------------------------------------------------

    private val _searchState = MutableStateFlow<AniSearchState>(AniSearchState.Idle)
    val searchState: StateFlow<AniSearchState> = _searchState.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    /**
     * 用 animeko 的规则搜索番剧。
     *
     * 走 /v2/subjects/search?q=..., 完全走 animeko 数据源。
     * 请求条数取「番剧设置 → 搜索结果数量」（AniSettingsStore，与主设置无关）。
     */
    fun search(keyword: String) {
        val q = keyword.trim()
        searchJob?.cancel()
        if (q.isEmpty()) {
            _searchState.value = AniSearchState.Idle
            return
        }
        val limit = AniSettingsStore.snapshot(appContext).searchResultLimit
        searchJob = viewModelScope.launch {
            _searchState.value = AniSearchState.Loading
            when (val r = AnimekoRepository.search(q, limit = limit)) {
                is AniResult.Ok -> _searchState.value = AniSearchState.Success(r.value)
                is AniResult.Err -> _searchState.value = AniSearchState.Error(r.message)
            }
        }
    }

    /** 清空搜索态, 回到「我的追番」列表。 */
    fun clearSearch() {
        searchJob?.cancel()
        _searchState.value = AniSearchState.Idle
    }

    // -----------------------------------------------------------------
    // 番剧首页 (发现 / 热榜 / 分类 / 新番)
    // -----------------------------------------------------------------

    private val _discoverState = MutableStateFlow<AniDiscoverState>(AniDiscoverState.Loading)
    val discoverState: StateFlow<AniDiscoverState> = _discoverState.asStateFlow()

    private val _trendsState = MutableStateFlow<AniTrendsState>(AniTrendsState.Loading)
    val trendsState: StateFlow<AniTrendsState> = _trendsState.asStateFlow()

    private val _categoryState = MutableStateFlow<AniCategoryState>(AniCategoryState.Idle)
    val categoryState: StateFlow<AniCategoryState> = _categoryState.asStateFlow()

    private val _seasonState = MutableStateFlow<AniSeasonState>(AniSeasonState.Loading)
    val seasonState: StateFlow<AniSeasonState> = _seasonState.asStateFlow()

    private var discoverJob: kotlinx.coroutines.Job? = null
    private var discoverMoreJob: kotlinx.coroutines.Job? = null
    private var trendsJob: kotlinx.coroutines.Job? = null
    private var categoryJob: kotlinx.coroutines.Job? = null
    private var categoryMoreJob: kotlinx.coroutines.Job? = null
    private var seasonJob: kotlinx.coroutines.Job? = null

    /** 首次进入番剧首页时懒加载一次；已有数据则不重复请求。 */
    fun ensureDiscoverLoaded() {
        if (_discoverState.value is AniDiscoverState.Success) return
        reloadDiscover()
    }

    fun ensureTrendsLoaded() {
        if (_trendsState.value is AniTrendsState.Success) return
        reloadTrends()
    }

    fun ensureSeasonLoaded() {
        if (_seasonState.value is AniSeasonState.Success) return
        reloadSeason()
    }

    /**
     * 拉取番剧首页「推荐」首屏。
     *
     * ★ offset/limit 现在直接透传给服务端(它支持到 total=200),
     *   修复了旧实现只在本地切片、永远只有 20 条的 bug。
     */
    fun reloadDiscover() {
        discoverJob?.cancel()
        discoverMoreJob?.cancel()
        discoverJob = viewModelScope.launch {
            _discoverState.value = AniDiscoverState.Loading
            when (val r = AnimekoRepository.recommendations(offset = 0, limit = ANI_BROWSE_PAGE_SIZE)) {
                is AniResult.Ok -> {
                    val items = r.value.items.toDiscoverItems()
                    _discoverState.value = if (items.isEmpty()) {
                        AniDiscoverState.Error("暂无推荐内容")
                    } else {
                        AniDiscoverState.Success(
                            items = items,
                            hasMore = AnimekoRepository.hasMoreRecommendations(r.value, items.size),
                        )
                    }
                }

                is AniResult.Err -> _discoverState.value = AniDiscoverState.Error(r.message)
            }
        }
    }

    /** 「推荐」加载下一页。 */
    fun loadMoreDiscover() {
        val current = _discoverState.value
        if (current !is AniDiscoverState.Success || current.isLoadingMore || !current.hasMore) return
        discoverMoreJob?.cancel()
        discoverMoreJob = viewModelScope.launch {
            _discoverState.value = current.copy(isLoadingMore = true)
            val offset = current.items.size
            when (val r = AnimekoRepository.recommendations(offset = offset, limit = ANI_BROWSE_PAGE_SIZE)) {
                is AniResult.Ok -> {
                    val fresh = r.value.items.toDiscoverItems()
                    val merged = (current.items + fresh).distinctBy { it.id }
                    _discoverState.value = AniDiscoverState.Success(
                        items = merged,
                        hasMore = AnimekoRepository.hasMoreRecommendations(r.value, merged.size),
                    )
                }

                is AniResult.Err -> {
                    // 翻页失败保留已有内容, 把错误挂到状态上让 UI 提示
                    (_discoverState.value as? AniDiscoverState.Success)?.let { keep ->
                        _discoverState.value = keep.copy(isLoadingMore = false, error = r.message)
                    }
                }
            }
        }
    }

    private fun List<com.android.purebilibili.data.model.animeko.AniRecommendItem>.toDiscoverItems():
        List<AniDiscoverItem> = map { item ->
            AniDiscoverItem(
                id = item.subjectId,
                title = item.displayName,
                cover = item.imageUrl,
                subtitle = item.desc1.takeIf { it.isNotBlank() },
            )
        }.filter { it.id > 0L && it.title.isNotBlank() }

    // ---- 热榜 ----

    fun reloadTrends() {
        trendsJob?.cancel()
        trendsJob = viewModelScope.launch {
            _trendsState.value = AniTrendsState.Loading
            when (val r = AnimekoRepository.trends()) {
                is AniResult.Ok -> {
                    val items = r.value.trendingSubjects.map {
                        AniDiscoverItem(
                            id = it.bangumiId,
                            title = it.nameCn.ifBlank { "未知条目" },
                            cover = it.imageLarge,
                            subtitle = null,
                        )
                    }.filter { it.id > 0L }
                    _trendsState.value = if (items.isEmpty()) {
                        AniTrendsState.Error("热榜暂无内容")
                    } else {
                        AniTrendsState.Success(items)
                    }
                }

                is AniResult.Err -> _trendsState.value = AniTrendsState.Error(r.message)
            }
        }
    }

    // ---- 分类 ----

    /** 选中某个分类标签并加载首页。 */
    fun selectCategory(tag: String) {
        categoryJob?.cancel()
        categoryMoreJob?.cancel()
        _categoryState.value = AniCategoryState.Loading
        categoryJob = viewModelScope.launch {
            when (val r = AnimekoRepository.browseByTags(listOf(tag), offset = 0)) {
                is AniResult.Ok -> _categoryState.value = AniCategoryState.Success(
                    tag = tag,
                    items = r.value,
                    hasMore = r.value.size >= ANI_BROWSE_PAGE_SIZE,
                )

                is AniResult.Err -> _categoryState.value = AniCategoryState.Error(tag, r.message)
            }
        }
    }

    /** 分类加载下一页。 */
    fun loadMoreCategory() {
        val current = _categoryState.value
        if (current !is AniCategoryState.Success || current.isLoadingMore || !current.hasMore) return
        categoryMoreJob?.cancel()
        categoryMoreJob = viewModelScope.launch {
            _categoryState.value = current.copy(isLoadingMore = true)
            when (val r = AnimekoRepository.browseByTags(
                tags = listOf(current.tag),
                offset = current.items.size,
            )) {
                is AniResult.Ok -> {
                    val merged = (current.items + r.value).distinctBy { it.id }
                    _categoryState.value = AniCategoryState.Success(
                        tag = current.tag,
                        items = merged,
                        hasMore = r.value.size >= ANI_BROWSE_PAGE_SIZE,
                    )
                }

                is AniResult.Err -> _categoryState.value =
                    current.copy(isLoadingMore = false)
            }
        }
    }

    // ---- 新番 ----

    fun reloadSeason() {
        seasonJob?.cancel()
        seasonJob = viewModelScope.launch {
            _seasonState.value = AniSeasonState.Loading
            when (val r = AnimekoRepository.latestSeasonSubjects()) {
                is AniResult.Ok -> {
                    val seasons = when (val s = AnimekoRepository.seasonList()) {
                        is AniResult.Ok -> s.value
                        is AniResult.Err -> emptyList()
                    }
                    val newest = seasons.firstOrNull()?.id
                    _seasonState.value = AniSeasonState.Success(
                        seasons = seasons,
                        selectedSeasonId = newest.orEmpty(),
                        items = r.value,
                    )
                }

                is AniResult.Err -> _seasonState.value = AniSeasonState.Error(r.message)
            }
        }
    }

    // 新番番表条目没有封面字段, 按 bangumiId 异步拉详情补齐(结果缓存)
    private val _seasonCoverCache = MutableStateFlow<Map<Long, String>>(emptyMap())
    val seasonCoverCache: StateFlow<Map<Long, String>> = _seasonCoverCache.asStateFlow()

    /** 为番表条目并发拉取封面(去重, 已缓存的跳过)。 */
    fun ensureSeasonCovers(items: List<com.android.purebilibili.data.model.animeko.AniScheduleItem>) {
        val missing = items.map { it.bangumiId }
            .distinct()
            .filter { it > 0L && _seasonCoverCache.value[it] == null }
        if (missing.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            missing.take(60).forEach { sid ->
                when (val r = AnimekoRepository.subject(sid)) {
                    is AniResult.Ok -> {
                        _seasonCoverCache.value = _seasonCoverCache.value + (sid to r.value.cover)
                    }

                    is AniResult.Err -> {
                        _seasonCoverCache.value = _seasonCoverCache.value + (sid to "")
                    }
                }
            }
        }
    }

    /** 切换季度。 */
    fun selectSeason(seasonId: String) {
        val current = _seasonState.value
        if (current is AniSeasonState.Success && current.selectedSeasonId == seasonId) return
        seasonJob?.cancel()
        _seasonState.value = AniSeasonState.Loading
        seasonJob = viewModelScope.launch {
            when (val r = AnimekoRepository.seasonSubjects(seasonId)) {
                is AniResult.Ok -> _seasonState.value = AniSeasonState.Success(
                    seasons = (current as? AniSeasonState.Success)?.seasons.orEmpty(),
                    selectedSeasonId = seasonId,
                    items = r.value,
                )

                is AniResult.Err -> _seasonState.value =
                    AniSeasonState.Error(r.message)
            }
        }
    }

    // -----------------------------------------------------------------
    // 详情
    // -----------------------------------------------------------------

    private val _detailState = MutableStateFlow<AniDetailState>(AniDetailState.Loading)
    val detailState: StateFlow<AniDetailState> = _detailState.asStateFlow()

    private val _characters = MutableStateFlow<List<AniRelatedCharacter>>(emptyList())
    val characters: StateFlow<List<AniRelatedCharacter>> = _characters.asStateFlow()

    private val _staff = MutableStateFlow<List<AniStaffMember>>(emptyList())
    val staff: StateFlow<List<AniStaffMember>> = _staff.asStateFlow()

    /** 短评分页状态。 */
    private val _reviews = MutableStateFlow<AniReviewsState>(AniReviewsState.Idle)
    val reviews: StateFlow<AniReviewsState> = _reviews.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private var detailJob: kotlinx.coroutines.Job? = null
    private var reviewsJob: kotlinx.coroutines.Job? = null

    fun loadDetail(subjectId: Long) {
        detailJob?.cancel()
        reviewsJob?.cancel()
        detailJob = viewModelScope.launch {
            _detailState.value = AniDetailState.Loading
            _characters.value = emptyList()
            _staff.value = emptyList()
            _reviews.value = AniReviewsState.Idle
            _isFavorite.value = AnimekoLocalRepository.isFavorite(appContext, subjectId)
            // 角色 / 制作人员 / 短评三个区块的开关都来自番剧设置（仅作用于番剧页）
            val aniSettings = AniSettingsStore.snapshot(appContext)
            val wantCharacters = aniSettings.autoLoadCharacters
            val wantStaff = wantCharacters && aniSettings.showStaff
            val wantReviews = aniSettings.showReviews
            when (val r = AnimekoRepository.subject(subjectId)) {
                is AniResult.Ok -> _detailState.value = AniDetailState.Success(r.value)
                is AniResult.Err -> _detailState.value = AniDetailState.Error(r.message)
            }
            // 角色列表是可选区块, 失败不覆盖主内容
            if (wantCharacters) {
                when (val c = AnimekoRepository.characters(subjectId)) {
                    is AniResult.Ok -> _characters.value = c.value
                    is AniResult.Err -> Unit
                }
            }
            if (wantStaff) {
                when (val s = AnimekoRepository.staff(subjectId)) {
                    is AniResult.Ok -> _staff.value = s.value
                    is AniResult.Err -> Unit
                }
            }
            // 短评同样可选, 失败只影响区块自身
            if (wantReviews) {
                loadReviewsInternal(subjectId, append = false)
            } else {
                _reviews.value = AniReviewsState.Idle
            }
        }
    }

    /** 重新加载详情（含短评），用于详情页「评论」区块的重试。 */
    fun reloadDetail(subjectId: Long) {
        if (detailJob?.isActive == true) return
        loadDetail(subjectId)
    }

    // -----------------------------------------------------------------
    // 短评 (Bangumi + animeko 聚合)
    // -----------------------------------------------------------------

    /** 已加载到第几页（用于 offset 计算）。 */
    private var reviewsLoadedPage = 0

    private suspend fun loadReviewsInternal(subjectId: Long, append: Boolean) {
        if (!append) {
            reviewsLoadedPage = 0
            _reviews.value = AniReviewsState.Loading
        } else {
            val current = _reviews.value
            if (current !is AniReviewsState.Success || current.isLoadingMore || !current.hasMore) return
            _reviews.value = current.copy(isLoadingMore = true)
        }
        val offset = if (append) reviewsLoadedPage * ANI_REVIEW_PAGE_SIZE else 0
        when (val r = AnimekoRepository.reviews(subjectId = subjectId, offset = offset)) {
            is AniResult.Ok -> {
                reviewsLoadedPage += 1
                val hasMore = AnimekoRepository.hasMoreReviews(r.value)
                val merged = if (append) {
                    val prev = (_reviews.value as? AniReviewsState.Success)?.items.orEmpty()
                    prev + r.value.items
                } else {
                    r.value.items
                }
                _reviews.value = AniReviewsState.Success(
                    items = merged,
                    hasMore = hasMore,
                    isLoadingMore = false,
                )
            }

            is AniResult.Err -> {
                _reviews.value = if (append) {
                    // 加载更多失败: 保留已有内容, 只结束 loading 态
                    (_reviews.value as? AniReviewsState.Success)
                        ?.copy(isLoadingMore = false)
                        ?: AniReviewsState.Error(r.message)
                } else {
                    AniReviewsState.Error(r.message)
                }
            }
        }
    }

    /** 加载下一页短评。 */
    fun loadMoreReviews(subjectId: Long) {
        reviewsJob?.cancel()
        reviewsJob = viewModelScope.launch {
            loadReviewsInternal(subjectId, append = true)
        }
    }

    /** 重试首屏短评。 */
    fun retryReviews(subjectId: Long) {
        reviewsJob?.cancel()
        reviewsJob = viewModelScope.launch {
            loadReviewsInternal(subjectId, append = false)
        }
    }

    /** 读取某一话上次的播放进度（「记住播放进度」开启时用于续播）。 */
    suspend fun resumePositionFor(episodeId: Long): Long = withContext(Dispatchers.IO) {
        val settings = AniSettingsStore.snapshot(appContext)
        if (!settings.rememberPlaybackPosition || !settings.recordWatchHistory) {
            return@withContext 0L
        }
        AnimekoLocalRepository.historyForEpisode(appContext, episodeId)?.positionMillis ?: 0L
    }

    /** 本地收藏 / 取消收藏 (不联网)。 */
    fun toggleFavorite(detail: AniSubjectDetail) {
        viewModelScope.launch {
            val nowFav = AnimekoLocalRepository.toggleFavorite(appContext, detail)
            _isFavorite.value = nowFav
        }
    }

    // -----------------------------------------------------------------
    // 换源
    // -----------------------------------------------------------------

    private val _sourceState = MutableStateFlow<AniSourceState>(AniSourceState.Idle)
    val sourceState: StateFlow<AniSourceState> = _sourceState.asStateFlow()

    private var sourceJob: kotlinx.coroutines.Job? = null

    /** 「下载」面板的 BT/磁力源检索状态。 */
    private val _downloadSourceState = MutableStateFlow<AniSourceState>(AniSourceState.Idle)
    val downloadSourceState: StateFlow<AniSourceState> = _downloadSourceState.asStateFlow()

    private var downloadSourceJob: kotlinx.coroutines.Job? = null

    /**
     * 为某一话检索可用的媒体源 (换源面板的数据来源)。
     *
     * ★ 应用户要求: 播放时默认**只搜在线源**(m3u8/mp4 直链),
     *   BT/磁力源在「下载」面板里单独检索 —— 避免换源列表被一堆
     *   播不了的种子刷屏。
     */
    fun loadMediaSources(query: AniMediaQuery, onlySourceId: String? = null) {
        sourceJob?.cancel()
        sourceJob = viewModelScope.launch {
            _sourceState.value = AniSourceState.Loading
            // ★ 番剧设置里的「优先源」(有序): 播放时先只搜这些源, 命中即播
            val preferred = AniSourcePreference.getPreferred(appContext)
            var result = AniMediaSourceRepository.search(
                query,
                enabledIds = onlySourceId?.let { setOf(it) }
                    ?: preferred.toSet().takeIf { it.isNotEmpty() }
                    ?: com.android.purebilibili.data.model.animeko.AniBuiltinSources.defaultEnabledIds,
                kinds = setOf(AniMediaSourceKind.ONLINE),
            )
            // 优先源一个都没命中 -> 回退全量搜索(只在没有指定单源时才回退)
            if (onlySourceId == null && preferred.isNotEmpty() && result.candidates.isEmpty()) {
                result = AniMediaSourceRepository.search(
                    query,
                    kinds = setOf(AniMediaSourceKind.ONLINE),
                )
            }
            // 结果按「优先源顺序」排序(没勾优先源时保持原有的速度排序)
            val orderedResult = if (preferred.isNotEmpty()) {
                val rank: (String) -> Int = { id ->
                    preferred.indexOf(id).let { if (it < 0) Int.MAX_VALUE else it }
                }
                AniMediaSearchResult(
                    candidates = result.candidates.sortedBy { c -> rank(c.sourceId) },
                    sourceStatus = result.sourceStatus,
                )
            } else {
                result
            }
            // 已下载到本机的集子: 插一条本地候选到最前, 点它即离线播放
            val cached = if (query.episodeId > 0L) {
                AnimekoLocalRepository.cacheForEpisode(appContext, query.episodeId)
            } else {
                null
            }
            // 缓存文件现在落在公共下载目录, 路径可能是 content:// URI(MediaStore),
            // 也可能是 file:// ; 两种都直接交给播放器
            val localPlayablePath = cached?.filePath?.takeIf { path ->
                path.startsWith("content://") ||
                    path.startsWith("file://") ||
                    java.io.File(path).isFile
            }
            val withLocal = if (cached != null && localPlayablePath != null) {
                val local = com.android.purebilibili.data.model.animeko.AniMediaCandidate(
                    sourceId = "local_cache",
                    sourceName = "本地缓存",
                    kind = AniMediaSourceKind.LOCAL.name,
                    url = if (localPlayablePath.startsWith("content://")) {
                        localPlayablePath
                    } else {
                        "file://" + localPlayablePath
                    },
                    title = cached.subjectName.ifBlank { "已缓存本集" },
                    quality = "离线",
                    subtitleGroup = "本机",
                    sizeBytes = cached.fileSizeBytes,
                )
                AniMediaSearchResult(
                    candidates = listOf(local) + orderedResult.candidates,
                    sourceStatus = orderedResult.sourceStatus,
                )
            } else {
                orderedResult
            }
            _sourceState.value = AniSourceState.Success(withLocal)
        }
    }

    /** 「下载」面板: 检索 BT/磁力源(蜜柑 RSS 等)。 */
    fun loadDownloadSources(query: AniMediaQuery) {
        downloadSourceJob?.cancel()
        downloadSourceJob = viewModelScope.launch {
            _downloadSourceState.value = AniSourceState.Loading
            val result = AniMediaSourceRepository.search(
                query,
                kinds = setOf(AniMediaSourceKind.BT, AniMediaSourceKind.CUSTOM),
            )
            _downloadSourceState.value = AniSourceState.Success(result)
        }
    }

    fun resetMediaSources() {
        sourceJob?.cancel()
        _sourceState.value = AniSourceState.Idle
    }

    // -----------------------------------------------------------------
    // 弹幕
    // -----------------------------------------------------------------

    private val _danmaku = MutableStateFlow<AniDanmaku?>(null)
    val danmaku: StateFlow<AniDanmaku?> = _danmaku.asStateFlow()

    /** 本地弹幕(用户发出的), 单独一份流便于 UI 提示「已发出」。 */
    private val _localDanmaku = MutableStateFlow<List<AniDanmakuItem>>(emptyList())
    val localDanmaku: StateFlow<List<AniDanmakuItem>> = _localDanmaku.asStateFlow()

    private var danmakuJob: kotlinx.coroutines.Job? = null

    /** 拉取某话弹幕 (animeko 弹幕源) + 合并本机历史弹幕, 免登录。 */
    fun loadDanmaku(episodeId: Long) {
        danmakuJob?.cancel()
        _danmaku.value = null
        _localDanmaku.value = emptyList()
        if (episodeId <= 0L) return
        danmakuJob = viewModelScope.launch {
            when (val r = AnimekoRepository.danmaku(episodeId)) {
                is AniResult.Ok -> _danmaku.value = r.value
                is AniResult.Err -> Unit
            }
            // 本地弹幕并进来(同一话下次重播还能看到自己发过的)
            val local = AnimekoLocalRepository.localDanmakuForEpisode(appContext, episodeId)
            _localDanmaku.value = local.map { it.toDanmakuItem() }
        }
    }

    /**
     * 发送弹幕(本地版)。
     *
     * ★ animeko 的发送接口需要登录(实测 POST /v1/danmaku/{episodeId} 无 token 返回 401),
     *   而本模块不登录任何非 B 站账号, 所以弹幕只落本机:
     *   1. 立刻回填到 [localDanmaku], 播放器把它并进弹幕层即时上屏;
     *   2. 持久化到 Room, 下次播同一话还能看到。
     *
     * @return 是否成功入库
     */
    fun sendDanmaku(
        episodeId: Long,
        text: String,
        positionMillis: Long,
        location: String = "NORMAL",
        color: Int = -1,
        onResult: (Boolean) -> Unit = {},
    ) {
        val trimmed = text.trim()
        if (episodeId <= 0L || trimmed.isEmpty()) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val entity = AniLocalDanmaku(
                episodeId = episodeId,
                playTimeMillis = positionMillis.coerceAtLeast(0L),
                text = trimmed.take(100),
                color = color,
                location = location,
            )
            AnimekoLocalRepository.addLocalDanmaku(appContext, entity)
            _localDanmaku.value = _localDanmaku.value + entity.toDanmakuItem()
            onResult(true)
        }
    }

    private fun AniLocalDanmaku.toDanmakuItem() = AniDanmakuItem(
        id = "local-$id",
        senderId = "local",
        danmakuInfo = AniDanmakuInfo(
            playTime = playTimeMillis,
            color = color,
            text = text,
            location = location,
        ),
    )

    // -----------------------------------------------------------------
    // 历史 / 缓存 (本地写入)
    // -----------------------------------------------------------------

    /** 播放时记录本地历史（受「记录追番历史」「历史条数上限」控制）。 */
    fun recordPlayback(
        detail: AniSubjectDetail,
        episode: AniEpisode,
        positionMillis: Long,
        durationMillis: Long,
    ) {
        val settings = AniSettingsStore.snapshot(appContext)
        if (!settings.recordWatchHistory) return
        viewModelScope.launch {
            // ★ NonCancellable: 退出播放页瞬间 viewModelScope 会被取消,
            //   不加这个, 兜底写入协程还没落库就没了(历史页空白的根因之一)
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                AnimekoLocalRepository.recordPlayback(
                    context = appContext,
                    subject = detail,
                    episode = episode,
                    positionMillis = positionMillis,
                    durationMillis = durationMillis,
                )
                // 超出上限时裁掉最早的记录
                AnimekoLocalRepository.trimHistory(appContext, settings.historyMaxCount)
            }
        }
    }

    fun deleteHistoryEpisode(episodeId: Long) {
        viewModelScope.launch {
            AnimekoLocalRepository.deleteHistoryEpisode(appContext, episodeId)
        }
    }

    fun clearHistory() {
        viewModelScope.launch { AnimekoLocalRepository.clearHistory(appContext) }
    }

    fun removeFavorite(subjectId: Long) {
        viewModelScope.launch {
            AnimekoLocalRepository.removeFavorite(appContext, subjectId)
            if (_isFavorite.value) _isFavorite.value = false
        }
    }
}
