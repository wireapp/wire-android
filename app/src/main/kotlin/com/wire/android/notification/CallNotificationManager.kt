package com.wire.android.notification

import android.app.Notification
import android.content.Context
import android.os.Build
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wire.android.R
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.call.Call
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class
)
@Singleton
class CallNotificationManager @Inject constructor(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    fun handleNotifications(calls: List<Call>, userId: QualifiedID?) {
        if (calls.isEmpty() || userId == null) hideCallNotification()
        else showIncomingCallNotification(calls.first(), userId)
    }

    fun hideCallNotification() = notificationManager.cancel(NOTIFICATION_ID)

    private fun showIncomingCallNotification(call: Call, userId: QualifiedID) {
        createNotificationChannelIfNeeded()

        notificationManager.notify(NOTIFICATION_ID, getNotification(call, userId))
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannelCompat
                .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_MAX)
                .setName(CHANNEL_NAME)
                .build()

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun getNotification(call: Call, userId: QualifiedID): Notification {
        val conversationIdString = call.conversationId.toString()
        val userIdString = userId.toString()
        val title = getNotificationTitle(call)
        val content = getNotificationBody(call)

        return getCompatNotification(title, content, conversationIdString, userIdString)
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

    private fun getCompatNotification(
        title: String,
        content: String,
        conversationIdString: String,
        userIdString: String
    ) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(content)
        .addAction(getDeclineCallCompatAction(conversationIdString, userIdString))
        .addAction(getOpenCallCompatAction(conversationIdString))
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setSmallIcon(R.drawable.notification_icon_small)
        .setContentIntent(fullScreenCallPendingIntent(context, conversationIdString))
        .setFullScreenIntent(fullScreenCallPendingIntent(context, conversationIdString), true)
        .setAutoCancel(true)
        .build()

    private fun getOpenCallAction(conversationId: String) = Notification.Action.Builder(
        null,
        context.getString(R.string.notification_action_open_call),
        openCallPendingIntent(context, conversationId)
    )
        .build()

    private fun getDeclineCallAction(conversationId: String, userId: String) = Notification.Action.Builder(
        null,
        context.getString(R.string.notification_action_decline_call),
        declineCallPendingIntent(context, conversationId, userId)
    )
        .build()

    private fun getOpenCallCompatAction(conversationId: String) = NotificationCompat.Action.Builder(
        null,
        context.getString(R.string.notification_action_open_call),
        openCallPendingIntent(context, conversationId)
    )
        .build()

    private fun getDeclineCallCompatAction(conversationId: String, userId: String) = NotificationCompat.Action.Builder(
        null,
        context.getString(R.string.notification_action_decline_call),
        declineCallPendingIntent(context, conversationId, userId)
    )
        .build()

    companion object {
        private const val CHANNEL_ID = "com.wire.android.notification_call_channel"
        private const val CHANNEL_NAME = "Call Channel"
        private const val NOTIFICATION_ID = 0

        fun cancelNotification(context: Context) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }
}
