package com.android.purebilibili.feature.settings.webdav

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.util.concurrent.TimeUnit

class WebDavAutoBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val config = WebDavBackupStore.getConfig(applicationContext).first()
        if (!shouldScheduleWebDavAutoBackup(config)) {
            Logger.d(TAG, "Skip auto backup: WebDAV disabled or config incomplete")
            return Result.success()
        }

        val service = WebDavBackupService(applicationContext)
        return service.backupNow(config).fold(
            onSuccess = { entry ->
                Logger.d(TAG, "Auto backup success: ${entry.fileName}")
                Result.success()
            },
            onFailure = { error ->
                Logger.e(TAG, "Auto backup failed", error)
                if (error is IOException) Result.retry() else Result.failure()
            }
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "webdav_auto_backup"
        const val TAG = "webdav_auto_backup"
    }
}

object WebDavAutoBackupScheduler {

    fun sync(context: Context, config: WebDavBackupConfig) {
        if (!shouldScheduleWebDavAutoBackup(config)) {
            cancel(context)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<WebDavAutoBackupWorker>(
            WEBDAV_AUTO_BACKUP_INTERVAL_HOURS,
            TimeUnit.HOURS,
            WEBDAV_AUTO_BACKUP_FLEX_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(WebDavAutoBackupWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WebDavAutoBackupWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WebDavAutoBackupWorker.UNIQUE_WORK_NAME)
    }
}
