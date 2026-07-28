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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.NotificationConstants
import com.wire.android.notification.NotificationIds
import com.wire.android.notification.WireNotificationManager
import com.wire.android.ui.WireActivity
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.startup.StartupFailure
import com.wire.kalium.logic.startup.StartupResult
import com.wire.kalium.logic.startup.StartupState
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class NotificationFetchWorker
@AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wireNotificationManager: WireNotificationManager,
    private val notificationChannelsManager: NotificationChannelsManager,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
) : CoroutineWorker(appContext, workerParams) {
    companion object {
        const val USER_ID_INPUT_DATA = "worker_user_id_input_data"
        const val WORK_NAME_PREFIX_PER_USER = "message-sync-"
        private val FOREGROUND_NOTIFICATION_DELAY = 5.seconds
    }

    private sealed interface UserResolution {
        data class Found(val userId: UserId) : UserResolution
        data object Retry : UserResolution
        data object Ignore : UserResolution
    }

    override suspend fun doWork(): Result {
        val userIdValue = inputData.getString(USER_ID_INPUT_DATA)
            ?.takeIf { it.isNotBlank() }
            ?: return Result.success()

        return when (val resolution = resolveUser(userIdValue)) {
            is UserResolution.Found -> fetchNotifications(userIdValue, resolution.userId)
            UserResolution.Retry -> Result.retry()
            UserResolution.Ignore -> Result.success()
        }
    }

    private suspend fun resolveUser(userIdValue: String): UserResolution =
        when (val sessions = coreLogic.getGlobalScope().getSessions()) {
            is GetAllSessionsResult.Success -> sessions.sessions
                .firstOrNull { it.userId.value == userIdValue }
                ?.let { UserResolution.Found(it.userId) }
                ?: UserResolution.Ignore

            is GetAllSessionsResult.Failure.Generic -> UserResolution.Retry
            GetAllSessionsResult.Failure.NoSessionFound -> UserResolution.Ignore
        }

    private suspend fun fetchNotifications(userIdValue: String, userId: UserId): Result =
        when (val sessionStartup = awaitSessionStartup(userId)) {
            is StartupResult.Success -> {
                wireNotificationManager.fetchAndShowNotificationsOnce(userIdValue)
                Result.success()
            }

            is StartupResult.Failure -> sessionStartup.failure.toWorkerResult()
        }

    private fun StartupFailure.toWorkerResult(): Result =
        if (isRetryable) Result.retry() else Result.failure()

    private suspend fun awaitSessionStartup(userId: UserId): StartupResult<UserSessionScope> = coroutineScope {
        val handle = coreLogic.startup.session(userId)
        val foregroundPromotion = launch {
            delay(FOREGROUND_NOTIFICATION_DELAY)
            if (handle.state.value is StartupState.Opening ||
                handle.state.value is StartupState.Migrating
            ) {
                try {
                    setForeground(createForegroundInfo(isMigrating = true))
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (exception: IllegalStateException) {
                    appLogger.w("Unable to promote database migration worker: ${exception.message}")
                }
            }
        }

        try {
            handle.open()
        } finally {
            foregroundPromotion.cancel()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        createForegroundInfo(isMigrating = false)

    private fun createForegroundInfo(isMigrating: Boolean): ForegroundInfo {
        notificationChannelsManager.createDatabaseMaintenanceChannel()

        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, WireActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationConstants.DATABASE_MAINTENANCE_CHANNEL_ID,
        )
            .setSmallIcon(com.wire.android.feature.notification.R.drawable.notification_icon_small)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setProgress(0, 0, true)
            .setContentTitle(
                applicationContext.getString(
                    if (isMigrating) R.string.migration_title else R.string.label_fetching_your_messages
                )
            )
            .setContentText(
                applicationContext.getString(
                    if (isMigrating) R.string.migration_message_unknown else R.string.label_fetching_your_messages
                )
            )
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        return ForegroundInfo(
            NotificationIds.MESSAGE_SYNC_NOTIFICATION_ID.ordinal,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }
}
