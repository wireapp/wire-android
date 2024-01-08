/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.workmanager.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.asFlow
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest.Companion.MIN_BACKOFF_MILLIS
import androidx.work.WorkerParameters
import androidx.work.await
import com.wire.android.R
import com.wire.android.migration.MigrationData
import com.wire.android.migration.MigrationManager
import com.wire.android.migration.getMigrationFailure
import com.wire.android.migration.getMigrationProgress
import com.wire.android.migration.toData
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.NotificationConstants
import com.wire.android.notification.openAppPendingIntent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

@HiltWorker
class MigrationWorker
@AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val migrationManager: MigrationManager,
    private val notificationChannelsManager: NotificationChannelsManager
) : CoroutineWorker(appContext, workerParams) {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result = coroutineScope {
        when (val result = migrationManager.migrate(this, { setProgress(it.type.toData()) })) {
            is MigrationData.Result.Success -> Result.success()
            is MigrationData.Result.Failure.NoNetwork -> Result.retry()
            is MigrationData.Result.Failure.Messages -> Result.failure(result.toData())
            is MigrationData.Result.Failure.Account -> Result.failure(result.toData())
            is MigrationData.Result.Failure.Unknown -> Result.failure(result.toData())
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {

        notificationChannelsManager.createRegularChannel(
            NotificationConstants.OTHER_CHANNEL_ID,
            NotificationConstants.OTHER_CHANNEL_NAME
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationConstants.OTHER_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setProgress(0, 0, true)
            .setContentTitle(applicationContext.getString(R.string.migration_title))
            .setContentText(applicationContext.getString(R.string.migration_message))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openAppPendingIntent(applicationContext))
            .build()

        return ForegroundInfo(NotificationConstants.MIGRATION_NOTIFICATION_ID, notification)
    }

    companion object {
        const val NAME = "migration"
    }
}

suspend fun WorkManager.enqueueMigrationWorker(): Flow<MigrationData> {
    val request = OneTimeWorkRequestBuilder<MigrationWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()
    val isAlreadyRunning = getWorkInfosForUniqueWork(MigrationWorker.NAME).await().let { it.firstOrNull()?.state == WorkInfo.State.RUNNING }
    enqueueUniqueWork(
        MigrationWorker.NAME,
        if (isAlreadyRunning) ExistingWorkPolicy.KEEP else ExistingWorkPolicy.REPLACE,
        request
    )
    return getWorkInfosForUniqueWorkLiveData(MigrationWorker.NAME).asFlow()
        .dropWhile { it.first().state == WorkInfo.State.ENQUEUED }
        .map {
            it.first().let { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> MigrationData.Result.Success
                    WorkInfo.State.FAILED -> workInfo.outputData.getMigrationFailure()
                    WorkInfo.State.CANCELLED -> workInfo.outputData.getMigrationFailure()
                    WorkInfo.State.RUNNING -> MigrationData.Progress(workInfo.progress.getMigrationProgress())
                    WorkInfo.State.ENQUEUED -> workInfo.outputData.getMigrationFailure()
                    WorkInfo.State.BLOCKED -> null
                }
            }
        }.filterNotNull()
}
