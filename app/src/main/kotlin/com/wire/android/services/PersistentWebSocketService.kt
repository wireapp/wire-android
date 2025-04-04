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

package com.wire.android.services

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.NotificationConstants.WEB_SOCKET_CHANNEL_ID
import com.wire.android.notification.NotificationConstants.WEB_SOCKET_CHANNEL_NAME
import com.wire.android.notification.NotificationIds
import com.wire.android.notification.WireNotificationManager
import com.wire.android.notification.openAppPendingIntent
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PersistentWebSocketService : Service() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private val scope by lazy {
        CoroutineScope(SupervisorJob() + dispatcherProvider.io())
    }

    @Inject
    lateinit var notificationManager: WireNotificationManager

    @Inject
    lateinit var notificationChannelsManager: NotificationChannelsManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        isServiceStarted = true
        generateForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /**
         * When service is restarted by system onCreate lifecycle method is not guaranteed to be called
         * so we need to check if service is already started and if not generate notification and call startForeground()
         * https://issuetracker.google.com/issues/307329994#comment100
         */
        if (!isServiceStarted) {
            isServiceStarted = true
            generateForegroundNotification()
        }
        scope.launch {
            coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus().let { result ->
                when (result) {
                    is ObservePersistentWebSocketConnectionStatusUseCase.Result.Failure -> {
                        appLogger.e("Failure while fetching persistent web socket status flow from service")
                    }

                    is ObservePersistentWebSocketConnectionStatusUseCase.Result.Success -> {
                        result.persistentWebSocketStatusListFlow.collectLatest { statuses ->
                            val usersToObserve = statuses
                                .filter { it.isPersistentWebSocketEnabled }
                                .map { it.userId }
                            notificationManager.observeNotificationsAndCallsPersistently(
                                usersToObserve,
                                scope
                            )
                            keepRunningSyncForUsers(usersToObserve)
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    private suspend fun keepRunningSyncForUsers(usersToObserve: List<UserId>) {
        coroutineScope {
            usersToObserve.forEach { userId ->
                launch {
                    coreLogic.getSessionScope(userId).syncExecutor.request {
                        awaitCancellation()
                    }
                }
            }
        }
    }

    private fun generateForegroundNotification() {
        notificationChannelsManager.createRegularChannel(WEB_SOCKET_CHANNEL_ID, WEB_SOCKET_CHANNEL_NAME)

        val notification: Notification = NotificationCompat.Builder(this, WEB_SOCKET_CHANNEL_ID)
            .setContentTitle("${resources.getString(R.string.app_name)} ${resources.getString(R.string.settings_service_is_running)}")
            .setSmallIcon(R.drawable.websocket_notification_icon_small)
            .setContentIntent(openAppPendingIntent(this))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                ServiceCompat.startForeground(
                    this,
                    NotificationIds.PERSISTENT_NOTIFICATION_ID.ordinal,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } catch (e: ForegroundServiceStartNotAllowedException) {
                // ForegroundServiceStartNotAllowedException may be thrown on restarting service from the background.
                // this is the only suggested workaround from google for now.
                // https://issuetracker.google.com/issues/307329994#comment86
                appLogger.e("Failure while starting foreground: $e")
                stopSelf()
            }
        } else {
            ServiceCompat.startForeground(
                this,
                NotificationIds.PERSISTENT_NOTIFICATION_ID.ordinal,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel("PersistentWebSocketService was destroyed")
        isServiceStarted = false
    }

    companion object {
        fun newIntent(context: Context?): Intent =
            Intent(context, PersistentWebSocketService::class.java)

        var isServiceStarted = false
    }
}
