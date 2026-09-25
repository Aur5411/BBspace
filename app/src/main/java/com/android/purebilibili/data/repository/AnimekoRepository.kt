// 文件路径: data/repository/AnimekoRepository.kt
//
// 追番模块数据仓库: 全部走 https://api.animeko.org (ANI 服务端)。
// 不使用 B 站任何接口 / 登录态, 也不使用 ANI 账号。
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.animeko.AnimekoNetwork
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.animeko.AniAiringDay
import com.android.purebilibili.data.model.animeko.AniAnimeSchedule
import com.android.purebilibili.data.model.animeko.AniDanmaku
import com.android.purebilibili.data.model.animeko.AniRecommendPage
import com.android.purebilibili.data.model.animeko.AniRelatedCharacter
import com.android.purebilibili.data.model.animeko.AniReviewPage
import com.android.purebilibili.data.model.animeko.AniSeasonId
import com.android.purebilibili.data.model.animeko.AniScheduleItem
import com.android.purebilibili.data.model.animeko.AniStaffMember
import com.android.purebilibili.data.model.animeko.AniSubjectBrief
import com.android.purebilibili.data.model.animeko.AniSubjectDetail
import com.android.purebilibili.data.model.animeko.AniTrends
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val TAG = "AnimekoRepository"

/** 短评分页大小。服务端单页上限约 100, 取 20 兼顾首屏速度与翻页次数。 */
const val ANI_REVIEW_PAGE_SIZE = 20

/** 番剧首页各页签的单页条数。 */
const val ANI_BROWSE_PAGE_SIZE = 30

/**
 * 服务端 pageSize 上限: 实测 /v2/subjects/search 传 limit=100 有效,
 * 再大没有验证过, 统一夹到 100 避免踩到未定义行为。
 */
const val ANI_SEARCH_MAX_LIMIT = 100

/** 首页推荐单页上限(服务端 total=200, 一次最多给 100)。 */
const val ANI_RECOMMEND_MAX_LIMIT = 100

/** 追番模块统一返回包装, 便于 UI 侧区分「空」与「失败」。 */
sealed interface AniResult<out T> {
    data class Ok<T>(val value: T) : AniResult<T>
    data class Err(val message: String) : AniResult<Nothing>
}

object AnimekoRepository {

    private val api get() = AnimekoNetwork.api

    // ---------------------------------------------------------------
    // 时间表
    // ---------------------------------------------------------------

    /** 季度列表 (服务端按由新到旧返回)。 */
    suspend fun seasons(): AniResult<List<AniSeasonId>> = call("seasons") {
        // 服务端返回的顺序即「最近的季度在前」, 直接透传。
        api.seasons().list
    }

    /** 某季度番表。 */
    suspend fun season(seasonId: String): AniResult<AniAnimeSchedule> = call("season/$seasonId") {
        api.season(seasonId)
    }

    /** 近期放送 (默认取今天所在的一周), 用于「时间表」视图。 */
    suspend fun airing(daysAhead: Int = 6): AniResult<List<AniAiringDay>> = call("airing") {
        val today = todayIsoDate()
        api.airing(today = today, timeZone = currentTimeZoneId()).list
    }

    // ---------------------------------------------------------------
    // 条目
    // ---------------------------------------------------------------

    /**
     * 搜索条目。
     *
     * @param tags 逗号分隔的 Bangumi 中文标签(AND 语义); 非空时即使 [keyword] 为空
     *             也会按标签浏览。注意服务端要求 `q` 参数必须存在, 空串合法。
     */
    suspend fun search(
        keyword: String,
        limit: Int = 30,
        offset: Int = 0,
        tags: String? = null,
    ): AniResult<List<AniSubjectBrief>> = call("search") {
        api.searchSubjects(
            q = keyword,
            limit = limit.coerceIn(1, ANI_SEARCH_MAX_LIMIT),
            offset = offset.coerceAtLeast(0),
            tags = tags?.takeIf { it.isNotBlank() },
        ).items
    }

    /**
     * 按分类标签浏览条目 —— 番剧首页「分类」页签的数据来源。
     *
     * ★ 走的就是搜索接口, 只是关键词传空串、改用 `tags` 过滤。
     *   实测 /v2/subjects/search?q=&limit=30&offset=0&tags=剧情 可正常翻页。
     */
    suspend fun browseByTags(
        tags: List<String>,
        limit: Int = ANI_BROWSE_PAGE_SIZE,
        offset: Int = 0,
    ): AniResult<List<AniSubjectBrief>> =
        search(keyword = "", limit = limit, offset = offset, tags = tags.joinToString(","))

    /** 条目详情。 */
    suspend fun subject(subjectId: Long): AniResult<AniSubjectDetail> = call("subject/$subjectId") {
        api.subject(subjectId)
    }

    /** 条目角色与声优 (详情页可选区块, 失败不阻塞主内容)。 */
    suspend fun characters(subjectId: Long): AniResult<List<AniRelatedCharacter>> =
        call("characters/$subjectId") {
            api.subjectCharacters(subjectId = subjectId, withActors = true)
        }

    /** 条目制作人员 (详情页可选区块, 失败不阻塞主内容)。 */
    suspend fun staff(subjectId: Long): AniResult<List<AniStaffMember>> =
        call("staff/$subjectId") {
            api.subjectStaff(subjectId = subjectId)
        }

    /**
     * 条目短评 (Bangumi + animeko 聚合)。
     *
     * @param offset 服务端扫描偏移, 按页递增 [limit] 即可。
     */
    suspend fun reviews(
        subjectId: Long,
        limit: Int = ANI_REVIEW_PAGE_SIZE,
        offset: Int = 0,
    ): AniResult<AniReviewPage> = call("reviews/$subjectId") {
        api.subjectReviews(subjectId = subjectId, limit = limit, offset = offset)
    }

    /** 短评是否还有下一页。服务端 total 不可靠, 只看「本页是否满页」。 */
    fun hasMoreReviews(page: AniReviewPage, limit: Int = ANI_REVIEW_PAGE_SIZE): Boolean =
        page.items.size >= limit

    // ---------------------------------------------------------------
    // 首页 / 趋势
    // ---------------------------------------------------------------

    /** 首页推荐 (服务端共 200 条)。
     *
     * ★ 早期版本在这里对「单次返回的 20 条」做 `drop(offset).take(limit)`,
     *   于是 offset 一旦超过 20 就永远是空 —— 表现就是「番剧页只有十多个条目,
     *   怎么翻都加载不出更多」。现已改为把 offset/limit 透传给服务端。
     */
    suspend fun recommendations(
        offset: Int = 0,
        limit: Int = ANI_BROWSE_PAGE_SIZE,
    ): AniResult<AniRecommendPage> = call("recommendations") {
        api.recommendations(
            offset = offset.coerceAtLeast(0),
            limit = limit.coerceIn(1, ANI_RECOMMEND_MAX_LIMIT),
        )
    }

    /** 首页推荐是否还有下一页(服务端 total=200, 按已取条数判断)。 */
    fun hasMoreRecommendations(page: AniRecommendPage, fetchedCount: Int): Boolean =
        fetchedCount < page.total && page.items.isNotEmpty()

    // ---------------------------------------------------------------
    // 分类 / 新番 / 热榜 (番剧首页的四个页签)
    // ---------------------------------------------------------------

    /**
     * 当季新番 —— 「新番」页签。
     *
     * 先取季度列表里最新的那一季, 再拉该季番表。
     * 季度 id 形如 2026q4; 服务端按由新到旧返回, 所以直接取第一个。
     */
    suspend fun latestSeasonSubjects(): AniResult<List<AniScheduleItem>> =
        call("latest-season") {
            val seasons = api.seasons().list
            val newest = seasons.firstOrNull() ?: error("没有可用的季度")
            api.season(newest.id).list
        }

    /** 季度列表(供「新番」页签做季度切换)。 */
    suspend fun seasonList(): AniResult<List<AniSeasonId>> = seasons()

    /** 指定季度的番表。 */
    suspend fun seasonSubjects(seasonId: String): AniResult<List<AniScheduleItem>> =
        call("season/$seasonId") {
            api.season(seasonId).list
        }

    /** 趋势热榜(实测稳定返回约 24 条)。 */
    suspend fun trends(): AniResult<AniTrends> = call("trends") {
        api.trends()
    }

    // ---------------------------------------------------------------
    // 弹幕 (animeko 的招牌能力, 保留)
    // ---------------------------------------------------------------

    /** 某剧集弹幕。 */
    suspend fun danmaku(episodeId: Long): AniResult<AniDanmaku> = call("danmaku/$episodeId") {
        api.danmaku(episodeId)
    }

    // ---------------------------------------------------------------
    // 内部工具
    // ---------------------------------------------------------------

    private suspend fun <T> call(
        label: String,
        block: suspend () -> T,
    ): AniResult<T> = withContext(Dispatchers.IO) {
        try {
            AniResult.Ok(block())
        } catch (e: Exception) {
            Logger.w(TAG, "请求失败 [$label]: ${e.message}")
            AniResult.Err(e.message ?: "网络请求失败")
        }
    }

    /** 本地日期 yyyy-MM-dd, 服务端 /v1/schedule/airing 需要 today 参数。 */
    private fun todayIsoDate(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date())
    }

    /** 当前时区 id, 形如 Asia/Shanghai。 */
    private fun currentTimeZoneId(): String =
        TimeZone.getDefault().id

    /** 取今天往前 [offsetDays] 天的 yyyy-MM-dd。 */
    fun isoDateWithOffset(offsetDays: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
}
