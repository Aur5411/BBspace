package com.android.purebilibili.feature.video.playback.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackPostLoadPlanPolicyTest {

    @Test
    fun `guest plan should prioritize player info and videoshot before enrichment`() {
        val plan = buildPlaybackPostLoadPlan(
            isLoggedIn = false,
            shouldShowOnlineCount = false
        )

        val playerInfoDelay = plan.firstDelayOf(PlaybackPostLoadTask.PLAYER_INFO)
        val videoshotDelay = plan.firstDelayOf(PlaybackPostLoadTask.VIDEO_SHOT)
        val ownerStatsDelay = plan.firstDelayOf(PlaybackPostLoadTask.OWNER_STATS)
        val tagsDelay = plan.firstDelayOf(PlaybackPostLoadTask.VIDEO_TAGS)
        val aiSummaryDelay = plan.firstDelayOf(PlaybackPostLoadTask.AI_SUMMARY)
        val heartbeatDelay = plan.firstDelayOf(PlaybackPostLoadTask.HEARTBEAT)

        assertEquals(150L, playerInfoDelay)
        assertEquals(150L, videoshotDelay)
        assertTrue(ownerStatsDelay > playerInfoDelay)
        assertTrue(tagsDelay > playerInfoDelay)
        assertTrue(aiSummaryDelay > ownerStatsDelay)
        assertTrue(heartbeatDelay > playerInfoDelay)
    }

    @Test
    fun `logged in plan should include deferred auth dependent enrichments`() {
        val plan = buildPlaybackPostLoadPlan(
            isLoggedIn = true,
            shouldShowOnlineCount = false
        )

        assertTrue(plan.containsTask(PlaybackPostLoadTask.REFRESH_DEFERRED_SIGNALS))
        assertTrue(plan.containsTask(PlaybackPostLoadTask.LOAD_FOLLOWING_MIDS))
        assertTrue(plan.containsTask(PlaybackPostLoadTask.PLUGIN_ON_VIDEO_LOAD))
        assertTrue(plan.containsTask(PlaybackPostLoadTask.START_PLUGIN_CHECK))
    }

    @Test
    fun `guest plan should skip auth dependent enrichments`() {
        val plan = buildPlaybackPostLoadPlan(
            isLoggedIn = false,
            shouldShowOnlineCount = false
        )

        assertFalse(plan.containsTask(PlaybackPostLoadTask.REFRESH_DEFERRED_SIGNALS))
        assertFalse(plan.containsTask(PlaybackPostLoadTask.LOAD_FOLLOWING_MIDS))
    }

    @Test
    fun `online count should only be scheduled when enabled`() {
        val disabled = buildPlaybackPostLoadPlan(
            isLoggedIn = false,
            shouldShowOnlineCount = false
        )
        val enabled = buildPlaybackPostLoadPlan(
            isLoggedIn = false,
            shouldShowOnlineCount = true
        )

        assertFalse(disabled.containsTask(PlaybackPostLoadTask.ONLINE_COUNT))
        assertTrue(enabled.containsTask(PlaybackPostLoadTask.ONLINE_COUNT))
    }

    private fun List<PlaybackPostLoadTaskSpec>.containsTask(task: PlaybackPostLoadTask): Boolean {
        return any { spec -> spec.task == task }
    }

    private fun List<PlaybackPostLoadTaskSpec>.firstDelayOf(task: PlaybackPostLoadTask): Long {
        return first { spec -> spec.task == task }.delayMs
    }
}
