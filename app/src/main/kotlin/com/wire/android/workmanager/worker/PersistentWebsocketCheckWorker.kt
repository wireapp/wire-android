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
@file:Suppress("StringTemplate")

package com.wire.android.workmanager.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.feature.StartPersistentWebsocketIfNecessaryUseCase
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.NotificationConstants
import com.wire.android.notification.NotificationIds
import com.wire.android.notification.openAppPendingIntent
import com.wire.android.workmanager.worker.PersistentWebsocketCheckWorker.Companion.NAME
import com.wire.android.workmanager.worker.PersistentWebsocketCheckWorker.Companion.TAG
import com.wire.android.workmanager.worker.PersistentWebsocketCheckWorker.Companion.WORK_INTERVAL
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

@HiltWorker
class PersistentWebsocketCheckWorker
@AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val startPersistentWebsocketIfNecessary: StartPersistentWebsocketIfNecessaryUseCase,
    private val notificationChannelsManager: NotificationChannelsManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        appLogger.i("${TAG}: Starting periodic work check for persistent websocket connection")
        startPersistentWebsocketIfNecessary()
        Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        notificationChannelsManager.createRegularChannel(
            NotificationConstants.OTHER_CHANNEL_ID,
            NotificationConstants.OTHER_CHANNEL_NAME
        )

        val title = "${applicationContext.getString(R.string.app_name)} " +
                applicationContext.getString(R.string.settings_service_is_running)
        val notification = NotificationCompat.Builder(applicationContext, NotificationConstants.OTHER_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openAppPendingIntent(applicationContext))
            .build()

        return ForegroundInfo(NotificationIds.PERSISTENT_CHECK_NOTIFICATION_ID.ordinal, notification)
    }

    companion object {
        const val NAME = "wss_check_worker"
        const val TAG = "PersistentWebsocketCheckWorker"
        val WORK_INTERVAL = 24.hours.toJavaDuration()
    }
}

fun WorkManager.enqueuePeriodicPersistentWebsocketCheckWorker() {
    appLogger.i("${TAG}: Enqueueing periodic work for $TAG")
    enqueueUniquePeriodicWork(
        NAME,
        ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
        PeriodicWorkRequestBuilder<PersistentWebsocketCheckWorker>(WORK_INTERVAL)
            .addTag(TAG) // adds the tag so we can cancel later all related work.
            .build()
    )
}

fun WorkManager.cancelPeriodicPersistentWebsocketCheckWorker() {
    appLogger.i("${TAG}: Cancelling all periodic scheduled work for the tag $TAG")
    cancelAllWorkByTag(TAG)
}
