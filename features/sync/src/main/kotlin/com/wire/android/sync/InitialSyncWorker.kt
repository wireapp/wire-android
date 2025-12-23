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
package com.wire.android.sync

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.sync.R
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.NotificationConstants
import com.wire.android.notification.NotificationIds
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.work.Work
import com.wire.kalium.work.WorkId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import com.wire.android.feature.notification.R as NR

@HiltWorker
class InitialSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val notificationChannelsManager: NotificationChannelsManager,
) : CoroutineWorker(context, parameters) {

    private val workId: WorkId? = parameters.getWorkId()

    override suspend fun doWork(): Result = if (workId == null) {
        Log.e("InitialSyncWorker", "WorkId is null, cannot start monitoring work.")
        Result.failure()
    } else {
        Log.i("InitialSyncWorker", "Starting InitialSyncWorker.")
        val result = coreLogic.globalScope {
            session.allSessions()
        }
        if (result !is GetAllSessionsResult.Success) {
            Log.e("InitialSyncWorker", "Failure to get active sessions. Not waiting for Sync.")
            Result.failure()
        } else {
            coroutineScope {
                result.sessions.forEach { session ->
                    launch {
                        coreLogic.sessionScope(session.userId) {
                            syncExecutor.request {
                                Log.i("InitialSyncWorker", "Waiting for Initial Sync for user '${session.userId}' to finish.")
                                longWork.observeWorkStatus(workId).takeWhile {
                                    it !is Work.Status.Complete
                                }.collect()
                                Log.i("InitialSyncWorker", "Initial Sync for user '${session.userId}' complete.")
                            }
                        }
                    }
                }
            }
            Log.i("InitialSyncWorker", "Initial Sync complete for all users")
            Result.success()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        notificationChannelsManager.createRegularChannel(
            NotificationConstants.OTHER_CHANNEL_ID,
            NotificationConstants.OTHER_CHANNEL_NAME
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationConstants.OTHER_CHANNEL_ID)
            .setSmallIcon(NR.drawable.notification_icon_small)
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentTitle(applicationContext.getString(R.string.notification_setting_up_wire))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setProgress(0, 0, true)
            .build()

        return ForegroundInfo(NotificationIds.UPLOADING_DATA_NOTIFICATION_ID.ordinal, notification)
    }

    companion object {
        private const val WORK_ID_KEY = "workId"
        private fun WorkerParameters.getWorkId(): WorkId? = inputData.getString(WORK_ID_KEY)?.let { WorkId(it) }

        fun createInputData(workId: WorkId) = Data.Builder()
            .putString(WORK_ID_KEY, workId.id)
            .build()
    }
}
