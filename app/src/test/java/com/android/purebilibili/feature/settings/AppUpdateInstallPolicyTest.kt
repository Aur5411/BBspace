package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppUpdateInstallPolicyTest {

    @Test
    fun androidOAndAbove_withoutUnknownSourcesPermission_routesToSettings() {
        assertEquals(
            AppUpdateInstallAction.OPEN_UNKNOWN_SOURCES_SETTINGS,
            resolveAppUpdateInstallAction(
                sdkInt = 26,
                canRequestPackageInstalls = false
            )
        )
    }

    @Test
    fun androidOAndAbove_withUnknownSourcesPermission_installsDirectly() {
        assertEquals(
            AppUpdateInstallAction.START_INSTALL,
            resolveAppUpdateInstallAction(
                sdkInt = 34,
                canRequestPackageInstalls = true
            )
        )
    }

    @Test
    fun preAndroidO_installsDirectly_withoutUnknownSourcesCheck() {
        assertEquals(
            AppUpdateInstallAction.START_INSTALL,
            resolveAppUpdateInstallAction(
                sdkInt = 25,
                canRequestPackageInstalls = false
            )
        )
    }
}
