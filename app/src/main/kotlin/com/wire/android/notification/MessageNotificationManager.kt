package com.wire.android.notification

import android.content.Context
import android.graphics.Typeface
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

    private var prevNotificationsData: List<LocalNotificationConversation> = listOf()

    init {
        appLogger.i("$TAG: initialized")
    }

    fun handleNotification(
        newData: List<LocalNotificationConversation>,
        userId: QualifiedID?
    ) {
        val oldData = prevNotificationsData
        val oldConversationIds = oldData.map { it.id }
        val newConversationIds = newData.map { it.id }

        val conversationIdsToRemove = oldConversationIds.filter { !newConversationIds.contains(it) }
        val conversationsToAdd = newData
            .filter { conversation ->
                val oldConversation = oldData.firstOrNull { it.id == conversation.id }
                oldConversation == null || oldConversation != conversation
            }
            .map { it.intoNotificationConversation() }

        val userIdString = userId?.toString()

        createNotificationChannel()
        conversationsToAdd.forEach { showConversationNotification(it, userIdString) }
        conversationIdsToRemove.forEach { hideNotification(it) }
        showSummaryIfNeeded(oldData, newData, userIdString)

        appLogger.i(
            "$TAG: handled notifications: oldDataSize ${oldData.size}; newDataSize ${newData.size}; " +
                    "${conversationsToAdd.size} notifications were added; ${conversationIdsToRemove.size} notifications were removed. "
        )
        prevNotificationsData = newData
    }

    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannelCompat
            .Builder(NotificationConstants.MESSAGE_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_MAX)
            .setName(NotificationConstants.MESSAGE_CHANNEL_NAME)
            .build()

        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun showSummaryIfNeeded(
        oldData: List<LocalNotificationConversation>,
        newData: List<LocalNotificationConversation>,
        userId: String?
    ) {
        if (newData.isEmpty()) {
            appLogger.i("$TAG removing groupSummary")
            notificationManager.cancel(NotificationConstants.MESSAGE_SUMMARY_ID)
        } else if (oldData.size <= 1 && newData.size > 1) {
            appLogger.i("$TAG adding groupSummary")
            notificationManager.notify(NotificationConstants.MESSAGE_SUMMARY_ID, getSummaryNotification(userId))
        }
    }

    private fun showConversationNotification(conversation: NotificationConversation, userId: String?) {
        val notificationId = NotificationConstants.getConversationNotificationId(conversation.id)
        val notification = getConversationNotification(conversation, userId)
        notificationManager.notify(notificationId, notification)
    }

    private fun hideNotification(conversationsId: ConversationId) =
        notificationManager.cancel(NotificationConstants.getConversationNotificationId(conversationsId))

    private fun getSummaryNotification(userId: String?) = NotificationCompat.Builder(context, NotificationConstants.MESSAGE_CHANNEL_ID)
        .setSmallIcon(R.drawable.notification_icon_small)
        .setGroup(NotificationConstants.MESSAGE_GROUP_KEY)
        .setGroupSummary(true)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setContentIntent(summaryMessagePendingIntent(context))
        .setDeleteIntent(dismissSummaryPendingIntent(context, userId))
        .build()

    private fun getConversationNotification(conversation: NotificationConversation, userId: String?) =
        NotificationCompat.Builder(context, NotificationConstants.MESSAGE_CHANNEL_ID).apply {
            setDefaults(NotificationCompat.DEFAULT_ALL)

            priority = NotificationCompat.PRIORITY_MAX
            setCategory(NotificationCompat.CATEGORY_MESSAGE)

            setSmallIcon(R.drawable.notification_icon_small)
            setGroup(NotificationConstants.MESSAGE_GROUP_KEY)
            setAutoCancel(true)

            conversation.messages
                .filterIsInstance<NotificationMessage.ConnectionRequest>()
                .firstOrNull()
                .let {
                    if (it == null) {
                        // It's regular Message Notification
                        setContentIntent(messagePendingIntent(context, conversation.id))
                        setDeleteIntent(dismissMessagePendingIntent(context, conversation.id, userId))
                        addAction(getActionCall(conversation.id))
                        addAction(getActionReply(conversation.id, userId))
                    } else {
                        // It's ConnectionRequest Notification
                        setContentIntent(otherUserProfilePendingIntent(context, it.authorId))
                        setDeleteIntent(dismissConnectionRequestPendingIntent(context, it.authorId, userId))
                    }
                }

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
        notificationStyle.isGroupConversation = !conversation.isOneToOneConversation

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
                is NotificationMessage.Comment -> italicTextFromResId(messageData.textResId.value)
                is NotificationMessage.ConnectionRequest -> italicTextFromResId(R.string.notification_connection_request)
                is NotificationMessage.ConversationDeleted -> italicTextFromResId(R.string.notification_conversation_deleted)
            }

            val notificationMessage = NotificationCompat.MessagingStyle.Message(message, messageData.time, sender)

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

    companion object {
        private const val TAG = "MessageNotificationManager"

        fun cancelNotification(context: Context, notificationId: Int) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }
}
