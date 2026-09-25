package com.android.purebilibili.feature.settings.webdav

internal const val WEBDAV_AUTO_BACKUP_INTERVAL_HOURS = 24L
internal const val WEBDAV_AUTO_BACKUP_FLEX_HOURS = 2L

internal fun shouldScheduleWebDavAutoBackup(config: WebDavBackupConfig): Boolean {
    return config.enabled &&
        normalizeWebDavBaseUrl(config.baseUrl).isNotBlank() &&
        config.username.trim().isNotBlank() &&
        config.password.isNotBlank()
}
