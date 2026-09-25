package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppUpdateUiPolicyTest {

    @Test
    fun checkUpdate_withoutNewVersion_orReleaseNotesRequest_showsNoDialog() {
        assertEquals(
            AppUpdateDialogMode.NONE,
            resolveAppUpdateDialogMode(
                isUpdateAvailable = false,
                shouldOpenReleaseNotes = false
            )
        )
    }

    @Test
    fun releaseNotesRequest_withoutNewVersion_showsChangelogDialog() {
        assertEquals(
            AppUpdateDialogMode.CHANGELOG,
            resolveAppUpdateDialogMode(
                isUpdateAvailable = false,
                shouldOpenReleaseNotes = true
            )
        )
    }

    @Test
    fun newVersion_alwaysUsesUpdateDialog_evenWhenReleaseNotesRequested() {
        assertEquals(
            AppUpdateDialogMode.UPDATE_AVAILABLE,
            resolveAppUpdateDialogMode(
                isUpdateAvailable = true,
                shouldOpenReleaseNotes = true
            )
        )
    }

    @Test
    fun autoCheckSubtitle_mentionsAppEntry_whenEnabled() {
        assertEquals(
            "进入应用时自动检查新版本",
            resolveAutoCheckUpdateSubtitle(autoCheckEnabled = true)
        )
    }

    @Test
    fun autoCheckSubtitle_guidesManualCheck_whenDisabled() {
        assertEquals(
            "关闭后仅手动检查",
            resolveAutoCheckUpdateSubtitle(autoCheckEnabled = false)
        )
    }

    @Test
    fun appEntryAutoCheck_requiresBothToggleAndGate() {
        assertTrue(shouldRunAppEntryAutoCheck(autoCheckEnabled = true, gateAllowsCheck = true))
        assertFalse(shouldRunAppEntryAutoCheck(autoCheckEnabled = false, gateAllowsCheck = true))
        assertFalse(shouldRunAppEntryAutoCheck(autoCheckEnabled = true, gateAllowsCheck = false))
    }
}
