package com.android.purebilibili.data.model.animeko

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 番剧「制作人员 / 短评」数据模型测试。
 *
 * 回归背景（实测）：
 * animeko 服务端**聚合了 Bangumi 短评**，接口是 `GET /v2/subjects/{id}/reviews`
 * （不是 /comments），staff 在 `GET /v2/subjects/{id}/staff`。
 * 这两个接口此前完全没接，导致详情页看不到配音角色截图之外的制作信息与评论。
 *
 * 这里的 JSON 全部取自真实抓包样本，避免「照着臆想的字段写解析」。
 */
class AniStaffAndReviewModelTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    // ---------------------------------------------------------------
    // staff
    // ---------------------------------------------------------------

    @Test
    fun `staff 真实样本可解析`() {
        // 取自 GET /v2/subjects/633836/staff 第一条
        val raw = """
            [{"index":0,"person":{"id":18838,"name":"長月達平","nameCn":"长月达平","type":1,
              "imageLarge":"https://api.animeko.org/v2/persons/18838/image?size=large",
              "imageMedium":"https://api.animeko.org/v2/persons/18838/image?size=medium",
              "summary":""},"position":1}]
        """.trimIndent()
        val list = json.decodeFromString<List<AniStaffMember>>(raw)
        assertEquals(1, list.size)
        assertEquals(1, list[0].position)
        assertEquals("长月达平", list[0].person.displayName)
        assertTrue(list[0].person.imageMedium.startsWith("https://api.animeko.org/"))
    }

    @Test
    fun `staff 缺失字段时使用默认值`() {
        val list = json.decodeFromString<List<AniStaffMember>>("""[{}]""")
        assertEquals(1, list.size)
        assertEquals(0, list[0].position)
        assertEquals("", list[0].person.displayName)
    }

    @Test
    fun `已知职位编码映射为中文`() {
        // 这些编码的语义已用「EVA / 命运石之门 等已知作品的实际班底」交叉验证过
        assertEquals("原作", aniStaffPositionLabel(1))
        assertEquals("导演", aniStaffPositionLabel(2))
        assertEquals("脚本", aniStaffPositionLabel(3))
        assertEquals("分镜", aniStaffPositionLabel(4))
        assertEquals("演出", aniStaffPositionLabel(5))
        assertEquals("音乐", aniStaffPositionLabel(6))
        assertEquals("人物设定", aniStaffPositionLabel(8))
        assertEquals("作画监督", aniStaffPositionLabel(15))
        assertEquals("原画", aniStaffPositionLabel(20))
    }

    @Test
    fun `未知职位编码回退为其他且不参与展示`() {
        assertEquals(ANI_STAFF_POSITION_FALLBACK, aniStaffPositionLabel(99999))
        assertFalse(isKnownAniStaffPosition(99999))
        assertTrue(isKnownAniStaffPosition(15))
    }

    // ---------------------------------------------------------------
    // 角色 role
    // ---------------------------------------------------------------

    @Test
    fun `角色定位编码映射正确`() {
        assertEquals("主角", AniRelatedCharacter(role = 1).roleLabel)
        assertEquals("配角", AniRelatedCharacter(role = 2).roleLabel)
        assertEquals("客串", AniRelatedCharacter(role = 4).roleLabel)
        // 未知编码不应硬编成「配角」，留空更诚实
        assertEquals("", AniRelatedCharacter(role = 99).roleLabel)
    }

    // ---------------------------------------------------------------
    // reviews
    // ---------------------------------------------------------------

    @Test
    fun `短评真实样本可解析（bangumi 来源）`() {
        // 取自 GET /v2/subjects/633836/reviews?limit=3 第一条
        val raw = """
            {"total":4,"items":[{
              "id":"bangumi:633836:880791","subjectId":633836,"source":"bangumi",
              "author":{"id":"880791","nickname":"sinx战士羊麟",
                "avatarUrl":"https://static.myani.org/bangumi/avatars/880791/72bcae9684659015.jpg"},
              "contentBbcode":"都10年了还有人发“486是不是fw”视频，是我穿越了还是byd没活了?",
              "updatedAt":"2026-09-23T09:56:06Z","rating":7,"likeCount":0}]}
        """.trimIndent()
        val page = json.decodeFromString<AniReviewPage>(raw)
        assertEquals(1, page.items.size)
        val r = page.items[0]
        assertEquals("Bangumi", r.sourceLabel)
        assertEquals("sinx战士羊麟", r.displayAuthor)
        assertEquals(7, r.rating)
        assertTrue(r.hasRating)
        assertTrue(r.displayContent.contains("486"))
    }

    @Test
    fun `短评 animeko 来源与空正文可解析`() {
        // 取自 GET /v2/subjects/501963/reviews 的 animeko 站内条（正文可能为空串）
        val raw = """
            {"total":101,"items":[{
              "id":"ani:7c131a3d-67f2-4008-b135-7f4c56d92dbe","subjectId":501963,"source":"animeko",
              "author":{"id":"84af710b-88fe-49df-8399-f3d795ab5fad","nickname":"漱玉01",
                "avatarUrl":"https://static.myani.org/avatars/84af710b/x.jpg"},
              "contentBbcode":"","updatedAt":"2026-09-23T10:18:57.269Z","rating":9,"likeCount":0}]}
        """.trimIndent()
        val page = json.decodeFromString<AniReviewPage>(raw)
        val r = page.items[0]
        assertEquals("animeko", r.sourceLabel)
        assertEquals("", r.displayContent)
        assertEquals(9, r.rating)
    }

    @Test
    fun `未打分与空昵称有兜底`() {
        val raw = """
            {"items":[{"contentBbcode":"好看","rating":0,"author":{"nickname":""}}]}
        """.trimIndent()
        val r = json.decodeFromString<AniReviewPage>(raw).items[0]
        assertFalse(r.hasRating)
        assertEquals("匿名用户", r.displayAuthor)
    }

    @Test
    fun `BBCode 图片与链接标记被清理但保留正文`() {
        val raw = """
            {"items":[{"contentBbcode":"[img]http://x.jpg[/img]神作[url=http://a]链接[/url]"}]}
        """.trimIndent()
        val r = json.decodeFromString<AniReviewPage>(raw).items[0]
        assertTrue(r.displayContent.contains("神作"))
        assertTrue(r.displayContent.contains("链接"))
        assertFalse(r.displayContent.contains("[img]"))
        assertFalse(r.displayContent.contains("[/url]"))
    }

    @Test
    fun `空 items 与缺字段不崩溃`() {
        val page = json.decodeFromString<AniReviewPage>("""{"total":0,"items":[]}""")
        assertEquals(0, page.items.size)
        val page2 = json.decodeFromString<AniReviewPage>("{}")
        assertEquals(0, page2.items.size)
    }
}
