package com.android.purebilibili.feature.plugin

import com.android.purebilibili.core.plugin.FeedKind
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 「屏蔽净化」插件（hoyofairy_block）核心规则测试。
 *
 * 全部针对纯函数，不依赖 Android 运行时：
 * - [normalizeForMatch] / [parseHoyoRule] / [parseScopedRule] 规则解析
 * - [buildHoyoFairyMatcher] + [shouldShowHoyoFairyItem] 过滤判定
 * - [mergeRuleLines] / [parseHoyoImport] / [buildHoyoExportJson] 名单维护
 * - 配置 JSON 往返（等价于「重启后从本地恢复屏蔽词」的语义）
 */
class HoyoFairyBlockPluginTest {

    private fun video(
        title: String = "普通视频",
        bvid: String = "BV1xx411c7XX",
        mid: Long = 1001L,
        upName: String = "UP$mid",
        duration: Int = 300,
        view: Int = 500_000,
        like: Int = 25_000,
        tname: String = "生活",
        isFollowed: Boolean = false
    ) = VideoItem(
        bvid = bvid,
        title = title,
        owner = Owner(mid = mid, name = upName),
        stat = Stat(view = view, like = like),
        duration = duration,
        tname = tname,
        isFollowed = isFollowed
    )

    /** true = 显示，false = 被屏蔽。 */
    private fun shows(
        config: HoyoFairyBlockConfig,
        item: VideoItem,
        kind: FeedKind = FeedKind.HOME_RECOMMEND,
        tags: Set<String> = emptySet()
    ): Boolean = shouldShowHoyoFairyItem(config, buildHoyoFairyMatcher(config), item, kind, tags)

    // ==================== 关键词 ====================

    @Test
    fun keywordPlainMatchesTitle() {
        assertFalse(shows(HoyoFairyBlockConfig(keywords = "鸣潮"), video(title = "鸣潮新版本解析")))
        assertTrue(shows(HoyoFairyBlockConfig(keywords = "鸣潮"), video(title = "原神新版本解析")))
    }

    @Test
    fun keywordPlainMatchesUpNameAndPartition() {
        // 无作用域前缀 → 命中 标题 / UP 名 / 分区 任一即屏蔽
        assertFalse(shows(HoyoFairyBlockConfig(keywords = "营销号"), video(upName = "营销号老王")))
        assertFalse(shows(HoyoFairyBlockConfig(keywords = "资讯"), video(tname = "资讯")))
    }

    @Test
    fun keywordRegexIsSupported() {
        assertFalse(shows(HoyoFairyBlockConfig(keywords = "/震惊|竟然/"), video(title = "竟然还能这样")))
        assertTrue(shows(HoyoFairyBlockConfig(keywords = "/震惊|竟然/"), video(title = "正常标题")))
    }

    @Test
    fun scopeTitleOnlyDoesNotMatchUpName() {
        val config = HoyoFairyBlockConfig(keywords = "title:原神")
        assertFalse(shows(config, video(title = "原神攻略")))
        // UP 名含"原神"但标题不含 → 不该被屏蔽
        assertTrue(shows(config, video(title = "普通标题", upName = "原神区UP")))
    }

    @Test
    fun scopeUpOnlyDoesNotMatchTitle() {
        val config = HoyoFairyBlockConfig(keywords = "up:某工作室")
        assertFalse(shows(config, video(title = "普通标题", upName = "某工作室")))
        assertTrue(shows(config, video(title = "某工作室出品", upName = "别的UP")))
    }

    @Test
    fun scopePartOnlyMatchesPartition() {
        val config = HoyoFairyBlockConfig(keywords = "part:资讯")
        assertFalse(shows(config, video(title = "普通标题", tname = "资讯")))
        assertTrue(shows(config, video(title = "资讯", tname = "生活")))
    }

    @Test
    fun fullWidthTitleIsMatchedByHalfWidthKeyword() {
        // 全角 "鸣潮" 应能被半角关键词命中（全角→半角归一）
        assertFalse(shows(HoyoFairyBlockConfig(keywords = "鸣潮"), video(title = "鸣潮 启动")))
        assertFalse(shows(HoyoFairyBlockConfig(keywords = "abc"), video(title = "ＡＢＣ 测试")))
    }

    @Test
    fun invalidRegexIsIgnoredInsteadOfCrashing() {
        assertNull(parseHoyoRule("/([unclosed/"))
        // 非法正则不会让整条配置爆炸，其余规则照常
        assertTrue(shows(HoyoFairyBlockConfig(keywords = "/([unclosed/"), video()))
    }

    // ==================== 精确维度 ====================

    @Test
    fun uidExactBlock() {
        assertFalse(shows(HoyoFairyBlockConfig(uids = "12345"), video(mid = 12345L)))
        assertTrue(shows(HoyoFairyBlockConfig(uids = "12345"), video(mid = 54321L)))
    }

    @Test
    fun upNameExactBlockDoesNotFuzzyMatch() {
        assertFalse(shows(HoyoFairyBlockConfig(upNames = "营销号老王"), video(upName = "营销号老王")))
        // 精确匹配：仅包含不算命中
        assertTrue(shows(HoyoFairyBlockConfig(upNames = "营销号老王"), video(upName = "营销号老王的小号")))
    }

    @Test
    fun bvidExactBlock() {
        assertFalse(shows(HoyoFairyBlockConfig(bvids = "BV1xx411c7XX"), video(bvid = "BV1xx411c7XX")))
        // 大小写不敏感
        assertFalse(shows(HoyoFairyBlockConfig(bvids = "bv1xx411c7xx"), video(bvid = "BV1xx411c7XX")))
        assertTrue(shows(HoyoFairyBlockConfig(bvids = "BV1xx411c7XX"), video(bvid = "BV1yy411c7YY")))
    }

    @Test
    fun partitionBlockSupportsRegex() {
        assertFalse(shows(HoyoFairyBlockConfig(partitions = "/资讯|广告/"), video(tname = "资讯")))
        assertTrue(shows(HoyoFairyBlockConfig(partitions = "/资讯|广告/"), video(tname = "生活")))
    }

    // ==================== 数值阈值 ====================

    @Test
    fun durationThresholds() {
        val min = HoyoFairyBlockConfig(minDurationSec = 60)
        assertFalse(shows(min, video(duration = 30)))
        assertTrue(shows(min, video(duration = 120)))

        val max = HoyoFairyBlockConfig(maxDurationSec = 600)
        assertFalse(shows(max, video(duration = 900)))
        assertTrue(shows(max, video(duration = 300)))
    }

    @Test
    fun viewsThresholdUsesWanUnit() {
        // minViews 单位是「万」
        val config = HoyoFairyBlockConfig(minViews = 10)
        assertFalse(shows(config, video(view = 50_000)))   // 5 万 < 10 万
        assertTrue(shows(config, video(view = 200_000)))   // 20 万 ≥ 10 万
    }

    @Test
    fun likesThresholds() {
        val config = HoyoFairyBlockConfig(maxLikes = 1000)
        assertFalse(shows(config, video(like = 50_000)))
        assertTrue(shows(config, video(like = 500)))
    }

    // ==================== 刷赞 / 营销号 ====================

    @Test
    fun spamDetectionBlocksLowLikeRatio() {
        val config = HoyoFairyBlockConfig(spamLikeRatio = 3, spamMinViews = 10)
        // 100 万播放 / 1 万赞 = 1% < 3% → 判为营销号
        assertFalse(shows(config, video(view = 1_000_000, like = 10_000)))
        // 100 万播放 / 10 万赞 = 10% ≥ 3% → 正常
        assertTrue(shows(config, video(view = 1_000_000, like = 100_000)))
    }

    @Test
    fun spamDetectionRespectsViewFloor() {
        val config = HoyoFairyBlockConfig(spamLikeRatio = 3, spamMinViews = 10)
        // 播放量不到门槛（5 万 < 10 万）时不判定，避免冤枉新视频
        assertTrue(shows(config, video(view = 50_000, like = 100)))
    }

    @Test
    fun spamDetectionOffByDefault() {
        // 默认 spamLikeRatio = 0 → 不启用
        assertTrue(shows(HoyoFairyBlockConfig(), video(view = 1_000_000, like = 0)))
    }

    // ==================== 白名单 / 豁免 / 来源 ====================

    @Test
    fun whitelistUidWinsOverBlockedUid() {
        val config = HoyoFairyBlockConfig(uids = "12345", allowUids = "12345")
        assertTrue(shows(config, video(mid = 12345L)))
    }

    @Test
    fun whitelistKeywordWinsOverBlockedKeyword() {
        val config = HoyoFairyBlockConfig(keywords = "鸣潮", allowKeywords = "鸣潮官方")
        assertTrue(shows(config, video(title = "鸣潮官方公告")))
        assertFalse(shows(config, video(title = "鸣潮民间解析")))
    }

    @Test
    fun followedCreatorIsExemptOnlyOnRecommendFeed() {
        // 必须同时开启「也应用于热门」，否则热门流本身就不参与过滤，测不出豁免差异
        val config = HoyoFairyBlockConfig(
            uids = "12345",
            exemptFollowed = true,
            applyToHot = true
        )
        val followed = video(mid = 12345L, isFollowed = true)
        // 推荐流：已关注 → 豁免放行
        assertTrue(shows(config, followed, FeedKind.HOME_RECOMMEND))
        // 热门流：不走豁免 → 按 UID 屏蔽
        assertFalse(shows(config, followed, FeedKind.HOME_POPULAR))
    }

    @Test
    fun feedKindGatingRespectsApplySwitches() {
        val config = HoyoFairyBlockConfig(keywords = "鸣潮", applyToHot = false)
        // 推荐流始终生效
        assertFalse(shows(config, video(title = "鸣潮"), FeedKind.HOME_RECOMMEND))
        // 未开启「也应用于热门」→ 热门不过滤
        assertTrue(shows(config, video(title = "鸣潮"), FeedKind.HOME_POPULAR))

        val enabled = config.copy(applyToHot = true)
        assertFalse(shows(enabled, video(title = "鸣潮"), FeedKind.HOME_POPULAR))
    }

    @Test
    fun emptyConfigShowsEverything() {
        val config = HoyoFairyBlockConfig()
        assertTrue(shows(config, video()))
    }

    // ==================== 视频标签维度 ====================

    @Test
    fun keywordAlsoMatchesVideoTag() {
        // 标题 / UP 名 / 分区都不含该词，但视频标签命中 → 屏蔽
        val config = HoyoFairyBlockConfig(keywords = "鬼畜")
        assertFalse(shows(config, video(title = "普通标题"), tags = setOf("鬼畜", "搞笑")))
        // 标签尚未补齐（空集）时不误伤，避免首屏抖动
        assertTrue(shows(config, video(title = "普通标题")))
    }

    @Test
    fun tagScopeOnlyMatchesTags() {
        val config = HoyoFairyBlockConfig(keywords = "tag:原神")
        assertFalse(shows(config, video(title = "普通标题"), tags = setOf("原神")))
        // tag: 作用域不匹配标题
        assertTrue(shows(config, video(title = "原神攻略")))
        assertTrue(shows(config, video(title = "普通标题"), tags = setOf("攻略")))
    }

    @Test
    fun tagScopeSupportsRegex() {
        val config = HoyoFairyBlockConfig(keywords = "tag:/米哈一儿|二抄/")
        assertFalse(shows(config, video(), tags = setOf("二抄")))
        assertTrue(shows(config, video(), tags = setOf("单机")))
    }

    @Test
    fun tagScopeIsParsedFromPrefix() {
        assertEquals(KwScope.TAG, parseScopedRule("tag:原神")?.scope)
        assertEquals(KwScope.TAG, parseScopedRule("TAG: 原神")?.scope)
        assertEquals(KwScope.ANY, parseScopedRule("原神")?.scope)
    }

    @Test
    fun tagMatchingIsNormalizedToo() {
        // 标签里的全角字符同样应被半角关键词命中
        val config = HoyoFairyBlockConfig(keywords = "abc")
        assertFalse(shows(config, video(), tags = setOf("ＡＢＣ")))
    }

    // ==================== 名单维护 ====================

    @Test
    fun mergeRuleLinesDeduplicates() {
        val merged = mergeRuleLines("鸣潮\n库洛", listOf("库洛", "  战双  ", "", "鸣潮"))
        assertEquals(listOf("鸣潮", "库洛", "战双"), merged.split("\n"))
    }

    @Test
    fun parseImportFromSubscriptionJson() {
        val json = """
            {
              "app": "biliHoyoFairy",
              "format": 1,
              "rules": {
                "keywords": ["库洛", "/战双|漂泊者/"],
                "uids": ["12345678"],
                "partitions": ["资讯"],
                "tags": ["/某某/"],
                "upBio": ["动漫领域大神"]
              }
            }
        """.trimIndent()
        val result = parseHoyoImport(json)
        assertEquals(2, result.keywords.size)
        assertEquals(listOf("12345678"), result.uids)
        assertEquals(listOf("资讯"), result.partitions)
        // tags / upBio 为联网维度，导入时应被明确忽略并计数
        assertTrue(result.ignoredDimensions.containsAll(listOf("tags", "upBio")))
    }

    @Test
    fun parseImportFromSectionedText() {
        val text = """
            [关键词]
            鸣潮
            库洛
            [UID]
            12345678
            [UP名]
            某工作室
            [分区]
            资讯
            [BV]
            BV1xx411c7XX
        """.trimIndent()
        val result = parseHoyoImport(text)
        assertEquals(listOf("鸣潮", "库洛"), result.keywords)
        assertEquals(listOf("12345678"), result.uids)
        assertEquals(listOf("某工作室"), result.upNames)
        assertEquals(listOf("资讯"), result.partitions)
        assertEquals(listOf("BV1xx411c7XX"), result.bvids)
        assertEquals(6, result.total)
    }

    @Test
    fun exportImportRoundTrip() {
        val config = HoyoFairyBlockConfig(
            keywords = "鸣潮\n库洛",
            partitions = "资讯",
            upNames = "某工作室",
            uids = "12345678",
            bvids = "BV1xx411c7XX"
        )
        val exported = buildHoyoExportJson(config)
        val parsed = parseHoyoImport(exported)
        assertEquals(listOf("鸣潮", "库洛"), parsed.keywords)
        assertEquals(listOf("资讯"), parsed.partitions)
        assertEquals(listOf("某工作室"), parsed.upNames)
        assertEquals(listOf("12345678"), parsed.uids)
        assertEquals(listOf("BV1xx411c7XX"), parsed.bvids)
    }

    // ==================== 持久化（等价于「启动自动加载」） ====================

    @Test
    fun configJsonRoundTripKeepsBlockingBehaviour() {
        // 模拟：设置屏蔽词 → 写入 JSON → 重启 → 从 JSON 恢复 → 过滤行为不变
        val configured = HoyoFairyBlockConfig(
            keywords = "鸣潮",
            uids = "12345",
            spamLikeRatio = 3,
            spamMinViews = 10
        )
        val persisted = Json.encodeToString(HoyoFairyBlockConfig.serializer(), configured)
        val restored = Json.decodeFromString(HoyoFairyBlockConfig.serializer(), persisted)

        assertEquals(configured, restored)
        val item = video(title = "鸣潮", mid = 12345L)
        assertFalse(shows(configured, item))
        assertFalse(shows(restored, item))   // 恢复后依然屏蔽
        // 恢复后的 matcher 也确实编译出了规则
        assertTrue(buildHoyoFairyMatcher(restored).keywordRules.isNotEmpty())
    }

    @Test
    fun builtInAdPresetIsAvailableAndEffective() {
        val adPreset = HOYO_PRESET_LIBRARY.firstOrNull { it.name.contains("广告推广") }
        assertNotNull(adPreset)
        assertTrue(adPreset.keywords.any { it == "恰饭" })

        val config = HoyoFairyBlockConfig(
            keywords = mergeRuleLines("", adPreset.keywords)
        )
        assertFalse(shows(config, video(title = "本期恰饭实录")))
        assertTrue(shows(config, video(title = "普通生活vlog")))
    }
}
