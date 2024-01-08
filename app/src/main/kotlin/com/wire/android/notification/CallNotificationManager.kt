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

package com.wire.android.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class CallNotificationManager @Inject constructor(
    context: Context,
    dispatcherProvider: DispatcherProvider,
    val builder: CallNotificationBuilder,
) {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private val incomingCallsForUsers = MutableStateFlow<List<Pair<UserId, Call>>>(emptyList())

    init {
        scope.launch {
            incomingCallsForUsers
                .debounce { if (it.isEmpty()) 0L else DEBOUNCE_TIME } // debounce to avoid showing and hiding notification too fast
                .distinctUntilChanged()
                .collectLatest {
                    if (it.isEmpty()) {
                        hideIncomingCallNotification()
                    } else {
                        it.first().let { (userId, call) ->
                            appLogger.i("$TAG: showing incoming call")
                            showIncomingCallNotification(call, userId)
                        }
                    }
                }
        }
    }

    fun handleIncomingCallNotifications(calls: List<Call>, userId: UserId) {
        if (calls.isEmpty()) {
            incomingCallsForUsers.update { it.filter { it.first != userId } }
        } else {
            incomingCallsForUsers.update { it.filter { it.first != userId } + (userId to calls.first()) }
        }
    }

    fun hideAllNotifications() {
        hideIncomingCallNotification()
    }

    fun hideIncomingCallNotification() {
        appLogger.i("$TAG: hiding incoming call")

        // This delay is just so when the user receives two calling signals one straight after the other [INCOMING -> CANCEL]
        // Due to the signals being one after the other we are creating a notification when we are trying to cancel it, it wasn't properly
        // cancelling vibration as probably when we were cancelling, the vibration object was still being created and started and thus
        // never stopped.
        TimeUnit.MILLISECONDS.sleep(CANCEL_CALL_NOTIFICATION_DELAY)
        notificationManager.cancel(NotificationConstants.CALL_INCOMING_NOTIFICATION_ID)
    }

    @VisibleForTesting
    internal fun showIncomingCallNotification(call: Call, userId: QualifiedID) {
        appLogger.i("$TAG: showing incoming call for user ${userId.toLogString()}")
        val notification = builder.getIncomingCallNotification(call, userId)
        notificationManager.notify(NotificationConstants.CALL_INCOMING_NOTIFICATION_ID, notification)
    }

    // Notifications

    companion object {
        private const val TAG = "CallNotificationManager"
        private const val CANCEL_CALL_NOTIFICATION_DELAY = 300L
        @VisibleForTesting
        internal const val DEBOUNCE_TIME = 200L

        fun hideIncomingCallNotification(context: Context) {
            NotificationManagerCompat.from(context).cancel(NotificationConstants.CALL_INCOMING_NOTIFICATION_ID)
        }
    }
}

@Singleton
class CallNotificationBuilder @Inject constructor(
    private val context: Context,
) {
    fun getIncomingCallNotification(call: Call, userId: QualifiedID): Notification {
        val conversationIdString = call.conversationId.toString()
        val userIdString = userId.toString()
        val title = getNotificationTitle(call)
        val content = getNotificationBody(call)
        val channelId = NotificationConstants.getIncomingChannelId(userId)

        val notification = NotificationCompat.Builder(context, channelId)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVibrate(VIBRATE_PATTERN)
            .setTimeoutAfter(INCOMING_CALL_TIMEOUT)
            .setFullScreenIntent(fullScreenIncomingCallPendingIntent(context, conversationIdString, userIdString), true)
            .addAction(getDeclineCallAction(context, conversationIdString, userIdString))
            .addAction(getOpenIncomingCallAction(context, conversationIdString, userIdString))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreenIncomingCallPendingIntent(context, conversationIdString, userIdString))
            .setDeleteIntent(declineCallPendingIntent(context, conversationIdString, userIdString))
            .build()

        // Added FLAG_INSISTENT so the ringing sound repeats itself until an action is done.
        notification.flags += Notification.FLAG_INSISTENT

        return notification
    }

    fun getOngoingCallNotification(callName: String, conversationId: String, userId: UserId): Notification {
        val channelId = NotificationConstants.ONGOING_CALL_CHANNEL_ID
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(callName)
            .setContentText(context.getString(R.string.notification_ongoing_call_content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setAutoCancel(true)
            .setOngoing(true)
            .addAction(getHangUpCallAction(context, conversationId, userId.toString()))
            .addAction(getOpenOngoingCallAction(context, conversationId))
            .setContentIntent(openOngoingCallPendingIntent(context, conversationId))
            .build()
    }

    /**
     * @return placeholder Notification for OngoingCall, that can be shown immediately after starting the Service
     * (e.g. in [android.app.Service.onCreate]). It has no any [NotificationCompat.Action], on click - just opens the app.
     * This notification should be replace by the user-specific notification (with corresponding [NotificationCompat.Action],
     * [android.content.Intent] and title) once it's possible (e.g. in [android.app.Service.onStartCommand])
     */
    fun getOngoingCallPlaceholderNotification(): Notification {
        val channelId = NotificationConstants.ONGOING_CALL_CHANNEL_ID
        return NotificationCompat.Builder(context, channelId)
            .setContentText(context.getString(R.string.notification_ongoing_call_content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent(context))
            .build()
    }

    // Notifications content
    private fun getNotificationBody(call: Call) =
        when (call.conversationType) {
            Conversation.Type.GROUP -> {
                val name = call.callerName ?: context.getString(R.string.notification_call_default_caller_name)
                (call.callerTeamName?.let { "$name @$it" } ?: name)
                    .let { context.getString(R.string.notification_group_call_content, it) }
            }

            else -> context.getString(R.string.notification_incoming_call_content)
        }

    fun getNotificationTitle(call: Call): String =
        when (call.conversationType) {
            Conversation.Type.GROUP -> call.conversationName ?: context.getString(R.string.notification_call_default_group_name)
            else -> {
                val name = call.callerName ?: context.getString(R.string.notification_call_default_caller_name)
                call.callerTeamName?.let { "$name @$it" } ?: name
            }
        }

    companion object {
        private const val INCOMING_CALL_TIMEOUT: Long = 30 * 1000
        private val VIBRATE_PATTERN = longArrayOf(0, 1000, 1000)
    }
}
