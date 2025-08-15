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
import com.wire.android.notification.CallNotificationData
import com.wire.android.notification.CallNotificationManager
import com.wire.android.notification.NotificationIds
import com.wire.android.services.CallService.Action
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.common.functional.fold
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.AndroidEntryPoint
import dev.ahmedmourad.bundlizer.Bundlizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

/**
 * Service that will be started when we have an outgoing/established call.
 */
@AndroidEntryPoint
class CallService : Service() {

    @Inject
    lateinit var lifecycleManager: CallServiceManager

    @Inject
    lateinit var callNotificationManager: CallNotificationManager

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private val scope by lazy {
        // There's no UI, no need to run anything using the Main/UI Dispatcher
        CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    }

    override fun onCreate() {
        _serviceState.value = ServiceState.STARTED
        super.onCreate()
        handleActions()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        appLogger.i("$TAG: onStartCommand")
        val action = intent?.getActionTypeExtra(EXTRA_ACTION_TYPE) ?: Action.Start.Default
        generatePlaceholderForegroundNotification()
        _serviceState.value = ServiceState.FOREGROUND
        scope.launch {
            lifecycleManager.handleAction(action)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        appLogger.i("$TAG: onDestroy")
        _serviceState.value = ServiceState.NOT_STARTED
        super.onDestroy()
        scope.cancel()
    }

    private fun handleActions() {
        scope.launch {
            lifecycleManager.handleActionsFlow().collectLatest {
                it.fold({ reason ->
                    appLogger.i("$TAG: stopSelf. Reason: ${reason.message}")
                    stopSelf()
                }, { data ->
                    updateForegroundNotification(data)
                })
            }
        }
    }

    private fun updateForegroundNotification(data: CallNotificationData) {
        // Updating service notification when notifications are disabled (or background work restricted)
        // causes app crash when putting app in background
        if (callNotificationManager.areServiceNotificationsEnabled()) {

            val notification = if (data.callStatus == CallStatus.STARTED) {
                callNotificationManager.builder.getOutgoingCallNotification(data)
            } else {
                callNotificationManager.builder.getOngoingCallNotification(data)
            }

            callNotificationManager.showNotification(NotificationIds.CALL_OUTGOING_ONGOING_NOTIFICATION_ID, notification)
        }
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
        private const val EXTRA_ACTION_TYPE = "action_type"

        fun newIntent(context: Context, actionType: Action = Action.Start.Default): Intent = Intent(context, CallService::class.java)
            .putExtra(EXTRA_ACTION_TYPE, actionType)

        private val _serviceState: MutableStateFlow<ServiceState> = MutableStateFlow(ServiceState.NOT_STARTED)
        val serviceState: StateFlow<ServiceState> = _serviceState
    }

    enum class ServiceState {
        NOT_STARTED, STARTED, FOREGROUND
    }

    @Serializable
    sealed interface Action {
        @Serializable
        sealed interface Start : Action {

            @Serializable
            data object Default : Start

            @Serializable
            data class AnswerCall(val userId: UserId, val conversationId: ConversationId) : Start
        }

        @Serializable
        data object Stop : Action
    }
}

private fun Intent.putExtra(name: String, actionType: Action): Intent = putExtra(name, Bundlizer.bundle(Action.serializer(), actionType))

private fun Intent.getActionTypeExtra(name: String): Action? = getBundleExtra(name)?.let {
    Bundlizer.unbundle(Action.serializer(), it)
}
