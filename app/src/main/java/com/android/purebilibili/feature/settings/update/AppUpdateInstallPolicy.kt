package com.android.purebilibili.feature.settings

internal enum class AppUpdateInstallAction {
    START_INSTALL,
    OPEN_UNKNOWN_SOURCES_SETTINGS
}

internal fun resolveAppUpdateInstallAction(
    sdkInt: Int,
    canRequestPackageInstalls: Boolean
): AppUpdateInstallAction {
    return if (sdkInt >= 26 && !canRequestPackageInstalls) {
        AppUpdateInstallAction.OPEN_UNKNOWN_SOURCES_SETTINGS
    } else {
        AppUpdateInstallAction.START_INSTALL
    }
}
