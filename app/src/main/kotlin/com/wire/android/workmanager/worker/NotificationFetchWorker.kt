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
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.wire.android.R
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.NotificationConstants
import com.wire.android.notification.NotificationIds
import com.wire.android.notification.WireNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NotificationFetchWorker
@AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wireNotificationManager: WireNotificationManager,
    private val notificationChannelsManager: NotificationChannelsManager
) : CoroutineWorker(appContext, workerParams) {
    companion object {
        const val USER_ID_INPUT_DATA = "worker_user_id_input_data"
        const val WORK_NAME_PREFIX_PER_USER = "message-sync-"
    }

    override suspend fun doWork(): Result {
        inputData.getString(USER_ID_INPUT_DATA)?.let { userId ->
            wireNotificationManager.fetchAndShowNotificationsOnce(userId)
        }

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {

        notificationChannelsManager.createRegularChannel(
            NotificationConstants.MESSAGE_SYNC_CHANNEL_ID,
            NotificationConstants.MESSAGE_SYNC_CHANNEL_NAME
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationConstants.MESSAGE_SYNC_CHANNEL_ID)
            .setSmallIcon(com.wire.android.feature.notification.R.drawable.notification_icon_small)
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setProgress(0, 0, true)
            .setContentTitle(applicationContext.getString(R.string.label_fetching_your_messages))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        return ForegroundInfo(NotificationIds.MESSAGE_SYNC_NOTIFICATION_ID.ordinal, notification)
    }
}
