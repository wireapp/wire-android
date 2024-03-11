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

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.NoSession
import com.wire.android.notification.CallNotificationManager
import com.wire.android.notification.NotificationConstants.CALL_ONGOING_NOTIFICATION_ID
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.fold
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@AndroidEntryPoint
class OngoingCallService : Service() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var callNotificationManager: CallNotificationManager

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    @NoSession
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    private val scope by lazy {
        // There's no UI, no need to run anything using the Main/UI Dispatcher
        CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    }

    override fun onCreate() {
        serviceState.set(ServiceState.STARTED)
        super.onCreate()
        generatePlaceholderForegroundNotification()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        appLogger.i("$TAG: onStartCommand")
        val stopService = intent?.getBooleanExtra(EXTRA_STOP_SERVICE, false)
        generatePlaceholderForegroundNotification()
        serviceState.set(ServiceState.FOREGROUND)
        if (stopService == true) {
            appLogger.i("$TAG: stopSelf. Reason: stopService was called")
            stopSelf()
        } else {
            scope.launch {
                coreLogic.getGlobalScope().session.currentSessionFlow()
                    .flatMapLatest {
                        if (it is CurrentSessionResult.Success && it.accountInfo.isValid()) {
                            val userId = it.accountInfo.userId
                            coreLogic.getSessionScope(userId).calls.establishedCall().map {
                                it.firstOrNull()?.let { call ->
                                    Either.Right(
                                        OngoingCallData(
                                            userId = userId,
                                            conversationId = call.conversationId,
                                            notificationTitle = callNotificationManager.builder.getNotificationTitle(call)
                                        )
                                    )
                                } ?: Either.Left("no ongoing calls")
                            }
                        } else {
                            flowOf(Either.Left("no valid current session"))
                        }
                    }
                    .collectLatest {
                        it.fold(
                            { reason ->
                                appLogger.i("$TAG: stopSelf. Reason: $reason")
                                stopSelf()
                            }, {
                                generateForegroundNotification(it.notificationTitle, it.conversationId.toString(), it.userId)
                            }
                        )
                    }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        appLogger.i("$TAG: onDestroy")
        serviceState.set(ServiceState.NOT_STARTED)
        super.onDestroy()
        scope.cancel()
    }

    private fun generateForegroundNotification(
        callName: String,
        conversationId: String,
        userId: UserId
    ) {
        appLogger.i("$TAG: generating foregroundNotification...")
        val notification: Notification = callNotificationManager.builder.getOngoingCallNotification(
            callName,
            conversationId,
            userId
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                CALL_ONGOING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(CALL_ONGOING_NOTIFICATION_ID, notification)
        }
        appLogger.i("$TAG: started foreground with proper notification")
    }

    private fun generatePlaceholderForegroundNotification() {
        appLogger.i("$TAG: generating foregroundNotification placeholder...")
        val notification: Notification =
            callNotificationManager.builder.getOngoingCallPlaceholderNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                CALL_ONGOING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(CALL_ONGOING_NOTIFICATION_ID, notification)
        }
        appLogger.i("$TAG: started foreground with placeholder notification")
    }

    companion object {
        private const val TAG = "OngoingCallService"
        private const val EXTRA_STOP_SERVICE = "stop_service"

        fun newIntent(context: Context): Intent = Intent(context, OngoingCallService::class.java)

        fun newIntentToStop(context: Context): Intent =
            Intent(context, OngoingCallService::class.java).apply {
                putExtra(EXTRA_STOP_SERVICE, true)
            }

        var serviceState: AtomicReference<ServiceState> = AtomicReference(ServiceState.NOT_STARTED)
            private set
    }

    enum class ServiceState {
        NOT_STARTED, STARTED, FOREGROUND
    }
}

data class OngoingCallData(
    val userId: UserId,
    val conversationId: ConversationId,
    val notificationTitle: String
)
