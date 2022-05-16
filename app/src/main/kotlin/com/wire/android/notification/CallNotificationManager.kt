package com.wire.android.notification

import android.app.Notification
import android.content.Context
import android.os.Build
import com.wire.kalium.logic.feature.call.Call
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wire.android.R
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
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

    fun handleNotifications(calls: List<Call>, userId: QualifiedID?, ongoing: Boolean = true) {
        println("cyka handleCalls : $ongoing ${calls.size}")
        if (calls.isEmpty() || userId == null) hideCallNotification()
        else showIncomingCallNotification(calls.first(), userId, ongoing)
    }

    fun hideCallNotification() = notificationManager.cancel(NOTIFICATION_ID)

    private fun showIncomingCallNotification(call: Call, userId: QualifiedID, ongoing: Boolean) {
        createNotificationChannelIfNeeded()

        notificationManager.notify(NOTIFICATION_ID, getNotification(call, userId, ongoing))
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

    private fun getNotification(call: Call, userId: QualifiedID, ongoing: Boolean): Notification {
        val conversationIdString = call.conversationId.toString()
        val userIdString = userId.toString()
        val title = getNotificationTitle(call)
        val content = getNotificationBody(call)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getCompatNotification(title, content, conversationIdString, userIdString, ongoing)
        } else {
            getOldNotification(title, content, conversationIdString, userIdString, ongoing)
        }
    }

    private fun getNotificationBody(call: Call) =
        when (call.conversationDetails) {
            is ConversationDetails.Group -> {
                val name = call.caller?.name ?: "Someone"
                (call.callerTeam?.name?.let { "$name @$it" } ?: name)
                    .let { context.getString(R.string.notification_group_call_content, it) }
            }
            else -> context.getString(R.string.notification_call_content)
        }

    private fun getNotificationTitle(call: Call): String =
        when (call.conversationDetails) {
            is ConversationDetails.Group -> call.conversationDetails.conversation.name ?: "Somewhere"
            else -> {
                val name = call.caller?.name ?: "Someone"
                call.callerTeam?.name?.let { "$name @$it" } ?: name
            }
        }

    private fun getOldNotification(
        title: String,
        content: String,
        conversationIdString: String,
        userIdString: String,
        ongoing: Boolean
    ) = Notification.Builder(context)
        .setContentTitle(title)
        .setContentText(content)
        .setSmallIcon(R.drawable.notification_icon_small)
        .setActions(getDeclineCallAction(conversationIdString, userIdString))
        .setActions(getOpenCallAction(conversationIdString))
        .setPriority(Notification.PRIORITY_MAX)
        .setOngoing(ongoing)
        .setContentIntent(fullScreenCallPendingIntent(context, conversationIdString))
        .setFullScreenIntent(fullScreenCallPendingIntent(context, conversationIdString), true)
        .setAutoCancel(true)
        .build()

    private fun getCompatNotification(
        title: String,
        content: String,
        conversationIdString: String,
        userIdString: String,
        ongoing: Boolean
    ) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(content)
        .addAction(getDeclineCallCompatAction(conversationIdString, userIdString))
        .addAction(getOpenCallCompatAction(conversationIdString))
        .setOngoing(ongoing)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
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
