package com.android.purebilibili.feature.video.playback.policy

import androidx.media3.common.C
import com.android.purebilibili.core.store.SettingsManager
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackPowerPolicyTest {

    @Test
    fun `pip mode keeps network wake mode`() {
        assertEquals(
            C.WAKE_MODE_NETWORK,
            resolvePlaybackWakeMode(
                miniPlayerMode = SettingsManager.MiniPlayerMode.SYSTEM_PIP,
                stopPlaybackOnExit = false
            )
        )
    }

    @Test
    fun `default foreground playback uses local wake mode`() {
        assertEquals(
            C.WAKE_MODE_LOCAL,
            resolvePlaybackWakeMode(
                miniPlayerMode = SettingsManager.MiniPlayerMode.OFF,
                stopPlaybackOnExit = false
            )
        )
        assertEquals(
            C.WAKE_MODE_LOCAL,
            resolvePlaybackWakeMode(
                miniPlayerMode = SettingsManager.MiniPlayerMode.IN_APP_ONLY,
                stopPlaybackOnExit = false
            )
        )
    }

    @Test
    fun `stop on exit disables wake mode retention`() {
        assertEquals(
            C.WAKE_MODE_NONE,
            resolvePlaybackWakeMode(
                miniPlayerMode = SettingsManager.MiniPlayerMode.SYSTEM_PIP,
                stopPlaybackOnExit = true
            )
        )
    }
}
