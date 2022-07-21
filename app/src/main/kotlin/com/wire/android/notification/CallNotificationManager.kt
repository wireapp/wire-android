package com.wire.android.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.call.Call
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallNotificationManager @Inject constructor(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        appLogger.i("${TAG}: initialized")
    }

    fun handleNotifications(calls: List<Call>, userId: QualifiedID?) {
        if (calls.isEmpty() || userId == null) hideCallNotification()
        else showIncomingCallNotification(calls.first(), userId)
    }

    fun hideCallNotification() {
        appLogger.i("$TAG: hiding a call")
        notificationManager.cancel(NotificationConstants.CALL_NOTIFICATION_ID)
    }

    private fun showIncomingCallNotification(call: Call, userId: QualifiedID) {
        appLogger.i("$TAG: showing a call")
        createNotificationChannel()
        notificationManager.notify(NotificationConstants.CALL_NOTIFICATION_ID, getNotification(call, userId))
    }

    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannelCompat
            .Builder(NotificationConstants.CALL_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_MAX)
            .setName(NotificationConstants.CALL_CHANNEL_NAME)
            .build()

        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun getNotification(call: Call, userId: QualifiedID): Notification {
        val conversationIdString = call.conversationId.toString()
        val userIdString = userId.toString()
        val title = getNotificationTitle(call)
        val content = getNotificationBody(call)

        return NotificationCompat.Builder(context, NotificationConstants.CALL_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .addAction(getDeclineCallAction(conversationIdString, userIdString))
            .addAction(getOpenCallAction(conversationIdString))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setContentIntent(fullScreenCallPendingIntent(context, conversationIdString))
            .setFullScreenIntent(fullScreenCallPendingIntent(context, conversationIdString), true)
            .setDeleteIntent(declineCallPendingIntent(context, conversationIdString, userIdString))
            .setAutoCancel(true)
            .build()
    }

    private fun getNotificationBody(call: Call) =
        when (call.conversationType) {
            Conversation.Type.GROUP -> {
                val name = call.callerName ?: context.getString(R.string.notification_call_default_caller_name)
                (call.callerTeamName?.let { "$name @$it" } ?: name)
                    .let { context.getString(R.string.notification_group_call_content, it) }
            }
            else -> context.getString(R.string.notification_call_content)
        }

    private fun getNotificationTitle(call: Call): String =
        when (call.conversationType) {
            Conversation.Type.GROUP -> call.conversationName ?: context.getString(R.string.notification_call_default_group_name)
            else -> {
                val name = call.callerName ?: context.getString(R.string.notification_call_default_caller_name)
                call.callerTeamName?.let { "$name @$it" } ?: name
            }
        }

    private fun getOpenCallAction(conversationId: String) = getAction(
        context.getString(R.string.notification_action_open_call),
        openCallPendingIntent(context, conversationId)
    )

    private fun getDeclineCallAction(conversationId: String, userId: String) = getAction(
        context.getString(R.string.notification_action_decline_call),
        declineCallPendingIntent(context, conversationId, userId)
    )

    private fun getAction(title: String, intent: PendingIntent) = NotificationCompat.Action
        .Builder(null, title, intent)
        .build()

    companion object {
        private const val TAG = "CallNotificationManager"

        fun cancelNotification(context: Context) {
            NotificationManagerCompat.from(context).cancel(NotificationConstants.CALL_NOTIFICATION_ID)
        }
    }
}
