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
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.asString
import com.wire.kalium.logic.data.notification.LocalNotificationCall
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

    fun handleCalls(calls: List<LocalNotificationCall>, userId: QualifiedID) {
        if (calls.isEmpty()) hideCallNotification()
        else showIncomingCallNotification(calls.first(), userId)
    }

    private fun hideCallNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun showIncomingCallNotification(data: LocalNotificationCall, userId: QualifiedID) {
        createNotificationChannelIfNeeded()

        notificationManager.notify(NOTIFICATION_ID, getNotification(data, userId))
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

    private fun getNotification(data: LocalNotificationCall, userId: QualifiedID): Notification {
        val conversationIdString = data.conversationId.asString()
        val userIdString = userId.asString()
        val title = data.notificationTitle
        val content = data.notificationBody?.let { context.getString(R.string.notification_group_call_content, it) }
            ?: context.getString(R.string.notification_call_content)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getCompatNotification(title, content, conversationIdString, userIdString)
        } else {
            getOldNotification(title, content, conversationIdString, userIdString)
        }
    }

    private fun getOldNotification(
        title: String,
        content: String,
        conversationIdString: String,
        userIdString: String
    ) = Notification.Builder(context)
        .setContentTitle(title)
        .setContentText(content)
        .setSmallIcon(R.drawable.notification_icon_small)
        .setActions(getOpenCallAction(conversationIdString))
        .setActions(getDeclineCallAction(conversationIdString, userIdString))
        .setFullScreenIntent(fullScreenCallPendingIntent(context), true)
        .setAutoCancel(true)
        .setDeleteIntent(declineCallPendingIntent(context, conversationIdString, userIdString))
        .build()

    private fun getCompatNotification(
        title: String,
        content: String,
        conversationIdString: String,
        userIdString: String
    ) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(content)
        .addAction(getOpenCallCompatAction(conversationIdString))
        .addAction(getDeclineCallCompatAction(conversationIdString, userIdString))
        .setSmallIcon(R.drawable.notification_icon_small)
        .setFullScreenIntent(fullScreenCallPendingIntent(context), true)
        .setAutoCancel(true)
        .setDeleteIntent(declineCallPendingIntent(context, conversationIdString, userIdString))
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
