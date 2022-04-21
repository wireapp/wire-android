package com.wire.android.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.style.StyleSpan
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import androidx.core.text.toSpannable
import com.wire.android.R
import com.wire.android.ui.WireActivity
import com.wire.android.util.toBitmap
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.asString
import com.wire.kalium.logic.data.notification.LocalNotificationConversation
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class
)
@Singleton
class MessageNotificationManager @Inject constructor(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    fun handleNotification(oldData: List<LocalNotificationConversation>, newData: List<LocalNotificationConversation>) {
        val oldConversationIds = oldData.map { it.id }
        val newConversationIds = newData.map { it.id }

        val conversationIdsToRemove = oldConversationIds.filter { !newConversationIds.contains(it) }
        val conversationsToAdd = newData
            .filter { conversation ->
                val oldConversation = oldData.firstOrNull { it.id == conversation.id }
                oldConversation == null || oldConversation != conversation
            }
            .map { NotificationConversation.fromDbData(it) }
            .sortedBy { it.lastMessageTime }

        createNotificationChannelIfNeeded()
        showSummaryIfNeeded(oldData, newData)
        conversationIdsToRemove.forEach { hideNotification(it) }
        conversationsToAdd.forEach { showConversationNotification(it) }
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

    private fun showSummaryIfNeeded(oldData: List<LocalNotificationConversation>, newData: List<LocalNotificationConversation>) {
        if (oldData.size > 1 || newData.size <= 1) return

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(getPendingIntentSummary())
            .setDeleteIntent(getDismissPendingIntent(null))
            .build()

        notificationManager.notify(SUMMARY_ID, summaryNotification)
    }

    private fun showConversationNotification(conversation: NotificationConversation) {
        notificationManager.notify(getNotificationId(conversation.id), getConversationNotification(conversation))
    }

    private fun hideNotification(conversationsId: ConversationId) = notificationManager.cancel(getNotificationId(conversationsId))

    private fun getConversationNotification(conversation: NotificationConversation) =
        NotificationCompat.Builder(context.applicationContext, CHANNEL_ID).apply {
            setDefaults(NotificationCompat.DEFAULT_ALL)

            priority = NotificationCompat.PRIORITY_MAX

            setSmallIcon(R.drawable.notification_icon_small)
            setGroup(GROUP_KEY)
            setAutoCancel(true)

            setContentIntent(getPendingIntentMessage(conversation.id))
            setDeleteIntent(getDismissPendingIntent(conversation.id))
            addAction(getActionCall(conversation.id))
            addAction(getActionReply(conversation.id))

            setWhen(conversation.lastMessageTime)

            setLargeIcon(conversation.image?.toBitmap())

            setStyle(getMessageStyle(conversation))
        }.build()

    private fun getMessageStyle(conversation: NotificationConversation): NotificationCompat.Style {
        val receiver = Person.Builder()
            .setName(context.getString(R.string.notification_receiver_name))
            .build()

        val notificationStyle = NotificationCompat.MessagingStyle(receiver)

        notificationStyle.conversationTitle = if (conversation.isOneToOneConversation) null else conversation.name

        conversation.messages.forEach { messageData ->
            val sender = Person.Builder()
                .apply {
                    setName(messageData.author.name)
                    messageData.author.image?.toBitmap()?.let {
                        setIcon(IconCompat.createWithAdaptiveBitmap(it))
                    }
                }
                .build()

            val message = when (messageData) {
                is NotificationMessage.Text -> messageData.text
                is NotificationMessage.Comment -> {
                    context.getString(messageData.textResId.value).toSpannable()
                        .apply { setSpan(StyleSpan(Typeface.ITALIC), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
                }
            }

            val notificationMessage = NotificationCompat.MessagingStyle.Message(message, messageData.time, sender)

            notificationStyle.addMessage(notificationMessage)
        }

        return notificationStyle
    }

    private fun getActionReply(conversationId: String): NotificationCompat.Action {
        val resultPendingIntent = PendingIntent.getBroadcast(
            context,
            conversationId.hashCode(),
            NotificationReplyReceiver.newIntent(context, conversationId),
            PendingIntent.FLAG_MUTABLE
        )

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).build()

        return NotificationCompat.Action.Builder(null, context.getString(R.string.notification_action_reply), resultPendingIntent)
            .addRemoteInput(remoteInput)
            .build()
    }

    private fun getActionCall(conversationId: String) = NotificationCompat.Action(
        null,
        context.getString(R.string.notification_action_call),
        getPendingIntentCall(conversationId)
    )

    //TODO
    private fun getPendingIntentMessage(conversationId: String): PendingIntent {
        return getPendingIntentSummary()
    }

    private fun getDismissPendingIntent(conversationId: String?): PendingIntent {
        val intent = NotificationDismissReceiver.newIntent(context, conversationId)
        val requestCode = conversationId?.hashCode() ?: DISMISS_DEFAULT_REQUEST_CODE

        return PendingIntent.getBroadcast(
            context.applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    //TODO
    private fun getPendingIntentCall(conversationId: String): PendingIntent = getPendingIntentSummary()

    private fun getPendingIntentSummary(): PendingIntent {
        val intent = Intent(context.applicationContext, WireActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            context.applicationContext,
            SUMMARY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getNotificationId(id: ConversationId) = getNotificationId(id.asString())
    private fun getNotificationId(id: String) = id.hashCode()

    companion object {
        private const val CHANNEL_ID = "com.wire.android.notification_channel"
        private const val CHANNEL_NAME = "Messages Channel"
        private const val GROUP_KEY = "wire_reloaded_notification_group"
        private const val SUMMARY_ID = 0
        private const val SUMMARY_REQUEST_CODE = 0
        private const val DISMISS_DEFAULT_REQUEST_CODE = 1

        const val KEY_TEXT_REPLY = "key_text_notification_reply"

        fun cancelNotification(context: Context, notificationId: Int) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }
}
