package com.android.purebilibili.feature.login

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import com.android.purebilibili.core.ui.AppScaffold
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.rememberAppBackIcon
import kotlinx.coroutines.delay

/** B 站官方登录页。鉴权、签名与风控全部由站点自身处理，客户端只回读 Cookie。 */
internal const val BILIBILI_WEB_LOGIN_URL = "https://passport.bilibili.com/login"

private const val BILIBILI_COOKIE_URL = "https://www.bilibili.com"
private const val BILIBILI_PASSPORT_COOKIE_URL = "https://passport.bilibili.com"

private const val MOBILE_WEB_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/131.0.0.0 Mobile Safari/537.36"

/** Google 提供的内核包名：Chrome 或独立的 Android System WebView。 */
private val GOOGLE_WEBVIEW_PACKAGES = setOf(
    "com.android.chrome",
    "com.google.android.webview",
    "com.android.webview",
)

/** 返回当前 WebView 内核的展示名，用于确认走的是 Google 内核。 */
internal fun resolveWebViewEngineLabel(): String {
    val packageName = runCatching {
        android.webkit.WebView.getCurrentWebViewPackage()?.packageName
    }.getOrNull().orEmpty()

    val version = runCatching {
        android.webkit.WebView.getCurrentWebViewPackage()?.versionName
    }.getOrNull().orEmpty()

    return when {
        packageName.isBlank() -> "系统 WebView"
        packageName in GOOGLE_WEBVIEW_PACKAGES ->
            if (packageName == "com.android.chrome") "Chrome $version" else "Google WebView $version"
        else -> "$packageName $version"
    }
}

/** 是否运行在 Google 内核上（Chrome / Android System WebView）。 */
internal fun isGoogleWebViewEngine(): Boolean =
    runCatching { android.webkit.WebView.getCurrentWebViewPackage()?.packageName }
        .getOrNull() in GOOGLE_WEBVIEW_PACKAGES

/**
 * 读取站点写入的登录 Cookie。
 * 只有拿到 [SESSDATA] 才视为登录成功，避免半途跳转误判。
 *
 * SESSDATA 的 domain 为 `.bilibili.com`，主站与 passport 域都能读到，
 * 这里按主站优先读取，读不到再回退到 passport 域。
 */
internal fun readBilibiliWebCookies(): Map<String, String>? {
    for (url in listOf(BILIBILI_COOKIE_URL, BILIBILI_PASSPORT_COOKIE_URL)) {
        val raw = runCatching {
            CookieManager.getInstance().getCookie(url)
        }.getOrNull().orEmpty()

        val values = raw.split(';')
            .asSequence()
            .map(String::trim)
            .mapNotNull { segment ->
                val separator = segment.indexOf('=')
                if (separator <= 0) {
                    null
                } else {
                    segment.substring(0, separator).trim() to
                        segment.substring(separator + 1).trim()
                }
            }
            .toMap()

        if (!values["SESSDATA"].isNullOrBlank()) return values
    }
    return null
}

/**
 * 全屏网页登录容器。
 *
 * UI 仍然由 App 自己的顶栏承载（与登录页风格一致），中间是 B 站官方登录页；
 * 登录成功后立刻回读 Cookie 交给上层走既有的 Cookie 校验通道。
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WebLoginContent(
    onCookiesReady: (Map<String, String>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnCookiesReady by rememberUpdatedState(onCookiesReady)
    var webView by remember { mutableStateOf<WebView?>(null) }
    val engineLabel = remember { resolveWebViewEngineLabel() }
    val googleEngine = remember { isGoogleWebViewEngine() }

    // 站点登录成功后由 JS 写入 Cookie，不一定触发 onPageFinished，因此轮询兜底。
    // 先清空上一段网页会话，避免残留 Cookie 被误判为本次登录成功。
    LaunchedEffect(Unit) {
        runCatching {
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                removeAllCookies(null)
                flush()
            }
        }
        // removeAllCookies 是异步的，必须等残留真的被清掉再开始轮询，
        // 否则上一段网页会话的 SESSDATA 会被误判为本次登录成功。
        var guard = 0
        while (guard < 20 && readBilibiliWebCookies() != null) {
            delay(150)
            guard++
        }
        while (true) {
            delay(500)
            val cookies = readBilibiliWebCookies()
            if (cookies != null) {
                latestOnCookiesReady(cookies)
                return@LaunchedEffect
            }
        }
    }

    BackHandler {
        val view = webView
        if (view != null && view.canGoBack()) {
            view.goBack()
        } else {
            onClose()
        }
    }

    AppScaffold(
        modifier = modifier,
        containerColor = AppSurfaceTokens.chromeBackground(),
        topBar = {
            AppTopBar(
                title = "B 站账号登录",
                subtitle = "B 站官方页面 · $engineLabel",
                navigationIcon = {
                    AppIconButton(onClick = onClose) {
                        AppIcon(
                            imageVector = rememberAppBackIcon(),
                            contentDescription = "返回登录方式选择",
                        )
                    }
                },
                actions = {
                    AppIconButton(onClick = { webView?.reload() }) {
                        AppIcon(Icons.Outlined.Refresh, contentDescription = "刷新页面")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppSurfaceTokens.chromeBackground(),
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!googleEngine) {
                // 内核不是 Google 提供时（部分定制系统内置内核），B 站登录页可能渲染异常。
                AppText(
                    text = "当前内核为 $engineLabel，若页面无法正常登录，" +
                        "请安装或更新 Google 的 Android System WebView / Chrome 后重试。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.userAgentString = MOBILE_WEB_USER_AGENT
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                val target = request?.url?.toString() ?: return false
                                // 非 http(s) 的 App 唤起（bilibili:// 等）一律留在页面内处理，
                                // 避免登录过程中被外部 App 抢走导致 Cookie 未落地。
                                return !target.startsWith("http://") && !target.startsWith("https://")
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // 站点登录页的协议文字/按钮容易重叠，注入既有样式做修正。
                                view?.evaluateJavascript(WEB_LOGIN_INJECT_JS, null)
                                readBilibiliWebCookies()?.let { latestOnCookiesReady(it) }
                            }
                        }

                        loadUrl(BILIBILI_WEB_LOGIN_URL)
                        webView = this
                    }
                },
                onRelease = { view ->
                    view.stopLoading()
                    view.destroy()
                    if (webView === view) webView = null
                },
            )
        }
    }
}
