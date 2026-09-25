package com.android.purebilibili.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

internal fun installDownloadedAppUpdate(
    context: Context,
    apkFile: File
): AppUpdateInstallAction {
    require(apkFile.exists()) { "APK 文件不存在: ${apkFile.absolutePath}" }

    val canRequestPackageInstalls = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.packageManager.canRequestPackageInstalls()
    } else {
        true
    }
    val installAction = resolveAppUpdateInstallAction(
        sdkInt = Build.VERSION.SDK_INT,
        canRequestPackageInstalls = canRequestPackageInstalls
    )

    if (installAction == AppUpdateInstallAction.OPEN_UNKNOWN_SOURCES_SETTINGS) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return installAction
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
    return installAction
}
