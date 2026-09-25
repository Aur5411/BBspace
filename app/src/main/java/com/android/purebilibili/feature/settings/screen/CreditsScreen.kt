// 文件路径: feature/settings/screen/CreditsScreen.kt
//
// 「感谢名单」页。
//
// ★ 这里只列本工程**实际依赖或实际参考**的开源项目，不写荣誉性占位条目。
//   每一条都能在 app/build.gradle.kts 的依赖或源码引用里对上：
//   - 上游/参考项目: BiliPai / PiliPala / bilibili-API-collect / Animeko
//   - UI: Miuix(miuix-kmp) / material-kolor / haze / cupertino / Compose Richeditor /
//         ColorPicker Compose / Lottie / material-icons-extended
//   - 媒体: AndroidX Media3(ExoPlayer) / Coil / (dolby-ffmpeg-decoder 子模块)
//   - 网络与序列化: Retrofit / OkHttp / kotlinx-serialization / kotlinx-coroutines / Brotli
//   - 数据与存储: Room / DataStore
//   - 工具: ZXing / pinyin4j / NanoHTTPD / AndroidX 全家桶 / Firebase Crashlytics
//   - 测试: JUnit / MockK / Turbine
package com.android.purebilibili.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.purebilibili.R
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.theme.iOSYellow
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.animation.EntranceGroup
import com.android.purebilibili.core.ui.animation.entrance
import com.android.purebilibili.core.ui.components.AppHorizontalDivider
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppPreferenceSectionTitle
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.rememberAdaptivePreferenceIconContainerColor
import com.android.purebilibili.core.ui.components.rememberAdaptivePreferenceIconContentColor
import com.android.purebilibili.core.ui.resolveBottomSafeAreaPadding
import com.android.purebilibili.feature.settings.ui.SettingsPageScaffold

/** 单个开源项目条目。 */
private data class CreditEntry(
    /** 项目名。 */
    val name: String,
    /** GitHub 仓库短地址，如 `jay3-yy/BiliPai`。 */
    val repo: String,
    /** 在本工程里承担什么。 */
    val description: String,
)

/** 一组同类项目。 */
private data class CreditGroup(
    @DrawableRes val iconResId: Int,
    val iconTint: Color,
    val title: String,
    val entries: List<CreditEntry>,
)

private const val GITHUB_PREFIX = "https://github.com/"

private val creditGroups: List<CreditGroup> = listOf(
    CreditGroup(
        iconResId = R.drawable.ms_language_24,
        iconTint = iOSBlue,
        title = "上游项目与参考资料",
        entries = listOf(
            CreditEntry(
                name = "BiliPai",
                repo = "jay3-yy/BiliPai",
                description = "本应用的上游框架：界面体系、播放器与整体页面结构",
            ),
            CreditEntry(
                name = "PiliPala",
                repo = "guozhigq/pilipala",
                description = "第三方 B 站客户端，交互与功能设计参考",
            ),
            CreditEntry(
                name = "bilibili-API-collect",
                repo = "SocialSisterYi/bilibili-API-collect",
                description = "B 站接口协议与鉴权流程文档",
            ),
            CreditEntry(
                name = "Animeko",
                repo = "Him188/animeko",
                description = "番剧模块数据源（api.animeko.org）、换源与弹幕能力",
            ),
        ),
    ),
    CreditGroup(
        iconResId = R.drawable.ms_palette_24,
        iconTint = iOSPurple,
        title = "界面与设计系统",
        entries = listOf(
            CreditEntry(
                name = "Miuix (miuix-kmp)",
                repo = "miuix-kmp/miuix",
                description = "组件库、背景模糊、着色器、图标与导航容器",
            ),
            CreditEntry(
                name = "Compose Material 3",
                repo = "androidx/androidx",
                description = "Material 3 组件、窗口尺寸类与自适应布局",
            ),
            CreditEntry(
                name = "material-kolor",
                repo = "jordond/materialkolor",
                description = "跟随系统壁纸的 Material You 动态取色",
            ),
            CreditEntry(
                name = "haze",
                repo = "chrisbanes/haze",
                description = "顶栏与底栏的毛玻璃模糊材质",
            ),
            CreditEntry(
                name = "cupertino",
                repo = "alexzhirkevich/compass",
                description = "iOS 风格控件与图标补全",
            ),
            CreditEntry(
                name = "Compose Rich Editor",
                repo = "MohamedRejeb/Compose-Rich-Editor",
                description = "动态发布等场景的富文本输入",
            ),
            CreditEntry(
                name = "ColorPicker Compose",
                repo = "skydoves/ColorPickerCompose",
                description = "主题自定义的取色器",
            ),
            CreditEntry(
                name = "Lottie",
                repo = "airbnb/lottie-android",
                description = "部分动效与骨架动画",
            ),
        ),
    ),
    CreditGroup(
        iconResId = R.drawable.ms_movie_24,
        iconTint = iOSOrange,
        title = "媒体与图片",
        entries = listOf(
            CreditEntry(
                name = "AndroidX Media3 (ExoPlayer)",
                repo = "androidx/media",
                description = "视频/直播播放内核、DASH/HLS 与投屏会话",
            ),
            CreditEntry(
                name = "Coil",
                repo = "coil-kt/coil",
                description = "图片与 GIF 加载、缓存",
            ),
            CreditEntry(
                name = "FFmpegKit / Dolby 解码子模块",
                repo = "arthenica/ffmpeg-kit",
                description = "杜比与特殊音轨的软解补偿",
            ),
        ),
    ),
    CreditGroup(
        iconResId = R.drawable.ms_hub_24,
        iconTint = iOSTeal,
        title = "网络与序列化",
        entries = listOf(
            CreditEntry(
                name = "Retrofit",
                repo = "square/retrofit",
                description = "接口声明式调用",
            ),
            CreditEntry(
                name = "OkHttp",
                repo = "square/okhttp",
                description = "HTTP 客户端、Cookie 与拦截器链路",
            ),
            CreditEntry(
                name = "kotlinx.serialization",
                repo = "Kotlin/kotlinx.serialization",
                description = "JSON 解析与导航参数序列化",
            ),
            CreditEntry(
                name = "kotlinx.coroutines",
                repo = "Kotlin/kotlinx.coroutines",
                description = "全应用的异步与状态流基础",
            ),
            CreditEntry(
                name = "Brotli (org.brotli:dec)",
                repo = "google/brotli",
                description = "接口 Brotli 压缩体的解压",
            ),
            CreditEntry(
                name = "NanoHTTPD",
                repo = "NanoHttpd/nanohttpd",
                description = "插件与外部播放器场景的本地回环服务",
            ),
        ),
    ),
    CreditGroup(
        iconResId = R.drawable.ms_analytics_24,
        iconTint = iOSGreen,
        title = "数据与存储",
        entries = listOf(
            CreditEntry(
                name = "Room",
                repo = "androidx/androidx",
                description = "追番收藏、历史、缓存等本地数据库",
            ),
            CreditEntry(
                name = "DataStore",
                repo = "androidx/androidx",
                description = "设置项的持久化与迁移",
            ),
            CreditEntry(
                name = "Firebase Crashlytics",
                repo = "firebase/firebase-android-sdk",
                description = "崩溃采集（可在设置里关闭）",
            ),
        ),
    ),
    CreditGroup(
        iconResId = R.drawable.ms_extension_24,
        iconTint = iOSPink,
        title = "工具与组件",
        entries = listOf(
            CreditEntry(
                name = "ZXing",
                repo = "zxing/zxing",
                description = "二维码识别（登录扫码、插件导入）",
            ),
            CreditEntry(
                name = "pinyin4j",
                repo = "belerweb/pinyin4j",
                description = "UP 主与分区的拼音搜索匹配",
            ),
            CreditEntry(
                name = "AndroidX (Core / Lifecycle / Work / Camera)",
                repo = "androidx/androidx",
                description = "基础能力、生命周期、后台任务与相机",
            ),
            CreditEntry(
                name = "Kotlin",
                repo = "JetBrains/kotlin",
                description = "开发语言",
            ),
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val contentBottomPadding = resolveBottomSafeAreaPadding(
        navigationBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        extraBottomPadding = 32.dp,
    )

    EntranceGroup {
        SettingsPageScaffold(
            title = "感谢名单",
            onBack = onBack,
            backContentDescription = "返回",
            bottomContentPadding = contentBottomPadding,
            lazyListContent = {
                item {
                    Box(modifier = Modifier.entrance()) {
                        AppPreferenceSectionTitle("本工程用到的开源项目")
                    }
                }
                item {
                    Box(modifier = Modifier.entrance()) {
                        AppText(
                            text = "下面每一项都能在工程的依赖清单或源码引用中找到对应。" +
                                "点击任意条目可打开其 GitHub 仓库。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = AppSpacingTokens.Large + AppSpacingTokens.ExtraSmall,
                                end = AppSpacingTokens.Large + AppSpacingTokens.ExtraSmall,
                                bottom = AppSpacingTokens.Small,
                            ),
                        )
                    }
                }
                creditGroups.forEach { group ->
                    item(key = "group-${group.title}") {
                        Box(modifier = Modifier.entrance()) {
                            CreditGroupCard(
                                group = group,
                                onOpenRepo = { repo -> uriHandler.openUri(GITHUB_PREFIX + repo) },
                            )
                        }
                    }
                    item(key = "group-space-${group.title}") {
                        Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
                    }
                }
            },
        )
    }
}

@Composable
private fun CreditGroupCard(
    group: CreditGroup,
    onOpenRepo: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacingTokens.Large + AppSpacingTokens.ExtraSmall)
            .clip(AppShapes.container(ContainerLevel.Field))
            .background(AppSurfaceTokens.cardContainer()),
    ) {
        group.entries.forEachIndexed { index, entry ->
            CreditRow(
                iconResId = group.iconResId,
                iconTint = group.iconTint,
                entry = entry,
                onClick = { onOpenRepo(entry.repo) },
            )
            if (index != group.entries.lastIndex) {
                AppHorizontalDivider(modifier = Modifier.padding(start = 64.dp))
            }
        }
    }
}

@Composable
private fun CreditRow(
    @DrawableRes iconResId: Int,
    iconTint: Color,
    entry: CreditEntry,
    onClick: () -> Unit,
) {
    val icon = rememberMaterialSymbol(iconResId)
    val containerColor = rememberAdaptivePreferenceIconContainerColor(iconTint)
    val contentColor = rememberAdaptivePreferenceIconContentColor(containerColor)
    val repoShape = remember { RoundedCornerShape(6.dp) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacingTokens.Large, vertical = AppSpacingTokens.Medium),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(AppShapes.container(ContainerLevel.Field))
                .background(containerColor),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            AppText(
                text = entry.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Micro))
            AppText(
                text = entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Micro))
            AppText(
                text = entry.repo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(repoShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
