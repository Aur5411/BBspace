package com.android.purebilibili.core.network.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 第三方域名站点头隔离策略测试。
 *
 * 回归背景（实测）：
 * `api.animeko.org` 收到 `Origin: https://www.bilibili.com` 时直接返回 403，
 * 曾导致番剧详情页的角色图与声优图全部加载失败。
 */
class ThirdPartyHostPolicyTest {

    @Test
    fun `B站主域名与子域被识别`() {
        assertTrue(isBilibiliEcosystemHost("bilibili.com"))
        assertTrue(isBilibiliEcosystemHost("www.bilibili.com"))
        assertTrue(isBilibiliEcosystemHost("api.bilibili.com"))
        assertTrue(isBilibiliEcosystemHost("app.bilibili.com"))
        assertTrue(isBilibiliEcosystemHost("space.bilibili.com"))
        assertTrue(isBilibiliEcosystemHost("t.bilibili.com"))
        assertTrue(isBilibiliEcosystemHost("api.live.bilibili.com"))
    }

    @Test
    fun `B站资源CDN被识别`() {
        assertTrue(isBilibiliEcosystemHost("i0.hdslb.com"))
        assertTrue(isBilibiliEcosystemHost("s1.hdslb.com"))
        assertTrue(isBilibiliEcosystemHost("upos-sz-mirrorcos.bilivideo.com"))
        assertTrue(isBilibiliEcosystemHost("b23.tv"))
        assertTrue(isBilibiliEcosystemHost("biliapi.net"))
    }

    @Test
    fun `追番模块的animeko域名不属于B站`() {
        // 这两个是本次 bug 的直接触发点
        assertFalse(isBilibiliEcosystemHost("api.animeko.org"))
        assertFalse(isBilibiliEcosystemHost("static.myani.org"))
        assertFalse(shouldAttachBilibiliSiteHeaders("api.animeko.org"))
        assertFalse(shouldAttachBilibiliSiteHeaders("static.myani.org"))
    }

    @Test
    fun `采集源与镜像站不属于B站`() {
        assertFalse(isBilibiliEcosystemHost("cj.lziapi.com"))
        assertFalse(isBilibiliEcosystemHost("api.ffzyapi.com"))
        assertFalse(isBilibiliEcosystemHost("360zy.com"))
        assertFalse(isBilibiliEcosystemHost("mikanime.tv"))
    }

    @Test
    fun `近似域名不会被误判`() {
        // 后缀必须以 `.` 或整体匹配为准，不能只做 contains
        assertFalse(isBilibiliEcosystemHost("notbilibili.com"))
        assertFalse(isBilibiliEcosystemHost("bilibili.com.evil.com"))
        assertFalse(isBilibiliEcosystemHost("fakebilibili.com"))
        assertFalse(isBilibiliEcosystemHost("mybilibili.com"))
        assertFalse(isBilibiliEcosystemHost("xbilibili.net"))
    }

    @Test
    fun `大小写不敏感`() {
        assertTrue(isBilibiliEcosystemHost("API.BILIBILI.COM"))
        assertTrue(isBilibiliEcosystemHost("WWW.Bilibili.Com"))
        assertFalse(isBilibiliEcosystemHost("API.Animeko.Org"))
    }

    @Test
    fun `空串与异常输入返回false`() {
        assertFalse(isBilibiliEcosystemHost(""))
        assertFalse(isBilibiliEcosystemHost("."))
        assertFalse(isBilibiliEcosystemHost("localhost"))
    }
}
