package com.wire.android.notification

import android.app.Notification
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
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
@Suppress("TooManyFunctions")
class CallNotificationManager @Inject constructor(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val soundUri by lazy { Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/ringing_from_them") }

    init {
        appLogger.i("${TAG}: initialized")
    }

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
        createIncomingCallsNotificationChannel()
        val notification = getIncomingCallNotification(call, userId)
        notificationManager.notify(NotificationConstants.CALL_INCOMING_NOTIFICATION_ID, notification)
    }

    // Channels
    private fun createIncomingCallsNotificationChannel() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(getAudioAttributeUsageByOsLevel())
            .build()

        val notificationChannel = NotificationChannelCompat
            .Builder(NotificationConstants.INCOMING_CALL_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(NotificationConstants.INCOMING_CALL_CHANNEL_NAME)
            .setSound(soundUri, audioAttributes)
            .setShowBadge(false)
            .setVibrationEnabled(true)
            .build()

        notificationManager.createNotificationChannel(notificationChannel)
    }

    /**
     * Tricky bug: No documentation whatsoever, but these values affect how the system cancels or not the vibration of the notification
     * on different Android OS levels, probably channel creation related.
     */
    private fun getAudioAttributeUsageByOsLevel() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AudioAttributes.USAGE_NOTIFICATION_RINGTONE else AudioAttributes.USAGE_MEDIA

    fun createOngoingNotificationChannel() {
        val notificationChannel = NotificationChannelCompat
            .Builder(NotificationConstants.ONGOING_CALL_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_MAX)
            .setName(NotificationConstants.ONGOING_CALL_CHANNEL_NAME)
            .setVibrationEnabled(false)
            .setImportance(NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setSound(null, null)
            .build()

        notificationManager.createNotificationChannel(notificationChannel)
    }

    // Notifications
    private fun getIncomingCallNotification(call: Call, userId: QualifiedID): Notification {
        val conversationIdString = call.conversationId.toString()
        val userIdString = userId.toString()
        val title = getNotificationTitle(call)
        val content = getNotificationBody(call)

        val notification = NotificationCompat.Builder(context, NotificationConstants.INCOMING_CALL_CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setOngoing(true)
            .setTimeoutAfter(INCOMING_CALL_TIMEOUT)
            .setFullScreenIntent(fullScreenIncomingCallPendingIntent(context, conversationIdString), true)
            .addAction(getDeclineCallAction(context, conversationIdString, userIdString))
            .addAction(getOpenIncomingCallAction(context, conversationIdString))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreenIncomingCallPendingIntent(context, conversationIdString))
            .setDeleteIntent(declineCallPendingIntent(context, conversationIdString, userIdString))
            .build()

        // Added FLAG_INSISTENT so the ringing sound repeats itself until an action is done.
        notification.flags += Notification.FLAG_INSISTENT

        return notification
    }

    fun getOngoingCallNotification(callName: String, conversationId: String, userId: String): Notification =
        NotificationCompat.Builder(context, NotificationConstants.ONGOING_CALL_CHANNEL_ID)
            .setContentTitle(callName)
            .setContentText(context.getString(R.string.notification_ongoing_call_content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setAutoCancel(true)
            .setOngoing(true)
            .addAction(getHangUpCallAction(context, conversationId, userId))
            .addAction(getOpenOngoingCallAction(context, conversationId))
            .setContentIntent(openOngoingCallPendingIntent(context, conversationId))
            .build()

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

        fun hideIncomingCallNotification(context: Context) {
            NotificationManagerCompat.from(context).cancel(NotificationConstants.CALL_INCOMING_NOTIFICATION_ID)
        }
    }
}
