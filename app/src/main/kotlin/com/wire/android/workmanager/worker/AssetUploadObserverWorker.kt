/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wire.android.R
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.NotificationConstants
import com.wire.android.notification.NotificationIds
import com.wire.android.notification.openAppPendingIntent
import com.wire.android.session.AppUserSessionPreparationResult
import com.wire.android.session.UserSessionPreparationGate
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * A Worker that observes asset uploads and only completes when there are no uploads in progress.
 * This is required to let the network operations running when the app is in the background.
 */
class AssetUploadObserverWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val coreLogic: CoreLogic,
    private val notificationChannelsManager: NotificationChannelsManager,
    private val userSessionPreparationGate: UserSessionPreparationGate,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        // Wait until there are no uploads in progress
        // switching to other user will cancel the observer and stop the worker
        return when (
            coreLogic.getGlobalScope().session.currentSessionFlow()
                .filterIsInstance<CurrentSessionResult.Success>()
                .map { it.accountInfo.userId }
                .waitForUploadCompletion()
        ) {
            UploadWaitResult.Complete -> Result.success()
            UploadWaitResult.Retry -> Result.retry()
            UploadWaitResult.Failure -> Result.failure()
        }
    }

    private suspend fun Flow<UserId>.waitForUploadCompletion(): UploadWaitResult =
        flatMapLatest { userId ->
            when (
                val preparation = userSessionPreparationGate.prepare(userId)
            ) {
                is AppUserSessionPreparationResult.Ready -> preparation.sessionScope.messages.observeAssetUploadState()
                    .map { uploadInProgress -> UploadState.InProgress(uploadInProgress) }
                is AppUserSessionPreparationResult.Failed -> kotlinx.coroutines.flow.flowOf(
                    UploadState.PreparationFailed(preparation.canRetry)
                )
            }
        }.first { state -> state !is UploadState.InProgress || !state.value }
            .let { state ->
                when (state) {
                    is UploadState.InProgress -> UploadWaitResult.Complete
                    is UploadState.PreparationFailed -> if (state.canRetry) UploadWaitResult.Retry else UploadWaitResult.Failure
                }
            }

    private sealed interface UploadState {
        data class InProgress(val value: Boolean) : UploadState
        data class PreparationFailed(val canRetry: Boolean) : UploadState
    }

    private enum class UploadWaitResult { Complete, Retry, Failure }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        notificationChannelsManager.createRegularChannel(
            NotificationConstants.OTHER_CHANNEL_ID,
            NotificationConstants.OTHER_CHANNEL_NAME
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationConstants.OTHER_CHANNEL_ID)
            .setSmallIcon(com.wire.android.feature.notification.R.drawable.notification_icon_small)
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentTitle(applicationContext.getString(R.string.notification_uploading_files))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setProgress(0, 0, true)
            .setContentIntent(openAppPendingIntent(applicationContext))
            .build()

        return ForegroundInfo(NotificationIds.UPLOADING_DATA_NOTIFICATION_ID.ordinal, notification)
    }
}

fun WorkManager.enqueueAssetUploadObserver() {
    val workerName = "asset_upload_observer_worker"
    val request = OneTimeWorkRequestBuilder<AssetUploadObserverWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .build()

    enqueueUniqueWork(
        workerName,
        // using APPEND_OR_REPLACE to avoid race condition between finishing and starting new worker
        ExistingWorkPolicy.APPEND_OR_REPLACE,
        request
    )
}
