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

package com.wire.android.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.Call
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class CallNotificationManager @Inject constructor(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    fun handleIncomingCallNotifications(calls: List<Call>, userId: QualifiedID?) {
        if (calls.isEmpty() || userId == null) {
            hideIncomingCallNotification()
        } else {
            appLogger.i("$TAG: showing incoming call")
            showIncomingCallNotification(calls.first(), userId)
        }
    }

    fun hideAllNotifications() {
        hideIncomingCallNotification()
    }

    fun hideIncomingCallNotification() {
        appLogger.i("$TAG: hiding incoming call")
        notificationManager.cancel(NotificationConstants.CALL_INCOMING_NOTIFICATION_ID)
    }

    private fun showIncomingCallNotification(call: Call, userId: QualifiedID) {
        val notification = getIncomingCallNotification(call, userId)
        notificationManager.notify(NotificationConstants.CALL_INCOMING_NOTIFICATION_ID, notification)
    }

    // Notifications
    private fun getIncomingCallNotification(call: Call, userId: QualifiedID): Notification {
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
        val channelId = NotificationConstants.getOngoingChannelId(userId)
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(callName)
            .setContentText(context.getString(R.string.notification_ongoing_call_content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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
        private const val TAG = "CallNotificationManager"
        private const val INCOMING_CALL_TIMEOUT: Long = 30 * 1000
        private val VIBRATE_PATTERN = longArrayOf(0, 1000, 1000)

        fun hideIncomingCallNotification(context: Context) {
            NotificationManagerCompat.from(context).cancel(NotificationConstants.CALL_INCOMING_NOTIFICATION_ID)
        }
    }
}
