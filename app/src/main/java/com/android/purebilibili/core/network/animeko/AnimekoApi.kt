// 文件路径: core/network/animeko/AnimekoApi.kt
//
// BB空间「追番」模块网络层。
//
// ★ 完全独立于 B 站网络栈:
//   - 不使用 NetworkModule.okHttpClient / guestOkHttpClient
//   - 不附加任何 B 站 Cookie / buvid3 / Referer / WBI 签名
//   - 不读取 TokenManager 的登录态
//   它只是一个纯净的 api.animeko.org 客户端。
package com.android.purebilibili.core.network.animeko

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** ANI 服务端根地址。 */
const val ANIMEKO_BASE_URL = "https://api.animeko.org/"

/** 图片 CDN 域名, 详情页头像走 api 域。 */
const val ANIMEKO_STATIC_HOST = "static.myani.org"

/**
 * 追番模块的 Retrofit 接口定义。
 * 全部为免登录只读接口 (实测 200); 需要 ANI 账号的接口一律不接。
 */
interface AnimekoApi {

    // ---- 时间表 ----

    /** 季度列表。 */
    @GET("v1/schedule/seasons")
    suspend fun seasons(): com.android.purebilibili.data.model.animeko.AniSeasonIdList

    /** 某季度的番表。 */
    @GET("v1/schedule/season/{seasonId}")
    suspend fun season(
        @Path("seasonId") seasonId: String,
    ): com.android.purebilibili.data.model.animeko.AniAnimeSchedule

    /** 近期放送 (含今日)。today 形如 2026-09-23, timeZone 形如 Asia/Shanghai。 */
    @GET("v1/schedule/airing")
    suspend fun airing(
        @Query("today") today: String,
        @Query("timeZone") timeZone: String,
    ): com.android.purebilibili.data.model.animeko.AniLatestAiringSchedule

    // ---- 条目 ----

    /**
     * 搜索条目 / 按标签浏览条目。
     *
     * ★ q 允许传空串 —— 此时等价于「按 [tags] 做纯分类浏览」, 实测可用:
     *     GET /v2/subjects/search?q=&limit=30&offset=0&tags=剧情
     * ★ 实测服务端 `query` 参数是必填, 缺省会 400 "Missing 'query'";
     *   但 `q=` 空串是合法的。所以浏览分类时必须显式带上 `q=`。
     *
     * @param q 关键词, 空串表示只看 [tags]
     * @param tags 逗号分隔的 Bangumi 中文标签, 语义为 AND; 空串表示不限
     */
    @GET("v2/subjects/search")
    suspend fun searchSubjects(
        @Query("q") q: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("tags") tags: String? = null,
    ): com.android.purebilibili.data.model.animeko.AniSubjectSearchPage

    /** 条目详情 (含剧集 / infobox / 关联 / TMDB 剧照)。 */
    @GET("v2/subjects/{subjectId}")
    suspend fun subject(
        @Path("subjectId") subjectId: Long,
    ): com.android.purebilibili.data.model.animeko.AniSubjectDetail

    /** 条目的角色与声优。 */
    @GET("v2/subjects/{subjectId}/characters")
    suspend fun subjectCharacters(
        @Path("subjectId") subjectId: Long,
        @Query("withActors") withActors: Boolean = true,
    ): List<com.android.purebilibili.data.model.animeko.AniRelatedCharacter>

    /**
     * 条目的制作人员 (Bangumi staff)。
     *
     * 实测 `{id}` 的 staff 可能有上百条(含原画/补间这类人数众多的岗位),
     * position 为 Bangumi 内部编码, 展示前需经 aniStaffPositionLabel 转换。
     */
    @GET("v2/subjects/{subjectId}/staff")
    suspend fun subjectStaff(
        @Path("subjectId") subjectId: Long,
    ): List<com.android.purebilibili.data.model.animeko.AniStaffMember>

    /**
     * 条目的短评 / 吐槽。
     *
     * ★ 服务端聚合了 Bangumi 站内短评与 animeko 站内评论, 免登录即可读。
     * ★ 注意 [offset] 是服务端扫描偏移, total 是「累计已扫条数」而非精确总数;
     *   判断还有没有下一页请用「本页返回条数 == limit」。
     */
    @GET("v2/subjects/{subjectId}/reviews")
    suspend fun subjectReviews(
        @Path("subjectId") subjectId: Long,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0,
    ): com.android.purebilibili.data.model.animeko.AniReviewPage

    // ---- 首页 ----

    /**
     * 首页推荐 (服务端 total=200)。
     *
     * ★ 实测这条接口**支持 offset / limit**:
     *     /v2/home/recommendations?offset=20&limit=20  -> 拿到第 21~40 条
     *     /v2/home/recommendations?limit=100           -> 一次给 100 条
     *   早期版本漏传了这两个参数, 只在本地对单页 20 条做 drop/take,
     *   结果「番剧页永远只有十多个、再也加载不出更多」。
     */
    @GET("v2/home/recommendations")
    suspend fun recommendations(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20,
    ): com.android.purebilibili.data.model.animeko.AniRecommendPage

    /** 趋势榜。 */
    @GET("v1/trends")
    suspend fun trends(): com.android.purebilibili.data.model.animeko.AniTrends

    // ---- 弹幕 ----

    /** 某剧集的弹幕。 */
    @GET("v1/danmaku/{episodeId}")
    suspend fun danmaku(
        @Path("episodeId") episodeId: Long,
    ): com.android.purebilibili.data.model.animeko.AniDanmaku
}

/**
 * 追番模块网络单例。
 *
 * 刻意不复用 [com.android.purebilibili.core.network.NetworkModule] 的客户端:
 * 追番数据不经过 B 站, 也不应携带任何 B 站身份信息。
 */
object AnimekoNetwork {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    /** 纯净客户端: 无 CookieJar(默认 NO_COOKIES), 无 B 站拦截器。 */
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    val api: AnimekoApi by lazy {
        Retrofit.Builder()
            .baseUrl(ANIMEKO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AnimekoApi::class.java)
    }
}
