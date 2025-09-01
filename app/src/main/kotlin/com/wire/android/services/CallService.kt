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
import com.wire.android.services.CallService.Action
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.logIfEmptyUserName
import com.wire.kalium.common.functional.Either
import com.wire.kalium.common.functional.fold
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.CallsScope
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.hilt.android.AndroidEntryPoint
import dev.ahmedmourad.bundlizer.Bundlizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
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
        val action = intent?.getActionTypeExtra(EXTRA_ACTION_TYPE)
        generatePlaceholderForegroundNotification()
        serviceState.set(ServiceState.FOREGROUND)
        if (action is Action.Stop) {
            appLogger.i("$TAG: stopSelf. Reason: stopService was called")
            stopSelf()
        } else {
            scope.launch {
                if (action is Action.AnswerCall) {
                    coreLogic.getSessionScope(action.userId).calls.answerCall(action.conversationId)
                }
                coreLogic.getGlobalScope().session.currentSessionFlow()
                    .flatMapLatest {
                        if (it is CurrentSessionResult.Success && it.accountInfo.isValid()) {
                            val userId = it.accountInfo.userId
                            val userSessionScope = coreLogic.getSessionScope(userId)
                            val outgoingCallsFlow = userSessionScope.calls.observeOutgoingCall()
                            val establishedCallsFlow = userSessionScope.calls.establishedCall()
                            val callCurrentlyBeingAnsweredFlow = userSessionScope.calls.observeCallCurrentlyBeingAnswered(action)

                            combine(
                                outgoingCallsFlow,
                                establishedCallsFlow,
                                callCurrentlyBeingAnsweredFlow
                            ) { outgoingCalls, establishedCalls, answeringCall ->
                                val calls = outgoingCalls + establishedCalls + answeringCall
                                calls.firstOrNull()?.let { call ->
                                    val userName = userSessionScope.users.observeSelfUser().first()
                                        .also { it.logIfEmptyUserName() }
                                        .let { it.handle ?: it.name ?: "" }
                                    Either.Right(CallNotificationData(userId, call, userName))
                                } ?: Either.Left("no calls")
                            }
                        } else {
                            flowOf(Either.Left("no valid current session"))
                        }
                    }
                    .distinctUntilChanged()
                    .debounce {
                        if (it is Either.Left) ServicesManager.DEBOUNCE_TIME else 0L
                    }
                    .collectLatest {
                        it.fold(
                            { reason ->
                                appLogger.i("$TAG: stopSelf. Reason: $reason")
                                stopSelf()
                            },
                            { data ->
                                updateForegroundNotification(data)
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

    private fun updateForegroundNotification(data: CallNotificationData) {
        // Updating service notification when notifications are disabled (or background work restricted)
        // causes app crash when putting app in background
//        if (callNotificationManager.areServiceNotificationsEnabled()) {
//            appLogger.e("$data")
//            val notification = if (data.callStatus == CallStatus.STARTED) {
//                callNotificationManager.builder.getOutgoingCallNotification(data)
//            } else {
//                callNotificationManager.builder.getOngoingCallNotification(data)
//            }
//
//            callNotificationManager.showNotification(NotificationIds.CALL_OUTGOING_ONGOING_NOTIFICATION_ID, notification)
//        }
    }

    private fun generatePlaceholderForegroundNotification() {
        appLogger.i("$TAG: generating foregroundNotification placeholder...")
        val notification: Notification =
            callNotificationManager.builder.getCallServicePlaceholderNotification()
        ServiceCompat.startForeground(
            this,
            NotificationIds.PERSISTENT_NOTIFICATION_ID.ordinal,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        appLogger.i("$TAG: started foreground with placeholder notification")
    }

    private suspend fun CallsScope.observeCallCurrentlyBeingAnswered(action: Action?) = when (action) {
        is Action.AnswerCall -> getIncomingCalls().map { it.filter { it.conversationId == action.conversationId } }
        else -> flowOf(emptyList())
    }

    companion object {
        private const val TAG = "CallService"
        private const val EXTRA_ACTION_TYPE = "action_type"

        fun newIntent(context: Context, actionType: Action = Action.Default): Intent = Intent(context, CallService::class.java)
            .putExtra(EXTRA_ACTION_TYPE, actionType)

        val serviceState: AtomicReference<ServiceState> = AtomicReference(ServiceState.NOT_STARTED)
    }

    enum class ServiceState {
        NOT_STARTED, STARTED, FOREGROUND
    }

    @Serializable
    sealed class Action {
        @Serializable
        data object Default : Action()

        @Serializable
        data class AnswerCall(val userId: UserId, val conversationId: ConversationId) : Action()

        @Serializable
        data object Stop : Action()
    }
}

private fun Intent.putExtra(name: String, actionType: Action): Intent = putExtra(name, Bundlizer.bundle(Action.serializer(), actionType))

private fun Intent.getActionTypeExtra(name: String): Action? = getBundleExtra(name)?.let {
        Bundlizer.unbundle(Action.serializer(), it)
    }
