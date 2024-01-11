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
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest.Companion.MIN_BACKOFF_MILLIS
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.workDataOf
import com.wire.android.R
import com.wire.android.migration.MigrationData
import com.wire.android.migration.MigrationManager
import com.wire.android.migration.getMigrationFailure
import com.wire.android.migration.getMigrationProgress
import com.wire.android.migration.toData
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.NotificationConstants
import com.wire.android.notification.openAppPendingIntent
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import com.wire.kalium.logic.data.user.UserId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

@HiltWorker
class SingleUserMigrationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val migrationManager: MigrationManager,
    private val notificationChannelsManager: NotificationChannelsManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        val userId = inputData.getString(USER_ID_INPUT_DATA)?.let { userId ->
            QualifiedIdMapperImpl(null).fromStringToQualifiedID(userId)
        } ?: return@coroutineScope Result.failure()

        when (val result = migrationManager.migrateSingleUser(userId, this) { setProgress(it.type.toData()) }) {
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

        return ForegroundInfo(NotificationConstants.SINGLE_USER_MIGRATION_NOTIFICATION_ID, notification)
    }

    companion object {
        const val NAME = "single_user_migration"
        const val USER_ID_INPUT_DATA = "single_user_migration_user_id"
    }
}

suspend fun WorkManager.enqueueSingleUserMigrationWorker(userId: UserId): Flow<MigrationData> {
    val request = OneTimeWorkRequestBuilder<SingleUserMigrationWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
        .setInputData(workDataOf(SingleUserMigrationWorker.USER_ID_INPUT_DATA to userId.toString()))
        .build()
    val isAlreadyRunning =
        getWorkInfosForUniqueWork(SingleUserMigrationWorker.NAME).await().let { it.firstOrNull()?.state == WorkInfo.State.RUNNING }
    enqueueUniqueWork(
        SingleUserMigrationWorker.NAME,
        if (isAlreadyRunning) ExistingWorkPolicy.KEEP else ExistingWorkPolicy.REPLACE,
        request
    )
    return getWorkInfosForUniqueWorkLiveData(SingleUserMigrationWorker.NAME).asFlow().map {
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
