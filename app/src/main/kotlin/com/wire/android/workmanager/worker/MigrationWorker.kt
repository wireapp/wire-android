package com.wire.android.workmanager.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.asFlow
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest.MIN_BACKOFF_MILLIS
import androidx.work.WorkerParameters
import com.wire.android.R
import com.wire.android.migration.MigrationManager
import com.wire.android.migration.MigrationResult
import com.wire.android.migration.getMigrationFailure
import com.wire.android.migration.toData
import com.wire.android.notification.NotificationConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

@HiltWorker
class MigrationWorker
@AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val migrationManager: MigrationManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = migrationManager.migrate().let {
        when (it) {
            MigrationResult.Success -> Result.success()
            is MigrationResult.Failure -> Result.failure(it.type.toData())
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, NotificationConstants.OTHER_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setProgress(0, 0, true)
            .setContentTitle(applicationContext.getString(R.string.migration_title))
            .setContentText(applicationContext.getString(R.string.migration_message))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        return ForegroundInfo(NotificationConstants.MIGRATION_NOTIFICATION_ID, notification)
    }

    companion object {
        const val NAME = "migration"
    }
}

fun WorkManager.enqueueMigrationWorker(): Flow<MigrationResult> {
    val request = OneTimeWorkRequestBuilder<MigrationWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
        .build()
    enqueueUniqueWork(MigrationWorker.NAME, ExistingWorkPolicy.KEEP, request)
    return getWorkInfosForUniqueWorkLiveData(MigrationWorker.NAME).asFlow().map {
        it.first().let { workInfo ->
            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> MigrationResult.Success
                WorkInfo.State.FAILED -> MigrationResult.Failure(workInfo.outputData.getMigrationFailure())
                else -> null
            }
        }
    }.filterNotNull()
}
