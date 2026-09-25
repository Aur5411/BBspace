package com.android.purebilibili.feature.settings.webdav

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavBackupAutoSchedulePolicyTest {

    @Test
    fun `should schedule when enabled and config is complete`() {
        val config = WebDavBackupConfig(
            baseUrl = "https://dav.example.com/remote.php/dav/files/demo",
            username = "demo",
            password = "secret",
            remoteDir = "/BiliPai/backups",
            enabled = true
        )

        assertTrue(shouldScheduleWebDavAutoBackup(config))
    }

    @Test
    fun `should not schedule when disabled`() {
        val config = WebDavBackupConfig(
            baseUrl = "https://dav.example.com/dav",
            username = "demo",
            password = "secret",
            enabled = false
        )

        assertFalse(shouldScheduleWebDavAutoBackup(config))
    }

    @Test
    fun `should not schedule when required fields are blank`() {
        assertFalse(
            shouldScheduleWebDavAutoBackup(
                WebDavBackupConfig(
                    baseUrl = "",
                    username = "demo",
                    password = "secret",
                    enabled = true
                )
            )
        )
        assertFalse(
            shouldScheduleWebDavAutoBackup(
                WebDavBackupConfig(
                    baseUrl = "https://dav.example.com/dav",
                    username = "",
                    password = "secret",
                    enabled = true
                )
            )
        )
        assertFalse(
            shouldScheduleWebDavAutoBackup(
                WebDavBackupConfig(
                    baseUrl = "https://dav.example.com/dav",
                    username = "demo",
                    password = "",
                    enabled = true
                )
            )
        )
    }
}
