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

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.app.NotificationManagerCompat
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.notification.NotificationConstants.INCOMING_CALL_ID_PREFIX
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class CallNotificationManager @Inject constructor(
    private val context: Context,
    dispatcherProvider: DispatcherProvider,
    val builder: CallNotificationBuilder,
) {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private val incomingCallsForUsers = MutableStateFlow<Map<UserId, IncomingCallsForUser>>(mapOf())
    private val reloadCallNotification = MutableSharedFlow<CallNotificationIds>()

    init {
        scope.launch {
            incomingCallsForUsers
                .debounce { if (it.isEmpty()) 0L else DEBOUNCE_TIME } // debounce to avoid showing and hiding notification too fast
                .map {
                    it.values.map { (userId, userName, calls) ->
                        calls.map { call ->
                            CallNotificationData(userId, call, userName)
                        }
                    }.flatten()
                }
                .scan(emptyList<CallNotificationData>() to emptyList<CallNotificationData>()) { (previousCalls, _), currentCalls ->
                    currentCalls to (currentCalls - previousCalls.toSet())
                }
                .distinctUntilChanged()
                .flatMapLatest { (allCurrentCalls, newCalls) ->
                    reloadCallNotification
                        .map { (userIdString, conversationIdString) ->
                            allCurrentCalls to allCurrentCalls.filter { // emit call that needs to be reloaded as newOrUpdated
                                it.userId.toString() == userIdString && it.conversationId.toString() == conversationIdString
                            }
                        }
                        .filter { (_, newCalls) -> newCalls.isNotEmpty() } // only emit if there is something to reload
                        .onStart { emit(allCurrentCalls to newCalls) }
                }
                .collectLatest { (allCurrentCalls, newCalls) ->
                    // remove outdated incoming call notifications
                    hideOutdatedIncomingCallNotifications(allCurrentCalls)
                    // show current incoming call notifications
                    appLogger.i("$TAG: showing ${newCalls.size} new incoming calls (all incoming calls: ${allCurrentCalls.size})")
                    newCalls.forEach { data ->
                        showIncomingCallNotification(data)
                    }
                }
        }
    }

    @VisibleForTesting
    internal fun hideOutdatedIncomingCallNotifications(currentIncomingCalls: List<CallNotificationData>) {
        val currentIncomingCallNotificationIds = currentIncomingCalls.map {
            NotificationConstants.getIncomingCallId(it.userId.toString(), it.conversationId.toString())
        }
        hideIncomingCallNotifications { _, id -> !currentIncomingCallNotificationIds.contains(id) }
    }

    fun reloadCallNotifications(reloadCallNotificationIds: CallNotificationIds) = scope.launch {
        reloadCallNotification.emit(reloadCallNotificationIds)
    }

    fun handleIncomingCalls(calls: List<Call>, userId: UserId, userName: String) {
        if (calls.isEmpty()) {
            incomingCallsForUsers.update {
                it.minus(userId)
            }
        } else {
            incomingCallsForUsers.update {
                it.plus(userId to IncomingCallsForUser(userId, userName, calls))
            }
        }
    }

    private fun hideIncomingCallNotifications(predicate: (tag: String, id: Int) -> Boolean) {
        notificationManager.activeNotifications.filter {
            it.tag?.startsWith(INCOMING_CALL_ID_PREFIX) == true && predicate(it.tag, it.id)
        }.forEach {
            it.hideIncomingCallNotification()
        }
    }

    fun hideAllIncomingCallNotifications() = hideIncomingCallNotifications { _, _ -> true }

    fun hideAllIncomingCallNotificationsForUser(userId: UserId) =
        hideIncomingCallNotifications { tag, _ -> tag == NotificationConstants.getIncomingCallTag(userId.toString()) }

    fun hideIncomingCallNotification(userIdString: String, conversationIdString: String) =
        hideIncomingCallNotifications { _, id -> id == NotificationConstants.getIncomingCallId(userIdString, conversationIdString) }

    private fun StatusBarNotification.hideIncomingCallNotification() {
        appLogger.i("$TAG: hiding incoming call")

        // This delay is just so when the user receives two calling signals one straight after the other [INCOMING -> CANCEL]
        // Due to the signals being one after the other we are creating a notification when we are trying to cancel it, it wasn't
        // properly cancelling vibration as probably when we were cancelling, the vibration object was still being created and started
        // and thus never stopped.
        TimeUnit.MILLISECONDS.sleep(CANCEL_CALL_NOTIFICATION_DELAY)
        notificationManager.cancel(tag, id)
    }

    @SuppressLint("MissingPermission")
    @VisibleForTesting
    internal fun showIncomingCallNotification(data: CallNotificationData) {
        appLogger.i(
            "$TAG: showing incoming call notification for user ${data.userId.toLogString()}" +
                    " and conversation ${data.conversationId.toLogString()}"
        )
        val tag = NotificationConstants.getIncomingCallTag(data.userId.toString())
        val id = NotificationConstants.getIncomingCallId(data.userId.toString(), data.conversationId.toString())
        val notification = builder.getIncomingCallNotification(data)
        notificationManager.notify(tag, id, notification)
    }

    // Notifications

    companion object {
        private const val TAG = "CallNotificationManager"
        private const val CANCEL_CALL_NOTIFICATION_DELAY = 300L

        @VisibleForTesting
        internal const val DEBOUNCE_TIME = 200L
    }
}

@Singleton
class CallNotificationBuilder @Inject constructor(
    private val context: Context,
) {

    fun getOutgoingCallNotification(data: CallNotificationData): Notification {
        val userIdString = data.userId.toString()
        val conversationIdString = data.conversationId.toString()
        val channelId = NotificationConstants.getOutgoingChannelId(data.userId)

        return NotificationCompat.Builder(context, channelId)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setContentTitle(data.conversationName)
            .setContentText(context.getString(R.string.notification_outgoing_call_tap_to_return))
            .setSubText(data.userName)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE )
            .addAction(getHangUpCallAction(context, conversationIdString, userIdString))
            .setFullScreenIntent(outgoingCallPendingIntent(context, conversationIdString), true)
            .setContentIntent(outgoingCallPendingIntent(context, conversationIdString))
            .setDeleteIntent(callNotificationDismissedPendingIntent(context, userIdString, conversationIdString))
            .build()
    }

    fun getIncomingCallNotification(data: CallNotificationData): Notification {
        val conversationIdString = data.conversationId.toString()
        val userIdString = data.userId.toString()
        val title = getNotificationTitle(data)
        val content = getNotificationBody(data)
        val channelId = NotificationConstants.getIncomingChannelId(data.userId)

        val notification = NotificationCompat.Builder(context, channelId)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(data.userName)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVibrate(VIBRATE_PATTERN)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(getDeclineCallAction(context, conversationIdString, userIdString))
            .addAction(getOpenIncomingCallAction(context, conversationIdString, userIdString))
            .setFullScreenIntent(fullScreenIncomingCallPendingIntent(context, conversationIdString, userIdString), true)
            .setContentIntent(fullScreenIncomingCallPendingIntent(context, conversationIdString, userIdString))
            .setDeleteIntent(callNotificationDismissedPendingIntent(context, userIdString, conversationIdString))
            .build()

        // Added FLAG_INSISTENT so the ringing sound repeats itself until an action is done.
        notification.flags += Notification.FLAG_INSISTENT

        return notification
    }

    fun getOngoingCallNotification(data: CallNotificationData): Notification {
        val channelId = NotificationConstants.ONGOING_CALL_CHANNEL_ID
        val conversationIdString = data.conversationId.toString()
        val userIdString = data.userId.toString()
        val title = getNotificationTitle(data)

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notification_ongoing_call_content))
            .setSubText(data.userName)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setAutoCancel(true)
            .setOngoing(true)
            .setUsesChronometer(true)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(getHangUpCallAction(context, conversationIdString, userIdString))
            .addAction(getOpenOngoingCallAction(context, conversationIdString))
            .setFullScreenIntent(openOngoingCallPendingIntent(context, conversationIdString), true)
            .setContentIntent(openOngoingCallPendingIntent(context, conversationIdString))
            .setDeleteIntent(callNotificationDismissedPendingIntent(context, userIdString, conversationIdString))
            .build()
    }

    /**
     * @return placeholder Notification for CallService, that can be shown immediately after starting the Service
     * (e.g. in [android.app.Service.onCreate]). It has no any [NotificationCompat.Action], on click - just opens the app.
     * This notification should be replace by the user-specific notification (with corresponding [NotificationCompat.Action],
     * [android.content.Intent] and title) once it's possible (e.g. in [android.app.Service.onStartCommand])
     */
    fun getCallServicePlaceholderNotification(): Notification {
        val channelId = NotificationConstants.ONGOING_CALL_CHANNEL_ID
        return NotificationCompat.Builder(context, channelId)
            .setContentText(context.getString(R.string.notification_outgoing_call_tap_to_return))
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
    private fun getNotificationBody(data: CallNotificationData) =
        when (data.conversationType) {
            Conversation.Type.GROUP -> {
                val name = data.callerName ?: context.getString(R.string.notification_call_default_caller_name)
                (data.callerTeamName?.let { "$name @$it" } ?: name)
                    .let { context.getString(R.string.notification_group_call_content, it) }
            }

            else -> context.getString(R.string.notification_incoming_call_content)
        }

    fun getNotificationTitle(data: CallNotificationData): String =
        when (data.conversationType) {
            Conversation.Type.GROUP -> data.conversationName ?: context.getString(R.string.notification_call_default_group_name)
            else -> {
                val name = data.callerName ?: context.getString(R.string.notification_call_default_caller_name)
                data.callerTeamName?.let { "$name @$it" } ?: name
            }
        }

    companion object {
        private val VIBRATE_PATTERN = longArrayOf(0, 1000, 1000)
    }
}

data class IncomingCallsForUser(val userId: UserId, val userName: String, val incomingCalls: List<Call>)

data class CallNotificationIds(val userIdString: String, val conversationIdString: String)

data class CallNotificationData(
    val userId: QualifiedID,
    val userName: String,
    val conversationId: ConversationId,
    val conversationName: String?,
    val conversationType: Conversation.Type,
    val callerName: String?,
    val callerTeamName: String?,
    val callStatus: CallStatus
) {
    constructor(userId: UserId, call: Call, userName: String) : this(
        userId,
        userName,
        call.conversationId,
        call.conversationName,
        call.conversationType,
        call.callerName,
        call.callerTeamName,
        call.status
    )
}
