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
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.NoSession
import com.wire.android.notification.CallNotificationData
import com.wire.android.notification.CallNotificationManager
import com.wire.android.notification.NotificationIds
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMapRight
import com.wire.kalium.logic.functional.fold
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * Service that will be started when we have an outgoing/established call.
 */
@AndroidEntryPoint
class CallService : Service() {

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
                            val outgoingCallsFlow =
                                coreLogic.getSessionScope(userId).calls.observeOutgoingCall()
                            val establishedCallsFlow =
                                coreLogic.getSessionScope(userId).calls.establishedCall()

                            combine(
                                outgoingCallsFlow,
                                establishedCallsFlow
                            ) { outgoingCalls, establishedCalls ->
                                val calls = outgoingCalls + establishedCalls
                                calls.firstOrNull()?.let { call ->
                                    Either.Right(CallNotificationData(userId, call))
                                } ?: Either.Left("no calls")
                            }
                        } else {
                            flowOf(Either.Left("no valid current session"))
                        }
                    }
                    .distinctUntilChanged()
                    .flatMapRight { callData ->
                        callNotificationManager.reloadIfNeeded(callData)
                    }
                    .collectLatest {
                        it.fold(
                            { reason ->
                                appLogger.i("$TAG: stopSelf. Reason: $reason")
                                stopSelf()
                            },
                            { data ->
                                generateForegroundNotification(data)
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

    private fun generateForegroundNotification(data: CallNotificationData) {
        appLogger.i("$TAG: generating foregroundNotification...")
        val notification = if (data.callStatus == CallStatus.STARTED) {
            callNotificationManager.builder.getOutgoingCallNotification(data)
        } else {
            callNotificationManager.builder.getOngoingCallNotification(data)
        }
        ServiceCompat.startForeground(
            this,
            NotificationIds.CALL_OUTGOING_ONGOING_NOTIFICATION_ID.ordinal,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        appLogger.i("$TAG: started foreground with proper notification")
    }

    private fun generatePlaceholderForegroundNotification() {
        appLogger.i("$TAG: generating foregroundNotification placeholder...")
        val notification: Notification =
            callNotificationManager.builder.getCallServicePlaceholderNotification()
        ServiceCompat.startForeground(
            this,
            NotificationIds.CALL_OUTGOING_ONGOING_NOTIFICATION_ID.ordinal,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        appLogger.i("$TAG: started foreground with placeholder notification")
    }

    companion object {
        private const val TAG = "CallService"
        private const val EXTRA_STOP_SERVICE = "stop_service"

        fun newIntent(context: Context): Intent = Intent(context, CallService::class.java)

        fun newIntentToStop(context: Context): Intent =
            Intent(context, CallService::class.java).apply {
                putExtra(EXTRA_STOP_SERVICE, true)
            }

        var serviceState: AtomicReference<ServiceState> = AtomicReference(ServiceState.NOT_STARTED)
            private set
    }

    enum class ServiceState {
        NOT_STARTED, STARTED, FOREGROUND
    }
}
