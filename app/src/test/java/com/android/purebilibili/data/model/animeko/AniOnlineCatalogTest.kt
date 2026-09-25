package com.android.purebilibili.data.model.animeko

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 在线源（maccms 采集接口）解析与「可否直接播放」判定的单元测试。
 *
 * 这些是「番剧能不能播」的核心逻辑，必须可回归：
 * - vod_play_url 拆分（标准 `第01集$url#第02集$url`）
 * - 集数匹配（要能挑对集，且多集番剧绝不回退到别的集）
 * - 多条目候选里挑正片（不要挑到剧场版/总集篇）
 * - playableDirectly：只有在线直链为 true，种子/磁力为 false
 */
class AniOnlineCatalogTest {

    private val detailJson = """
        {
          "code": 1,
          "list": [
            {
              "vod_name": "孤独摇滚！",
              "vod_play_from": "lzm3u8",
              "vod_play_url": "第01集${'$'}https://a.example/1/index.m3u8#第02集${'$'}https://a.example/2/index.m3u8#第12集${'$'}https://a.example/12/index.m3u8"
            },
            {
              "vod_name": "孤独摇滚(剧场总集篇)",
              "vod_play_from": "lzm3u8",
              "vod_play_url": "HD中字${'$'}https://b.example/movie/index.m3u8"
            }
          ]
        }
    """.trimIndent()

    // ---------------------------------------------------------------
    // JSON 解析
    // ---------------------------------------------------------------

    @Test
    fun `解析详情 JSON 得到条目列表`() {
        val items = AniOnlineCatalog.parseDetail(detailJson)
        assertEquals(2, items.size)
        assertEquals("孤独摇滚！", items[0].name)
        assertEquals("lzm3u8", items[0].playFrom)
    }

    @Test
    fun `空响应或坏 JSON 返回空列表而不抛异常`() {
        assertTrue(AniOnlineCatalog.parseDetail("").isEmpty())
        assertTrue(AniOnlineCatalog.parseDetail("not json at all").isEmpty())
    }

    // ---------------------------------------------------------------
    // vod_play_url 拆分
    // ---------------------------------------------------------------

    @Test
    fun `拆分标准 maccms 播放串`() {
        val entries = AniOnlineCatalog.splitPlayUrls(
            "第01集\$https://a/1.m3u8#第02集\$https://a/2.m3u8"
        )
        assertEquals(2, entries.size)
        assertEquals("第01集", entries[0].label)
        assertEquals("https://a/1.m3u8", entries[0].url)
        assertEquals("第02集", entries[1].label)
        assertEquals("https://a/2.m3u8", entries[1].url)
    }

    @Test
    fun `名称与地址顺序颠倒时也能解析`() {
        // 有些站会给出「地址#集名」的顺序
        val entries = AniOnlineCatalog.splitPlayUrls("https://a/1.m3u8#第01集")
        assertEquals(1, entries.size)
        assertEquals("https://a/1.m3u8", entries[0].url)
        assertEquals("第01集", entries[0].label)
    }

    @Test
    fun `过滤掉没有 http 地址的段`() {
        val entries = AniOnlineCatalog.splitPlayUrls("第01集\$magnet:?xt=urn:btih:abc#第02集\$https://a/2.m3u8")
        assertEquals(1, entries.size)
        assertEquals("https://a/2.m3u8", entries[0].url)
    }

    // ---------------------------------------------------------------
    // 集数解析与匹配
    // ---------------------------------------------------------------

    @Test
    fun `从多种集名里取出集数`() {
        assertEquals(12, AniOnlineCatalog.parseEpisodeNumber("第12集"))
        assertEquals(3, AniOnlineCatalog.parseEpisodeNumber("03"))
        assertEquals(7, AniOnlineCatalog.parseEpisodeNumber("EP07"))
        assertNull(AniOnlineCatalog.parseEpisodeNumber("HD中字"))
    }

    @Test
    fun `能挑中目标集`() {
        val entries = AniOnlineCatalog.splitPlayUrls(
            "第01集\$https://a/1.m3u8#第05集\$https://a/5.m3u8#第12集\$https://a/12.m3u8"
        )
        val picked = AniOnlineCatalog.pickEpisode(entries, episodeSort = "5")
        assertNotNull(picked)
        assertEquals("https://a/5.m3u8", picked.url)
    }

    @Test
    fun `多集番剧缺目标集时不回退到别的集`() {
        val entries = AniOnlineCatalog.splitPlayUrls(
            "第01集\$https://a/1.m3u8#第02集\$https://a/2.m3u8"
        )
        // 目标第 9 集不存在 —— 必须返回空，宁可不播也不能播错集
        assertNull(AniOnlineCatalog.pickEpisode(entries, episodeSort = "9"))
    }

    @Test
    fun `只有单条（剧场版）时允许回退`() {
        val entries = AniOnlineCatalog.splitPlayUrls("HD中字\$https://b/movie.m3u8")
        val picked = AniOnlineCatalog.pickEpisode(entries, episodeSort = "1")
        assertNotNull(picked)
        assertEquals("https://b/movie.m3u8", picked.url)
    }

    // ---------------------------------------------------------------
    // 候选条目挑选
    // ---------------------------------------------------------------

    @Test
    fun `优先挑正片而不是剧场版`() {
        val items = AniOnlineCatalog.parseDetail(detailJson)
        val best = AniOnlineCatalog.pickBestItem(
            items = items,
            subjectName = "孤独摇滚！",
            wantEpisode = 5,
        )
        assertNotNull(best)
        assertEquals("孤独摇滚！", best.name)
    }

    @Test
    fun `没有同名条目时回退到集数最多的那个`() {
        val items = AniOnlineCatalog.parseDetail(detailJson)
        val best = AniOnlineCatalog.pickBestItem(
            items = items,
            subjectName = "完全不存在的番名",
            wantEpisode = null,
        )
        assertNotNull(best)
        // 12 集的那条比单集剧场版更可能是正片
        assertEquals("孤独摇滚！", best.name)
    }

    // ---------------------------------------------------------------
    // 可否直接播放
    // ---------------------------------------------------------------

    @Test
    fun `在线 m3u8 可直链播放`() {
        val candidate = AniMediaCandidate(
            kind = AniMediaSourceKind.ONLINE.name,
            url = "https://vip.lz-cdn14.com/20221009/12985_d1e80e23/index.m3u8",
        )
        assertTrue(candidate.playableDirectly)
    }

    @Test
    fun `种子与磁力不可直链播放`() {
        val torrent = AniMediaCandidate(
            kind = AniMediaSourceKind.BT.name,
            url = "https://mikanime.tv/Download/123.torrent",
        )
        assertFalse(torrent.playableDirectly)

        val magnet = AniMediaCandidate(
            kind = AniMediaSourceKind.ONLINE.name,
            url = "magnet:?xt=urn:btih:abcdef",
        )
        assertFalse(magnet.playableDirectly)
    }

    @Test
    fun `BT 源即使给的是 http 地址也不自动播放`() {
        val btHttp = AniMediaCandidate(
            kind = AniMediaSourceKind.BT.name,
            url = "https://mikanime.tv/some/page",
        )
        assertFalse(btHttp.playableDirectly)
    }
}
