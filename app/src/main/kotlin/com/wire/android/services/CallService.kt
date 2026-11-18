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

import android.Manifest.permission.RECORD_AUDIO
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.wire.android.appLogger
import com.wire.android.notification.CallNotificationData
import com.wire.android.notification.CallNotificationManager
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.NotificationIds
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.android.services.CallService.Action
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.common.functional.fold
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import dev.ahmedmourad.bundlizer.Bundlizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import dev.zacsweers.metro.Inject

/**
 * Service that will be started when we have an outgoing/established call.
 */
class CallService : Service() {

    @Inject
    lateinit var lifecycleManager: CallServiceManager

    @Inject
    lateinit var callNotificationManager: CallNotificationManager

    @Inject
    lateinit var notificationChannelsManager: NotificationChannelsManager

    @Inject
    lateinit var currentScreenManager: CurrentScreenManager

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private val scope by lazy {
        // There's no UI, no need to run anything using the Main/UI Dispatcher
        CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    }

    override fun onCreate() {
        wireApplicationGraph.inject(this)
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
        if (!startForegroundWithPlaceholderNotification(action)) {
            stopSelf()
            return START_NOT_STICKY
        }
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

    /**
     * Starts the service in the foreground and returns whether it succeeded.
     *
     * The microphone type is only requested when starting a call with the RECORD_AUDIO permission granted. Since Android 14,
     * the system rejects it when the app isn't allowed to use the microphone at that moment (e.g. not visible), so in that case
     * we fall back to the phone call type only instead of crashing.
     */
    private fun startForegroundWithPlaceholderNotification(action: Action): Boolean {
        appLogger.i("$TAG: generating foregroundNotification placeholder...")
        notificationChannelsManager.createOngoingCallNotificationChannel()
        val notification: Notification = callNotificationManager.builder.getCallServicePlaceholderNotification()
        val hasRecordAudioPermission = ContextCompat.checkSelfPermission(this, RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val isAppVisible = currentScreenManager.isAppVisibleFlow().value
        val requestMicrophone = action is Action.Start && hasRecordAudioPermission
        val diagnostics = "action: ${action::class.simpleName}, RECORD_AUDIO permission: $hasRecordAudioPermission, " +
                "isAppVisible: $isAppVisible"

        val started = if (requestMicrophone) {
            tryStartForeground(notification, PHONE_CALL_AND_MICROPHONE_TYPE, diagnostics) ||
                tryStartForeground(notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL, diagnostics)
        } else {
            // Without the permission, or when only stopping, the microphone is not needed, and the phone call type
            // doesn't require the app to be in the foreground.
            tryStartForeground(notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL, diagnostics)
        }

        if (!started) {
            appLogger.e("$TAG: Unable to start foreground service with any type, stopping. $diagnostics")
            return false
        }

        appLogger.i("$TAG: started foreground with placeholder notification, isAppVisible:$isAppVisible")
        if (!hasRecordAudioPermission) {
            scope.launch {
                appLogger.i("$TAG: Stopping service started without RECORD_AUDIO permission")
                lifecycleManager.handleAction(Action.Stop)
            }
        }
        return true
    }

    private fun tryStartForeground(notification: Notification, foregroundServiceType: Int, diagnostics: String): Boolean = try {
        ServiceCompat.startForeground(
            this,
            NotificationIds.CALL_OUTGOING_ONGOING_NOTIFICATION_ID.ordinal,
            notification,
            foregroundServiceType
        )
        true
    } catch (e: SecurityException) {
        appLogger.w("$TAG: startForeground rejected for type $foregroundServiceType, $diagnostics, reason: ${e.message}")
        false
    }

    companion object {
        private const val TAG = "CallService"
        private const val EXTRA_ACTION_TYPE = "action_type"
        private const val PHONE_CALL_AND_MICROPHONE_TYPE =
            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE

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
