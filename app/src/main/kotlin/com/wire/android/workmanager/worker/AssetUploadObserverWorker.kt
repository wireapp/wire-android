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
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.feature.UserSessionScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * A Worker that observes asset uploads and only completes when there are no uploads in progress.
 * This is required to let the network operations running when the app is in the background.
 *
 * It covers both message attachment drafts and Shared Drive direct uploads. The worker never uploads
 * anything itself: the transfers keep running in their own session-scoped coroutines, and this only
 * holds the process at foreground priority until they are all done.
 */
class AssetUploadObserverWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val coreLogic: CoreLogic,
    private val notificationChannelsManager: NotificationChannelsManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        // Wait until no logged-in account has an upload in progress. Deliberately not scoped to just the
        // current session: switching to an account with nothing uploading must not let this finish (and
        // release foreground priority) while a different logged-in account's upload is still running.
        coreLogic.getGlobalScope().observeValidAccounts()
            .flatMapLatest { accounts -> accounts.anyUploadInProgress() }
            .first { uploadInProgress -> !uploadInProgress }

        return Result.success()
    }

    private suspend fun List<Pair<SelfUser, Team?>>.anyUploadInProgress(): Flow<Boolean> {
        if (isEmpty()) return flowOf(false)
        val perAccountFlows = mutableListOf<Flow<Boolean>>()
        for ((selfUser, _) in this) {
            perAccountFlows += coreLogic.getSessionScope(selfUser.id).observeAnyUploadInProgress()
        }
        return combine(perAccountFlows) { states -> states.any { it } }
    }

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

/**
 * Emits true while either a message attachment draft or a Shared Drive direct upload is still transferring.
 *
 * Both kinds of upload keep the process alive through the same worker, so they are observed as one signal.
 */
internal suspend fun UserSessionScope.observeAnyUploadInProgress(): Flow<Boolean> =
    combine(
        messages.observeAssetUploadState(),
        cells.uploadCoordinator.hasActiveUploads,
    ) { attachmentUploadInProgress, driveUploadInProgress ->
        attachmentUploadInProgress || driveUploadInProgress
    }.distinctUntilChanged()

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
