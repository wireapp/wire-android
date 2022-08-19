package com.wire.android.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.Typeface
import android.service.notification.StatusBarNotification
import android.text.Spannable
import android.text.style.StyleSpan
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import androidx.core.text.toSpannable
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.notification.NotificationConstants.getConversationNotificationId
import com.wire.android.util.toBitmap
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.notification.LocalNotificationConversation
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Singleton
class MessageNotificationManager @Inject constructor(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val oldNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun handleNotification(newNotifications: List<LocalNotificationConversation>, userId: QualifiedID?) {

        if (newNotifications.isEmpty()) return

        val activeNotifications: Array<StatusBarNotification> = oldNotificationManager.activeNotifications ?: arrayOf()
        val userIdString = userId?.toString()

        createNotificationChannel()
        showSummaryIfNeeded(activeNotifications)
        newNotifications.forEach {
            showConversationNotification(it.intoNotificationConversation(), userIdString, activeNotifications)
        }

        appLogger.i("$TAG: handled notifications: newNotifications size ${newNotifications.size}; ")
    }

    fun hideNotification(conversationsId: ConversationId) {
        val notificationId = getConversationNotificationId(conversationsId)

        if (isThereAnyOtherWireNotification(notificationId)) {
            notificationManager.cancel(notificationId)
        } else {
            hideAllNotifications()
        }
    }

    fun hideAllNotifications() {
        // removing groupSummary removes all the notifications in a group
        notificationManager.cancel(NotificationConstants.MESSAGE_SUMMARY_ID)
    }

    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannelCompat
            .Builder(NotificationConstants.MESSAGE_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_MAX)
            .setName(NotificationConstants.MESSAGE_CHANNEL_NAME)
            .build()

        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun showSummaryIfNeeded(activeNotifications: Array<StatusBarNotification>) {
        if (activeNotifications.find { it.id == NotificationConstants.MESSAGE_SUMMARY_ID } != null) return

        appLogger.i("$TAG adding groupSummary")
        notificationManager.notify(NotificationConstants.MESSAGE_SUMMARY_ID, getSummaryNotification())
    }

    private fun showConversationNotification(
        conversation: NotificationConversation,
        userId: String?,
        activeNotifications: Array<StatusBarNotification>
    ) {
        val notificationId = getConversationNotificationId(conversation.id)
        getConversationNotification(conversation, userId, activeNotifications)?.let { notification ->
            appLogger.i("$TAG adding ConversationNotification")
            notificationManager.notify(notificationId, notification)
        }
    }

    private fun getSummaryNotification() = NotificationCompat.Builder(context, NotificationConstants.MESSAGE_CHANNEL_ID)
        .setSmallIcon(R.drawable.notification_icon_small)
        .setGroup(NotificationConstants.MESSAGE_GROUP_KEY)
        .setGroupSummary(true)
        .setAutoCancel(true)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setContentIntent(summaryMessagePendingIntent(context))
        .build()

    /**
     * Create or update existed [Notification]
     * @return [Notification] for the conversation with all the messages in it (including the messages that been there before)
     * OR null if there is no new messages in conversation and no need to update the existed notification.
     */
    private fun getConversationNotification(
        conversation: NotificationConversation,
        userId: String?,
        activeNotifications: Array<StatusBarNotification>
    ): Notification? {

        val updatedMessageStyle = getUpdatedMessageStyle(conversation, activeNotifications) ?: return null

        return NotificationCompat.Builder(context, NotificationConstants.MESSAGE_CHANNEL_ID).apply {
            setDefaults(NotificationCompat.DEFAULT_ALL)

            priority = NotificationCompat.PRIORITY_MAX
            setCategory(NotificationCompat.CATEGORY_MESSAGE)

            setSmallIcon(R.drawable.notification_icon_small)
            setGroup(NotificationConstants.MESSAGE_GROUP_KEY)
            setAutoCancel(true)

            conversation.messages
                .firstOrNull()
                .let {
                    when (it) {
                        is NotificationMessage.ConnectionRequest -> {
                            setContentIntent(otherUserProfilePendingIntent(context, it.authorId))
                        }
                        is NotificationMessage.ConversationDeleted -> {
                            setContentIntent(openAppPendingIntent(context))
                        }
                        else -> {
                            setContentIntent(messagePendingIntent(context, conversation.id))
                            addAction(getActionCall(conversation.id))
                            addAction(getActionReply(conversation.id, userId))
                        }
                    }
                }

            setWhen(conversation.lastMessageTime)

            setLargeIcon(conversation.image?.toBitmap())

            setStyle(updatedMessageStyle)
        }.build()
    }

    /**
     * @return [NotificationCompat.Style] that should be set to Notification with all the messages in it,
     * OR null if there is no new messages in conversation and no need to update the existed notification
     */
    private fun getUpdatedMessageStyle(
        conversation: NotificationConversation,
        activeNotifications: Array<StatusBarNotification>
    ): NotificationCompat.Style? {

        val activeMessages = activeNotifications
            .find { it.id == getConversationNotificationId(conversation.id) }
            ?.notification
            ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
            ?.messages

        val messagesToAdd = conversation.messages
            .map { it.intoStyledMessage() }
            .filter { notificationMessage ->
                // to not notify about messages that are already there
                activeMessages?.find { it.text == notificationMessage.text && it.timestamp == notificationMessage.timestamp } == null
            }

        if (messagesToAdd.isEmpty()) {
            // there is nothing to add to the notification
            return null
        }

        val receiver = Person.Builder()
            .setName(context.getString(R.string.notification_receiver_name))
            .build()

        val notificationStyle = NotificationCompat.MessagingStyle(receiver)

        notificationStyle.conversationTitle = if (conversation.isOneToOneConversation) null else conversation.name
        notificationStyle.isGroupConversation = !conversation.isOneToOneConversation

        activeMessages?.forEach { notificationMessage ->
            notificationStyle.addMessage(notificationMessage)
        }

        messagesToAdd.forEach { notificationMessage ->
            notificationStyle.addMessage(notificationMessage)
        }

        return notificationStyle
    }

    private fun getActionReply(conversationId: String, userId: String?): NotificationCompat.Action {
        val resultPendingIntent = replyMessagePendingIntent(context, conversationId, userId)

        val remoteInput = RemoteInput.Builder(NotificationConstants.KEY_TEXT_REPLY).build()

        return NotificationCompat.Action.Builder(null, context.getString(R.string.notification_action_reply), resultPendingIntent)
            .addRemoteInput(remoteInput)
            .build()
    }

    private fun getActionCall(conversationId: String) = NotificationCompat.Action(
        null,
        context.getString(R.string.notification_action_call),
        callMessagePendingIntent(context, conversationId)
    )

    private fun italicTextFromResId(@StringRes stringResId: Int) =
        context.getString(stringResId).toSpannable()
            .apply { setSpan(StyleSpan(Typeface.ITALIC), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }

    private fun NotificationMessage.intoStyledMessage(): NotificationCompat.MessagingStyle.Message {
        val sender = Person.Builder()
            .apply {
                setName(author.name)
                author.image?.toBitmap()?.let {
                    setIcon(IconCompat.createWithAdaptiveBitmap(it))
                }
            }
            .build()

        val message = when (this) {
            is NotificationMessage.Text -> text
            is NotificationMessage.Comment -> italicTextFromResId(textResId.value)
            is NotificationMessage.ConnectionRequest -> italicTextFromResId(R.string.notification_connection_request)
            is NotificationMessage.ConversationDeleted -> italicTextFromResId(R.string.notification_conversation_deleted)
        }

        return NotificationCompat.MessagingStyle.Message(message, time, sender)
    }

    /**
     * @return true if there is at least one Wire message notification except the Summary notification
     * and notification with id [exceptNotificationId]
     */
    private fun isThereAnyOtherWireNotification(exceptNotificationId: Int): Boolean {
        return oldNotificationManager.activeNotifications
            ?.any {
                it.groupKey.endsWith(NotificationConstants.MESSAGE_GROUP_KEY)
                        && it.id != exceptNotificationId
                        && it.id != NotificationConstants.MESSAGE_SUMMARY_ID
            }
            ?: false
    }

    companion object {
        private const val TAG = "MessageNotificationManager"
    }
}
