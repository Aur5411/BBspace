// 文件路径: feature/plugin/HoyoFairyBlockPlugin.kt
package com.android.purebilibili.feature.plugin

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.plugin.FeedKind
import com.android.purebilibili.core.plugin.FeedPlugin
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppSwitchPreference
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

/**
 * 推荐流屏蔽净化插件 —— 移植自 biliHoyoFairy（抗击黑潮）。
 *
 * 出处：biliHoyoFairy，作者 gendu-amd，MIT License
 *   GitHub:     https://github.com/gendu-amd/biliHoyoFairy
 *   GreasyFork: https://greasyfork.org/scripts/582873
 *
 * 规则维度与 biliHoyoFairy 的 block 名单一致:
 * 关键词(支持 `title:` / `up:` / `part:` / `tag:` 作用域与 `/正则/`)、分区(tname)、UP 名、
 * UID、BV 号、时长/播放量/点赞数阈值、刷赞(营销号)检测; 白名单优先级最高。
 *
 * 另提供: 预置规则库一键添加、biliHoyoFairy 订阅名单导入/导出。
 *
 * 视频标签(tags)维度: 信息流接口不返回标签, 按 bvid 惰性补齐(见 [prefetchTags]),
 * 缓存命中后即可参与匹配。关键词默认同时匹配 标题 / UP 名 / 分区 / 视频标签,
 * 也可用 `tag:` 前缀只匹配标签。`upBio` 维度仍不支持(需逐个拉详情, 成本过高)。
 */
class HoyoFairyBlockPlugin : FeedPlugin {

    override val id = "hoyofairy_block"
    override val name = "屏蔽净化"
    override val description = "移植自 biliHoyoFairy: 关键词/标签/UP/UID/BV/分区/时长/播放点赞/刷赞多维屏蔽"
    override val version = "1.2.0"
    override val author = "gendu-amd (port)"
    override val icon: ImageVector = Icons.Outlined.Block

    // [修复] filterFeedItems 在 Dispatchers.Default 后台线程读取，而 onEnable 在主线程写。
    // 不加 @Volatile 时，首次加载可能读到陈旧的空 matcher → 启动时不过滤、刷新后才生效。
    @Volatile
    private var config = HoyoFairyBlockConfig()
    @Volatile
    private var matcher: HoyoFairyMatcher? = null

    /** 配置变更信号：供 HomeViewModel 监听后在主页立即重新过滤。 */
    private val _configState = MutableStateFlow(HoyoFairyBlockConfig())
    val configState: StateFlow<HoyoFairyBlockConfig> = _configState.asStateFlow()

    /** bvid -> 视频标签集合（懒加载，供 [shouldShowItem] 同步读取）。 */
    private val tagCache = ConcurrentHashMap<String, Set<String>>()

    override suspend fun onEnable() {
        loadConfig(PluginManager.getContext())
        matcher = buildHoyoFairyMatcher(config)
        _configState.value = config
        Log.d(TAG, "屏蔽净化已启用")
    }

    override suspend fun onDisable() {
        Log.d(TAG, "屏蔽净化已禁用")
    }

    /**
     * 把一个词加入「屏蔽词列表」（关键词维度）。
     * 供详情页「长按标签 → 添加到屏蔽词列表」等入口调用。
     *
     * @return true = 新增成功；false = 空词或已存在
     */
    suspend fun appendBlockedKeyword(context: android.content.Context, keyword: String): Boolean {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return false
        val current = currentConfig(context)
        val merged = mergeRuleLines(current.keywords, listOf(trimmed))
        if (merged == current.keywords) return false
        val next = current.copy(keywords = merged)
        rebuild(next)
        PluginStore.setConfigJson(
            context,
            id,
            Json.encodeToString(HoyoFairyBlockConfig.serializer(), next)
        )
        // 插件若处于关闭状态，FeedPlugin 不会参与过滤 —— 加入屏蔽词时自动启用，保证立即生效
        val registered = PluginManager.plugins.firstOrNull { it.plugin.id == id }
        if (registered != null && !registered.enabled) {
            PluginManager.setEnabled(id, true)
        }
        return true
    }

    /** 读取已落盘配置；读不到时回退到内存中的当前配置。 */
    private suspend fun currentConfig(context: android.content.Context): HoyoFairyBlockConfig =
        runCatching {
            PluginStore.getConfigJson(context, id)?.let {
                Json.decodeFromString(HoyoFairyBlockConfig.serializer(), it)
            }
        }.getOrNull() ?: config

    /**
     * 为信息流补齐视频标签（信息流接口不返回标签，只能按 bvid 补拉）。
     *
     * 仅在「开启了按标签精确过滤」且确实配了关键词规则时才联网，其余情况直接返回，
     * 所以没用标签功能的用户不会有任何额外请求。整体限时 + 并发上限，避免拖慢首屏。
     */
    suspend fun prefetchTags(videos: List<VideoItem>) {
        if (!config.preciseTagFilter) return
        if (matcher?.keywordRules.isNullOrEmpty()) return
        val pending = videos.asSequence()
            .map { it.bvid }
            .filter { it.isNotBlank() && !tagCache.containsKey(it) }
            .distinct()
            .take(MAX_TAG_FETCH_PER_PAGE)
            .toList()
        if (pending.isEmpty()) return

        withContext(Dispatchers.IO) {
            withTimeoutOrNull(TAG_FETCH_TIMEOUT_MS) {
                val semaphore = Semaphore(MAX_TAG_CONCURRENCY)
                coroutineScope {
                    pending.map { bvid ->
                        async {
                            semaphore.withPermit { fetchAndCacheTags(bvid) }
                        }
                    }.awaitAll()
                }
            }
        }
    }

    private suspend fun fetchAndCacheTags(bvid: String) {
        val tags = runCatching { NetworkModule.api.getVideoTags(bvid) }
            .getOrNull()
            ?.data
            ?.mapNotNull { it.tag_name.trim().takeIf { name -> name.isNotEmpty() } }
            ?.toSet()
            ?: return
        if (tagCache.size > TAG_CACHE_MAX) tagCache.clear()
        tagCache[bvid] = tags
    }

    override fun shouldShowItem(item: VideoItem): Boolean =
        shouldShowItem(item, FeedKind.HOME_RECOMMEND)

    override fun shouldShowItem(item: VideoItem, feedKind: FeedKind): Boolean =
        shouldShowHoyoFairyItem(
            config = config,
            matcher = matcher ?: buildHoyoFairyMatcher(config),
            item = item,
            feedKind = feedKind,
            videoTags = tagCache[item.bvid].orEmpty()
        )

    internal companion object {
        const val TAG = "HoyoFairyBlockPlugin"

        /** 插件唯一标识（注册与外部调用共用）。 */
        const val PLUGIN_ID = "hoyofairy_block"

        /** 单页最多补拉多少条视频的标签，避免一页几十个请求。 */
        const val MAX_TAG_FETCH_PER_PAGE = 30

        /** 标签补拉并发上限。 */
        const val MAX_TAG_CONCURRENCY = 6

        /** 标签补拉整体超时（毫秒）；超时则本轮不按标签匹配（fail-open）。 */
        const val TAG_FETCH_TIMEOUT_MS = 2_500L

        /** 标签缓存条数上限，超出直接清空重建。 */
        const val TAG_CACHE_MAX = 2_000
    }

    // ==================== 配置持久化 ====================

    private suspend fun loadConfig(context: android.content.Context) {
        try {
            val json = PluginStore.getConfigJson(context, id)
            if (json != null) {
                config = Json.decodeFromString(HoyoFairyBlockConfig.serializer(), json)
            }
        } catch (e: Exception) {
            Log.w(TAG, "加载配置失败, 使用默认值: ${e.message}")
        }
    }

    private fun rebuild(next: HoyoFairyBlockConfig) {
        config = next
        matcher = buildHoyoFairyMatcher(next)
        // 通知监听方（HomeViewModel）在主页立即重新过滤
        _configState.value = next
    }

    // ==================== 设置界面 ====================

    @Composable
    override fun SettingsContent() {
        SettingsBody(Modifier.padding(16.dp))
    }

    @Composable
    override fun SettingsContent(modifier: Modifier) {
        SettingsBody(modifier)
    }

    @Composable
    private fun SettingsBody(modifier: Modifier = Modifier) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val scope = rememberCoroutineScope()
        val clipboard = LocalClipboardManager.current
        var loaded by remember { mutableStateOf(false) }
        var cfg by remember { mutableStateOf(config) }
        var status by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            loadConfig(context)
            cfg = config
            matcher = buildHoyoFairyMatcher(config)
            loaded = true
        }
        if (!loaded) return

        fun persist(next: HoyoFairyBlockConfig) {
            cfg = next
            rebuild(next)
            scope.launch {
                PluginStore.setConfigJson(
                    context,
                    id,
                    Json.encodeToString(HoyoFairyBlockConfig.serializer(), next)
                )
                // 保存/加入屏蔽规则时确保插件启用，否则 FeedPlugin 不参与信息流过滤
                val registered = PluginManager.plugins.firstOrNull { it.plugin.id == id }
                if (registered != null && !registered.enabled) {
                    PluginManager.setEnabled(id, true)
                }
            }
        }

        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (status.isNotEmpty()) {
                AppText(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Section("屏蔽名单（每行一条，支持 /正则/）") {
                MultiLineField(
                    title = "关键词",
                    subtitle = "命中 标题 / UP 名 / 分区 / 视频标签。可用 title:、up:、part:、tag: 限定作用域",
                    value = cfg.keywords,
                    onValue = { persist(cfg.copy(keywords = it)) }
                )
                MultiLineField(
                    title = "分区关键词",
                    subtitle = "按视频分区名(tname)屏蔽",
                    value = cfg.partitions,
                    onValue = { persist(cfg.copy(partitions = it)) }
                )
                MultiLineField(
                    title = "UP 主名（精确匹配）",
                    subtitle = "整名一致才命中",
                    value = cfg.upNames,
                    onValue = { persist(cfg.copy(upNames = it)) }
                )
                MultiLineField(
                    title = "UID（精确匹配，每行一个）",
                    subtitle = "直接屏蔽该 UP 主的所有视频",
                    value = cfg.uids,
                    onValue = { persist(cfg.copy(uids = it)) }
                )
                MultiLineField(
                    title = "BV 号（精确匹配）",
                    subtitle = "屏蔽指定视频",
                    value = cfg.bvids,
                    onValue = { persist(cfg.copy(bvids = it)) }
                )
            }

            Section("数值阈值（0 = 不启用）") {
                NumberField("最短时长（秒）", cfg.minDurationSec) { persist(cfg.copy(minDurationSec = it)) }
                NumberField("最长时长（秒）", cfg.maxDurationSec) { persist(cfg.copy(maxDurationSec = it)) }
                NumberField("最低播放量（万）", cfg.minViews) { persist(cfg.copy(minViews = it)) }
                NumberField("最高播放量（万）", cfg.maxViews) { persist(cfg.copy(maxViews = it)) }
                NumberField("最低点赞数", cfg.minLikes) { persist(cfg.copy(minLikes = it)) }
                NumberField("最高点赞数", cfg.maxLikes) { persist(cfg.copy(maxLikes = it)) }
            }

            Section("刷赞 / 营销号检测") {
                NumberField("点赞率下限（%）", cfg.spamLikeRatio) { persist(cfg.copy(spamLikeRatio = it)) }
                NumberField("判定最低播放量（万）", cfg.spamMinViews) { persist(cfg.copy(spamMinViews = it)) }
                InfoLine("点赞率低于下限且播放量达到门槛的视频视为营销号/搬运号（0 = 关闭）")
            }

            Section("白名单（优先级最高）") {
                MultiLineField(
                    title = "关键词白名单",
                    subtitle = "命中标题/UP名/分区的词直接放行",
                    value = cfg.allowKeywords,
                    onValue = { persist(cfg.copy(allowKeywords = it)) }
                )
                MultiLineField(
                    title = "UP 名白名单（精确）",
                    value = cfg.allowUpNames,
                    onValue = { persist(cfg.copy(allowUpNames = it)) }
                )
                MultiLineField(
                    title = "UID 白名单（每行一个）",
                    value = cfg.allowUids,
                    onValue = { persist(cfg.copy(allowUids = it)) }
                )
            }

            Section("预置规则库（点击加入关键词）") {
                HOYO_PRESET_LIBRARY.forEach { preset ->
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                        TextButton(onClick = {
                            persist(cfg.copy(keywords = mergeRuleLines(cfg.keywords, preset.keywords)))
                            status = "已加入预置：${preset.name}"
                        }) {
                            AppText(
                                text = "+ ${preset.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (preset.desc.isNotEmpty()) {
                            AppText(
                                text = preset.desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            ImportExportSection(
                onImport = { text ->
                    val parsed = parseHoyoImport(text)
                    if (parsed.total == 0) {
                        status = "未解析到任何条目，请检查格式"
                    } else {
                        val merged = cfg.copy(
                            keywords = mergeRuleLines(cfg.keywords, parsed.keywords),
                            partitions = mergeRuleLines(cfg.partitions, parsed.partitions),
                            upNames = mergeRuleLines(cfg.upNames, parsed.upNames),
                            uids = mergeRuleLines(cfg.uids, parsed.uids),
                            bvids = mergeRuleLines(cfg.bvids, parsed.bvids),
                        )
                        persist(merged)
                        status = buildString {
                            append("已导入 ${parsed.total} 条")
                            if (parsed.ignoredDimensions.isNotEmpty()) {
                                append("；忽略不支持维度：")
                                append(parsed.ignoredDimensions.joinToString("/"))
                            }
                        }
                    }
                },
                onExport = {
                    clipboard.setText(AnnotatedString(buildHoyoExportJson(cfg)))
                    status = "已复制订阅 JSON 到剪贴板"
                }
            )

            Section("已关注 / 应用范围") {
                AppSwitchPreference(
                    title = "按视频标签精确过滤",
                    subtitle = "信息流接口不含标签，开启后按 bvid 补拉真实标签参与匹配（仅当配了关键词时才联网）",
                    checked = cfg.preciseTagFilter,
                    onCheckedChange = { persist(cfg.copy(preciseTagFilter = it)) }
                )
                AppSwitchPreference(
                    title = "已关注 UP 主豁免",
                    subtitle = "推荐流中已关注的 UP 主不受屏蔽影响",
                    checked = cfg.exemptFollowed,
                    onCheckedChange = { persist(cfg.copy(exemptFollowed = it)) }
                )
                AppSwitchPreference(
                    title = "也应用于热门",
                    checked = cfg.applyToHot,
                    onCheckedChange = { persist(cfg.copy(applyToHot = it)) }
                )
                AppSwitchPreference(
                    title = "也应用于排行榜",
                    checked = cfg.applyToRank,
                    onCheckedChange = { persist(cfg.copy(applyToRank = it)) }
                )
                AppSwitchPreference(
                    title = "也应用于搜索结果",
                    checked = cfg.applyToSearch,
                    onCheckedChange = { persist(cfg.copy(applyToSearch = it)) }
                )
            }
        }
    }

    @Composable
    private fun ImportExportSection(
        onImport: (String) -> Unit,
        onExport: () -> Unit,
    ) {
        var draft by remember { mutableStateOf("") }
        Section("名单导入 / 导出") {
            Column(modifier = Modifier.padding(16.dp)) {
                AppText(
                    text = "粘贴 biliHoyoFairy 订阅 JSON，或带 [关键词]/[UID]/[UP名]/[分区]/[BV] 小节头的文本",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AppOutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    minLines = 3,
                    maxLines = 8,
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    TextButton(onClick = {
                        onImport(draft)
                        draft = ""
                    }) {
                        AppText("导入并合并", color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = { draft = "" }) {
                        AppText("清空输入", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = onExport) {
                    AppText("导出当前名单（订阅 JSON）", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    @Composable
    private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                modifier = Modifier.fillMaxWidth().clip(AppShapes.container(ContainerLevel.Card)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
            ) {
                Column(content = content)
            }
        }
    }

    @Composable
    private fun InfoLine(text: String) {
        AppText(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }

    @Composable
    private fun MultiLineField(
        title: String,
        subtitle: String = "",
        value: String,
        onValue: (String) -> Unit,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AppText(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                AppText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AppOutlinedTextField(
                value = value,
                onValueChange = onValue,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                minLines = 2,
                maxLines = 6,
            )
        }
    }

    @Composable
    private fun NumberField(
        title: String,
        value: Long,
        onValue: (Long) -> Unit,
    ) {
        var draft by remember(value) { mutableStateOf(value.toString()) }
        Column(modifier = Modifier.padding(16.dp)) {
            AppText(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            AppOutlinedTextField(
                value = draft,
                onValueChange = { text ->
                    val digits = text.filter { it.isDigit() }
                    draft = digits
                    onValue(digits.toLongOrNull() ?: 0L)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                singleLine = true,
            )
        }
    }
}

/**
 * 供详情页等 UI 调用：把 [keyword] 加入「屏蔽净化」插件的屏蔽词列表（关键词维度）。
 *
 * 加入后，标题 / UP 名 / 分区命中该词的视频会在信息流中被隐藏。
 * @return true = 新增成功；false = 插件未注册、空词或已存在
 */
suspend fun appendHoyoFairyBlockedKeyword(
    context: android.content.Context,
    keyword: String,
): Boolean {
    val plugin = PluginManager.plugins
        .firstOrNull { it.plugin.id == HoyoFairyBlockPlugin.PLUGIN_ID }
        ?.plugin as? HoyoFairyBlockPlugin
    return plugin?.appendBlockedKeyword(context, keyword) ?: false
}

/**
 * 供信息流加载前调用：为这批视频补齐标签缓存，让「关键词 / tag:」规则能按真实标签命中。
 * 未开启按标签精确过滤、或没配关键词规则时是空操作（不产生任何网络请求）。
 */
suspend fun prefetchHoyoFairyTags(videos: List<VideoItem>) {
    val plugin = PluginManager.plugins
        .firstOrNull { it.plugin.id == HoyoFairyBlockPlugin.PLUGIN_ID }
        ?.plugin as? HoyoFairyBlockPlugin
    plugin?.prefetchTags(videos)
}

/** 插件配置(默认值与 biliHoyoFairy 的 BlockConfig 对齐) */
@Serializable
data class HoyoFairyBlockConfig(
    val keywords: String = "",
    val partitions: String = "",
    val upNames: String = "",
    val uids: String = "",
    val bvids: String = "",
    val minDurationSec: Long = 0L,
    val maxDurationSec: Long = 0L,
    val minViews: Long = 0L,     // 单位: 万
    val maxViews: Long = 0L,     // 单位: 万
    val minLikes: Long = 0L,
    val maxLikes: Long = 0L,
    val spamLikeRatio: Long = 0L, // %；>0 时启用刷赞(营销号)检测
    val spamMinViews: Long = 10L, // 万；刷赞检测的最低播放门槛
    val allowKeywords: String = "",
    val allowUpNames: String = "",
    val allowUids: String = "",
    /** 按视频标签精确过滤：开启后会按 bvid 补拉标签（信息流接口不含标签）。 */
    val preciseTagFilter: Boolean = true,
    val exemptFollowed: Boolean = true,
    val applyToHot: Boolean = false,
    val applyToRank: Boolean = false,
    val applyToSearch: Boolean = false
)

/** 预置规则库(移植自 biliHoyoFairy 的 PRESET_LIBRARY, 仅取本地维度)。 */
internal data class HoyoPreset(
    val cat: String,
    val name: String,
    val desc: String,
    val keywords: List<String>
)

internal val HOYO_PRESET_LIBRARY: List<HoyoPreset> = listOf(
    HoyoPreset(
        cat = "广告 / 营销",
        name = "广告推广（强化）",
        desc = "商业合作 / 恰饭 / 电商带货 / 游戏推广等内置词（原「去广告增强」的内置词表）",
        keywords = listOf(
            // 商业合作类
            "商业合作", "恰饭", "推广", "广告", "赞助", "植入",
            "合作推广", "品牌合作", "本期合作", "本视频由",
            // 平台推广类
            "官方活动", "官方推荐", "平台活动", "创作激励",
            // 电商带货类
            "淘宝", "天猫", "京东", "拼多多", "双十一", "双11",
            "优惠券", "领券", "限时优惠", "好物推荐", "种草",
            // 游戏推广类
            "新游推荐", "游戏推广", "首发", "公测", "不删档"
        )
    ),
    HoyoPreset(
        cat = "无意义弹幕",
        name = "B站弹幕·无意义弹幕",
        desc = "超长弹幕 / 无意义符号 / 单个非中文字符（来源 xmcp/bilibili-ban-list）",
        keywords = listOf(
            "/.{50}/",
            "/^[\\.。,，\\/\\?？!！~～@\\^、+=\\-_ ]+$/",
            "/^[^\\u4E00-\\u9FA5]$/"
        )
    ),
    HoyoPreset(
        cat = "建筑美",
        name = "B站弹幕·建筑美",
        desc = "弹幕墙 / 倍速 / 立体弹幕 / 颜色（来源 xmcp/bilibili-ban-list）",
        keywords = listOf(
            "/弹\\s*[屏幕][^护]*$/",
            "倍速",
            "/[3３三立]\\s+[dDＤ体體]/",
            "/(^|是)..[红橙黄绿青蓝紫黑白灰].{0,2}$/"
        )
    ),
    HoyoPreset(
        cat = "播放相关",
        name = "B站弹幕·播放相关",
        desc = "第N / N刷 / 助攻 / 完结撒花等（来源 xmcp/bilibili-ban-list）",
        keywords = listOf(
            "/^[第前]([0-9零一二两三四五六七八九十百千万亿].{0,6})?$/",
            "/^.{1,6}[天时点分钟秒]前/",
            "/^[0-9零一二两三四五六七八九十百千万亿nN]{1,4}(刷|周[木目])$/",
            "/破.*[wW万萬]/",
            "/[wW万萬]助攻/",
            "/钟的?诅咒/",
            "/口[气氣].*[集级]/",
            "完结",
            "撒花",
            "从左到右",
            "/[0-9万名].{1,6}[伴你们人友]($|.*[豪嚎号好])/"
        )
    ),
    HoyoPreset(
        cat = "空降",
        name = "B站弹幕·空降",
        desc = "空降成功 / 完美空降 / 空难（来源 xmcp/bilibili-ban-list）",
        keywords = listOf(
            "/降.{0,3}(成功|失败|完美)/",
            "完美空降",
            "/空难(现场|$)/"
        )
    ),
    HoyoPreset(
        cat = "漂移",
        name = "B站弹幕·漂移",
        desc = "漂移 / 左漂右漂（来源 xmcp/bilibili-ban-list）",
        keywords = listOf(
            "/[漂飘飄飙][移逸遗]/",
            "/[左右][漂飘飄飙]/",
            "/(\\s|^|[我怎来会].?)[漂飘飄飙]+($|.?[～~?？。，啊]|\\s)/",
            "/\\s{5}/"
        )
    ),
    HoyoPreset(
        cat = "跟风刷屏",
        name = "B站弹幕·跟风刷屏",
        desc = "入宅 / 书名刷屏 / 周目 / 干杯等（来源 xmcp/bilibili-ban-list）",
        keywords = listOf(
            "/入(宅|坑$)/",
            "/^什么[!\\?！？，,、~\\s]/",
            "/^《.*》$/",
            "/^告诉.*[我还永]/",
            "/暴[露漏]((补番)?时间)?[了乐啦拉]/",
            "/周目($|.*(我|回|起|报|人|过|来|此))/",
            "/つロ|[干肝乾]杯/",
            "/[马馬][冬东]|[冬东]梅|[马馬]什么梅/"
        )
    ),
    HoyoPreset(
        cat = "无意义撕逼",
        name = "B站弹幕·无意义撕逼",
        desc = "FFF / 豆腐 / 老婆等（来源 xmcp/bilibili-ban-list）",
        keywords = listOf(
            "/[fF]{3}.*(真爱|百合|基|性恋)/",
            "/豆腐.*[酸甜苦辣咸臭糖盐醋吃加]/",
            "/[酸甜苦辣咸臭糖盐醋吃加].*豆腐/",
            "/我的?(床上|老婆)/",
            "我选"
        )
    ),
    HoyoPreset(
        cat = "日期相关",
        name = "B站弹幕·日期相关",
        desc = "日期路过 / 节日快乐（来源 xmcp/bilibili-ban-list）",
        keywords = listOf(
            "/(\\d{1,2}|十|十?[一二三四五六七八九十])([\\.,\\/:：、，；点\\-月。]|\\s+)(\\d{1,2}|[二三]?十|[二三]?十?[一二三四五六七八九]).{0,4}($|[日的过我别你上午夜晨])/",
            "/\\d\\d(\\d\\d)?([\\.,。，、\\/\\-年—*]|\\s+)\\d(\\d)?([\\.,。，、\\/\\-月—*]|\\s+)\\d/",
            "/^(2([^3]\\d|3[^3])|6([^6]\\d|6[^6])|[^26]\\d{2})\\d{0,5}$/",
            "/(元旦|年|国庆|春|秋|圣诞).?快乐/"
        )
    ),
    HoyoPreset(
        cat = "引战",
        name = "引战话术",
        desc = "挑动对立的话术片段（已收敛正则、防误伤）",
        keywords = listOf("/接触wuwa后|大脑发生的异变/")
    ),
    HoyoPreset(
        cat = "标题党 / 营销",
        name = "标题党",
        desc = "震惊体 + 一口气看完",
        keywords = listOf(
            "/(一口气|一次性|一天|分钟|分半|小时)(看完|带你看完|直接看完)/",
            "/震惊|竟然|万万没想到/"
        )
    ),
    HoyoPreset(
        cat = "标题党 / 营销",
        name = "营销号UP名",
        desc = "常见营销号账号名",
        keywords = listOf("今日话题", "话题酱", "今日知乎", "大型纪录片")
    ),
    HoyoPreset(
        cat = "标题党 / 营销",
        name = "软传销",
        desc = "日入月入 / 为自己打工",
        keywords = listOf("/(日入|日赚|月入|月赚)\\d+/", "/(小时|内耗).+为自己打工/")
    ),
    HoyoPreset(
        cat = "其它",
        name = "MBTI",
        desc = "MBTI 相关标题",
        keywords = listOf("/MBTI|[IE][SN][TF][JP]|I人|E人/")
    ),
    HoyoPreset(
        cat = "其它",
        name = "梗视频",
        desc = "科目三 / 猫meme / 梗百科",
        keywords = listOf("科目三", "猫meme", "/是什么梗|梗百科|大型[纪记]录片/")
    ),
    HoyoPreset(
        cat = "其它",
        name = "含日语标题",
        desc = "标题含日文假名",
        keywords = listOf("/[ぁ-ヶ]/")
    )
)

// ==================== 匹配引擎(纯逻辑, 便于单元测试) ====================

/**
 * 关键词作用域, 对应 biliHoyoFairy 的 `title:` / `up:` / `part:` 前缀。
 * [TAG] 为本端扩展: 只匹配视频真实标签(需先经 [prefetchHoyoFairyTags] 补齐)。
 */
internal enum class KwScope { ANY, TITLE, UP, PART, TAG }

/** 单条编译后的规则。 */
internal sealed interface HoyoSingleRule {
    data class Substring(val needle: String) : HoyoSingleRule
    data class Pattern(val regex: Regex) : HoyoSingleRule
}

internal data class HoyoScopedRule(val scope: KwScope, val rule: HoyoSingleRule)

internal class HoyoFairyMatcher(
    val keywordRules: List<HoyoScopedRule>,
    val allowKeywordRules: List<HoyoScopedRule>,
    val partitionRules: List<HoyoSingleRule>,
    val upNames: Set<String>,
    val uids: Set<Long>,
    val bvids: Set<String>,
    val allowUpNames: Set<String>,
    val allowUids: Set<Long>,
)

/** 全角→半角 + 去首尾空白 + 小写, 与 biliHoyoFairy 的 normMatch 保持同一套归一。 */
internal fun normalizeForMatch(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
        val code = ch.code
        val mapped = when {
            code == 0x3000 -> ' '
            code in 0xFF01..0xFF5E -> (code - 0xFEE0).toChar()
            else -> ch
        }
        sb.append(mapped)
    }
    return sb.toString().trim().lowercase()
}

private val SCOPE_PREFIX_RE = Regex("^(title|up|part|tag)\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)

/** 解析单条规则: `/正则/` 或普通包含词。返回 null 表示空行/非法正则。 */
internal fun parseHoyoRule(raw: String): HoyoSingleRule? {
    val line = raw.trim()
    if (line.isEmpty()) return null
    if (line.startsWith("/") && line.length >= 2) {
        val lastSlash = line.lastIndexOf('/')
        if (lastSlash > 0) {
            val body = line.substring(1, lastSlash)
            val flags = line.substring(lastSlash + 1)
            if (body.isEmpty()) return null
            val options = buildSet {
                if (flags.contains('i')) add(RegexOption.IGNORE_CASE)
                if (flags.contains('s')) add(RegexOption.DOT_MATCHES_ALL)
            }
            return try {
                HoyoSingleRule.Pattern(
                    if (options.isEmpty()) Regex(body) else Regex(body, options)
                )
            } catch (e: Exception) {
                Log.w("HoyoFairyBlockPlugin", "非法正则已跳过: $line")
                null
            }
        }
    }
    return HoyoSingleRule.Substring(normalizeForMatch(line))
}

/** 解析关键词行, 拆出 `title:` / `up:` / `part:` / `tag:` 作用域前缀。 */
internal fun parseScopedRule(raw: String): HoyoScopedRule? {
    val line = raw.trim()
    if (line.isEmpty()) return null
    // biliHoyoFairy: 以 '/' 开头时不做作用域解析, 整行当正则
    if (!line.startsWith("/")) {
        SCOPE_PREFIX_RE.find(line)?.let { m ->
            val scope = when (m.groupValues[1].lowercase()) {
                "title" -> KwScope.TITLE
                "up" -> KwScope.UP
                "part" -> KwScope.PART
                "tag" -> KwScope.TAG
                else -> KwScope.ANY
            }
            val body = m.groupValues[2].trim()
            val rule = parseHoyoRule(body) ?: return null
            return HoyoScopedRule(scope, rule)
        }
    }
    val rule = parseHoyoRule(line) ?: return null
    return HoyoScopedRule(KwScope.ANY, rule)
}

private fun splitLines(text: String): List<String> =
    text.split('\n', ',').map { it.trim() }.filter { it.isNotEmpty() }

private fun HoyoSingleRule.hit(normalizedText: String, originalText: String): Boolean =
    when (this) {
        is HoyoSingleRule.Substring -> needle.isNotEmpty() && normalizedText.contains(needle)
        is HoyoSingleRule.Pattern -> regex.containsMatchIn(originalText) ||
            regex.containsMatchIn(normalizedText)
    }

/** 命中任一视频标签即算命中（标签需已补拉，未补拉时返回 false）。 */
private fun HoyoSingleRule.hitAnyTag(videoTags: Set<String>): Boolean =
    videoTags.isNotEmpty() && videoTags.any { tag -> hit(normalizeForMatch(tag), tag) }

/**
 * 按作用域命中判定: ANY 命中任一字段即算命中。
 * [videoTags] 为视频真实标签（可为空集 = 尚未补拉，此时标签维度不参与判定）。
 */
internal fun HoyoScopedRule.hitAny(
    title: String,
    up: String,
    part: String,
    normTitle: String,
    normUp: String,
    normPart: String,
    videoTags: Set<String> = emptySet(),
): Boolean = when (scope) {
    KwScope.TITLE -> rule.hit(normTitle, title)
    KwScope.UP -> rule.hit(normUp, up)
    KwScope.PART -> rule.hit(normPart, part)
    KwScope.TAG -> rule.hitAnyTag(videoTags)
    KwScope.ANY -> rule.hit(normTitle, title) || rule.hit(normUp, up) ||
        rule.hit(normPart, part) || rule.hitAnyTag(videoTags)
}

internal fun buildHoyoFairyMatcher(config: HoyoFairyBlockConfig): HoyoFairyMatcher {
    return HoyoFairyMatcher(
        keywordRules = splitLines(config.keywords).mapNotNull { parseScopedRule(it) },
        allowKeywordRules = splitLines(config.allowKeywords).mapNotNull { parseScopedRule(it) },
        partitionRules = splitLines(config.partitions).mapNotNull { parseHoyoRule(it) },
        upNames = splitLines(config.upNames).map { normalizeForMatch(it) }.toSet(),
        uids = splitLines(config.uids).mapNotNull { it.toLongOrNull() }.toSet(),
        bvids = splitLines(config.bvids).map { it.lowercase() }.toSet(),
        allowUpNames = splitLines(config.allowUpNames).map { normalizeForMatch(it) }.toSet(),
        allowUids = splitLines(config.allowUids).mapNotNull { it.toLongOrNull() }.toSet()
    )
}

/**
 * 屏蔽判定(纯函数)。返回 true 表示显示, false 表示隐藏。
 * 判定顺序: 白名单 → 已关注豁免 → UID/UP名/BV → 关键词 → 分区 → 时长 → 播放/点赞 → 刷赞。
 */
internal fun shouldShowHoyoFairyItem(
    config: HoyoFairyBlockConfig,
    matcher: HoyoFairyMatcher,
    item: VideoItem,
    feedKind: FeedKind,
    videoTags: Set<String> = emptySet(),
): Boolean {
    val allowedByKind = when (feedKind) {
        FeedKind.HOME_POPULAR -> config.applyToHot
        FeedKind.HOME_RANK -> config.applyToRank
        FeedKind.SEARCH -> config.applyToSearch
        else -> true
    }
    if (!allowedByKind) return true

    val mid = item.owner.mid
    val upName = item.owner.name
    val title = item.title
    val part = item.tname

    val normTitle = normalizeForMatch(title)
    val normUp = normalizeForMatch(upName)
    val normPart = normalizeForMatch(part)

    // 白名单: 最高优先级豁免
    if (mid > 0L && matcher.allowUids.contains(mid)) return true
    if (upName.isNotBlank() && matcher.allowUpNames.contains(normUp)) return true
    if (matcher.allowKeywordRules.any {
            it.hitAny(title, upName, part, normTitle, normUp, normPart, videoTags)
        }
    ) {
        return true
    }

    // 已关注豁免(仅推荐流)
    if (feedKind == FeedKind.HOME_RECOMMEND && item.isFollowed && config.exemptFollowed) return true

    // 精确维度
    if (mid > 0L && matcher.uids.contains(mid)) return false
    if (upName.isNotBlank() && matcher.upNames.contains(normUp)) return false
    val bvid = item.bvid.lowercase()
    if (bvid.isNotEmpty() && matcher.bvids.contains(bvid)) return false

    // 关键词(标题/UP名/分区/视频标签)
    if (matcher.keywordRules.any {
            it.hitAny(title, upName, part, normTitle, normUp, normPart, videoTags)
        }
    ) {
        return false
    }

    // 分区黑名单
    if (part.isNotBlank() && matcher.partitionRules.any { it.hit(normPart, part) }) return false

    // 时长阈值
    if (item.duration > 0) {
        if (config.minDurationSec > 0 && item.duration < config.minDurationSec) return false
        if (config.maxDurationSec > 0 && item.duration > config.maxDurationSec) return false
    }

    // 播放量(万) / 点赞数阈值
    val views = item.stat.view.toLong()
    val likes = item.stat.like.toLong()
    if (views >= 0) {
        if (config.minViews > 0 && views < config.minViews * 10_000L) return false
        if (config.maxViews > 0 && views > config.maxViews * 10_000L) return false
    }
    if (likes >= 0) {
        if (config.minLikes > 0 && likes < config.minLikes) return false
        if (config.maxLikes > 0 && likes > config.maxLikes) return false
    }

    // 刷赞 / 营销号检测（点赞率 = 点赞/播放 * 100）
    if (config.spamLikeRatio > 0 && views > 0) {
        val threshold = config.spamMinViews * 10_000L
        if (views >= threshold && likes * 100L < config.spamLikeRatio * views) return false
    }

    return true
}

// ==================== 名单导入 / 导出 ====================

internal data class HoyoImportResult(
    val keywords: List<String> = emptyList(),
    val partitions: List<String> = emptyList(),
    val upNames: List<String> = emptyList(),
    val uids: List<String> = emptyList(),
    val bvids: List<String> = emptyList(),
    val ignoredDimensions: List<String> = emptyList(),
) {
    val total: Int get() = keywords.size + partitions.size + upNames.size + uids.size + bvids.size
}

/** 把新增行合并进已有文本(按行去重, 换行分隔)。 */
internal fun mergeRuleLines(existing: String, additions: List<String>): String {
    val current = splitLines(existing).toMutableList()
    val seen = current.toMutableSet()
    for (raw in additions) {
        val v = raw.trim()
        if (v.isEmpty() || seen.contains(v)) continue
        seen.add(v)
        current.add(v)
    }
    return current.joinToString("\n")
}

private fun JsonObject.stringList(key: String): List<String> =
    (this[key] as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.content }
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()

private val SECTION_RE = Regex("^\\[(.+?)]$")

/** 解析订阅 JSON 或带 [关键词]/[UID] 小节头的纯文本。 */
internal fun parseHoyoImport(text: String): HoyoImportResult {
    val src = text.trim()
    if (src.isEmpty()) return HoyoImportResult()

    // 1) JSON 订阅格式
    val root = runCatching { Json.parseToJsonElement(src) as? JsonObject }.getOrNull()
    if (root != null) {
        val rules = (root["rules"] as? JsonObject) ?: root
        val ignored = listOf("tags", "upBio", "dualTags").filter { rules.stringList(it).isNotEmpty() }
        return HoyoImportResult(
            keywords = rules.stringList("keywords"),
            partitions = rules.stringList("partitions"),
            upNames = rules.stringList("upNames"),
            uids = rules.stringList("uids"),
            bvids = rules.stringList("bvids"),
            ignoredDimensions = ignored
        )
    }

    // 2) 小节头纯文本
    val keywords = mutableListOf<String>()
    val partitions = mutableListOf<String>()
    val upNames = mutableListOf<String>()
    val uids = mutableListOf<String>()
    val bvids = mutableListOf<String>()
    var current = "keywords"
    for (raw in src.lines()) {
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue
        val header = SECTION_RE.find(line)?.groupValues?.get(1)?.trim()?.lowercase()
        if (header != null) {
            current = when (header) {
                "关键词", "keywords" -> "keywords"
                "分区", "partitions" -> "partitions"
                "up", "up名", "up主", "upnames" -> "upNames"
                "uid", "uids" -> "uids"
                "bv", "bv号", "bvids" -> "bvids"
                else -> "ignored"
            }
            continue
        }
        when (current) {
            "keywords" -> keywords.add(line)
            "partitions" -> partitions.add(line)
            "upNames" -> upNames.add(line)
            "uids" -> uids.add(line)
            "bvids" -> bvids.add(line)
        }
    }
    return HoyoImportResult(
        keywords = keywords,
        partitions = partitions,
        upNames = upNames,
        uids = uids,
        bvids = bvids
    )
}

private fun jsonArrayOfLines(joined: String): String =
    splitLines(joined).joinToString(", ") { "\"" + it.replace("\\", "\\\\").replace("\"", "\\\"") + "\"" }

/** 导出为 biliHoyoFairy 订阅 JSON。 */
internal fun buildHoyoExportJson(config: HoyoFairyBlockConfig): String = buildString {
    appendLine("{")
    appendLine("  \"app\": \"biliHoyoFairy\",")
    appendLine("  \"format\": 1,")
    appendLine("  \"rules\": {")
    appendLine("    \"keywords\": [" + jsonArrayOfLines(config.keywords) + "],")
    appendLine("    \"partitions\": [" + jsonArrayOfLines(config.partitions) + "],")
    appendLine("    \"upNames\": [" + jsonArrayOfLines(config.upNames) + "],")
    appendLine("    \"uids\": [" + jsonArrayOfLines(config.uids) + "],")
    appendLine("    \"bvids\": [" + jsonArrayOfLines(config.bvids) + "]")
    appendLine("  }")
    append("}")
}
