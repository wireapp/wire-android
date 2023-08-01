/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.services

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.NoSession
import com.wire.android.notification.CallNotificationManager
import com.wire.android.notification.NotificationConstants.CALL_ONGOING_NOTIFICATION_ID
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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

    private var currentObserveEstablishedCallJob: Pair<UserId, Job>? = null

    private val scope by lazy {
        // There's no UI, no need to run anything using the Main/UI Dispatcher
        CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    }

    override fun onCreate() {
        super.onCreate()
        generatePlaceholderForegroundNotification()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        appLogger.i("$TAG: onStartCommand")
        val userIdString = intent?.getStringExtra(EXTRA_USER_ID)
        val conversationIdString = intent?.getStringExtra(EXTRA_CONVERSATION_ID)
        val callName = intent?.getStringExtra(EXTRA_CALL_NAME)
        val stopService = intent?.getBooleanExtra(EXTRA_STOP_SERVICE, false)
        generatePlaceholderForegroundNotification()
        when {
            stopService == true -> {
                appLogger.i("$TAG: stopSelf. Reason: stopService was called")
                stopSelf()
            }

            userIdString == null || conversationIdString == null || callName == null -> {
                appLogger.w(
                    "$TAG: stopSelf. Reason: some of the parameter is absent. " +
                            "userIdString: ${userIdString?.obfuscateId()}, " +
                            "conversationIdString: ${conversationIdString?.obfuscateId()}, " +
                            "callName: $callName"
                )
                stopSelf()
            }

            else -> {
                val userId = qualifiedIdMapper.fromStringToQualifiedID(userIdString)
                generateForegroundNotification(callName, conversationIdString, userId)
                currentObserveEstablishedCallJob?.let { (jobUserId, job) ->
                    if (userId != jobUserId) { // if previous job was for different user, cancel it
                        if (job.isActive) job.cancel()
                        currentObserveEstablishedCallJob = null
                    }
                }
                if (currentObserveEstablishedCallJob == null) { // if there is no job active, start a new one
                    currentObserveEstablishedCallJob = userId to scope.launch {
                        if (coreLogic.getGlobalScope().doesValidSessionExist(userId).doesValidSessionExist()) {
                            coreLogic.getSessionScope(userId).calls.establishedCall().collect { establishedCall ->
                                if (establishedCall.isEmpty()) {
                                    appLogger.i("$TAG: stopSelf. Reason: call was ended")
                                    stopSelf()
                                }
                            }
                        } else {
                            appLogger.i("$TAG: stopSelf. Reason: session was ended")
                            stopSelf()
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        appLogger.i("$TAG: onDestroy")
        super.onDestroy()
        scope.cancel()
    }

    private fun generateForegroundNotification(callName: String, conversationId: String, userId: UserId) {
        appLogger.i("$TAG: generating foregroundNotification...")
        val notification: Notification = callNotificationManager.getOngoingCallNotification(callName, conversationId, userId)
        startForeground(CALL_ONGOING_NOTIFICATION_ID, notification)
        appLogger.i("$TAG: started foreground with proper notification")
    }

    private fun generatePlaceholderForegroundNotification() {
        appLogger.i("$TAG: generating foregroundNotification placeholder...")
        val notification: Notification = callNotificationManager.getOngoingCallPlaceholderNotification()
        startForeground(CALL_ONGOING_NOTIFICATION_ID, notification)
        appLogger.i("$TAG: started foreground with placeholder notification")
    }

    companion object {
        private const val TAG = "OngoingCallService"
        private const val EXTRA_USER_ID = "user_id_extra"
        private const val EXTRA_CONVERSATION_ID = "conversation_id_extra"
        private const val EXTRA_CALL_NAME = "call_name_extra"
        private const val EXTRA_STOP_SERVICE = "stop_service"

        fun newIntent(context: Context, ongoingCallData: OngoingCallData): Intent =
            Intent(context, OngoingCallService::class.java).apply {
                putExtra(EXTRA_USER_ID, ongoingCallData.userId.toString())
                putExtra(EXTRA_CONVERSATION_ID, ongoingCallData.conversationId.toString())
                putExtra(EXTRA_CALL_NAME, ongoingCallData.notificationTitle)
            }

        fun newIntentToStop(context: Context): Intent =
            Intent(context, OngoingCallService::class.java).apply {
                putExtra(EXTRA_STOP_SERVICE, true)
            }
    }
}

data class OngoingCallData(
    val userId: UserId,
    val conversationId: ConversationId,
    val notificationTitle: String
)
