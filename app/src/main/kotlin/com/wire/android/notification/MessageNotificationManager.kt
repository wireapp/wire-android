package com.wire.android.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.service.notification.StatusBarNotification
import android.text.Spannable
import android.text.style.StyleSpan
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
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
class MessageNotificationManager
@Inject constructor(
    private val context: Context,
    private val notificationManagerCompat: NotificationManagerCompat,
    private val notificationManager: NotificationManager
) {
    fun handleNotification(newNotifications: List<LocalNotificationConversation>, userId: QualifiedID?) {
        if (newNotifications.isEmpty()) return

        val activeNotifications: Array<StatusBarNotification> = notificationManager.activeNotifications ?: arrayOf()
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
            notificationManagerCompat.cancel(notificationId)
        } else {
            hideAllNotifications()
        }
    }

    fun hideAllNotifications() {
        // removing groupSummary removes all the notifications in a group
        notificationManagerCompat.cancel(NotificationConstants.MESSAGE_SUMMARY_ID)
    }

    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannelCompat
            .Builder(NotificationConstants.MESSAGE_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_MAX)
            .setName(NotificationConstants.MESSAGE_CHANNEL_NAME)
            .build()

        notificationManagerCompat.createNotificationChannel(notificationChannel)
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
        val notificationId = getConversationNotificationId(conversation.id + userId)
        getConversationNotification(conversation, userId, activeNotifications)?.let { notification ->
            appLogger.i("$TAG adding ConversationNotification")
            notificationManagerCompat.notify(notificationId, notification)
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
     * Creates or updates existing [Notification]
     * @param [conversation] the notification object containing all the messages for the given conversation
     * @param [userId] the id of the user receiving the notifications
     * @param [activeNotifications] a list with the notifications that are already be displayed to the user
     * @return [Notification] for the conversation with all the messages in it (including previous messages as well)
     * OR null if there is no new messages in conversation and no need to update the existed notification.
     */
    private fun getConversationNotification(
        conversation: NotificationConversation,
        userId: String?,
        activeNotifications: Array<StatusBarNotification>
    ): Notification? {

        val updatedMessageStyle = getUpdatedMessageStyle(conversation, userId, activeNotifications) ?: return null

        return setUpNotificationBuilder(context).apply {
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
                            setContentIntent(messagePendingIntent(context, conversation.id, userId))
                            addAction(getActionCall(context, conversation.id, userId))
                            addAction(getActionReply(context, conversation.id, userId))
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
        userId: String?,
        activeNotifications: Array<StatusBarNotification>
    ): NotificationCompat.Style? {

        val activeMessages = activeNotifications
            .find { it.id == getConversationNotificationId(conversation.id + userId) }
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
            // there is nothing to add to this notification
            return null
        }

        val receiver = Person.Builder()
            .setName(context.getString(R.string.notification_receiver_name))
            .build()

        val notificationStyle = NotificationCompat.MessagingStyle(receiver)

        notificationStyle.conversationTitle = getConversationTitle(conversation)
        notificationStyle.isGroupConversation = !conversation.isOneToOneConversation

        activeMessages?.forEach { notificationMessage ->
            notificationStyle.addMessage(notificationMessage)
        }

        messagesToAdd.forEach { notificationMessage ->
            notificationStyle.addMessage(notificationMessage)
        }

        return notificationStyle
    }

    private fun getConversationTitle(conversation: NotificationConversation): String? =
        if (conversation.isOneToOneConversation) null else conversation.name

    private fun italicTextFromResId(@StringRes stringResId: Int): Spannable {
        return context.getString(stringResId)
            .let {
                val typeface = Typeface.defaultFromStyle(Typeface.ITALIC)
                val isItalicFaked = !typeface.isItalic
                // Default typeface doesn't support italic, system will fake it by skewing the glyphs using `textPaint.textSkewX = -0.25f`,
                // but the width won't be adjusted so we have to add a space after the text.
                // https://saket.me/android-fake-vs-true-bold-and-italic/
                if (isItalicFaked) "$it\u00A0" else it
            }
            .toSpannable()
            .apply {
                setSpan(StyleSpan(Typeface.ITALIC), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
    }


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
        return notificationManager.activeNotifications
            ?.any {
                it.groupKey.endsWith(NotificationConstants.MESSAGE_GROUP_KEY)
                        && it.id != exceptNotificationId
                        && it.id != NotificationConstants.MESSAGE_SUMMARY_ID
            }
            ?: false
    }

    companion object {
        private const val TAG = "MessageNotificationManager"

        /**
         * Update notification by adding [replyText] to the end of messages list with "You" as sender.
         * Do nothing if there is no notification for [conversationId] conversation,
         * or that notification is not in [NotificationCompat.MessagingStyle].
         * @param context Context
         * @param conversationId String ConversationId that reply was sent in.
         * @param userId String UserId of the user that is replied.
         * @param replyText String text the user replied to the conversation.
         * If it's null then just update the notification to remove send reply loading.
         */
        fun updateNotificationAfterQuickReply(
            context: Context,
            conversationId: String,
            userId: String,
            replyText: String?
        ) {
            val conversationNotificationId = getConversationNotificationId(conversationId)

            val currentNotification = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .activeNotifications
                .find { it.id == conversationNotificationId }
                ?.notification
                ?: return // if there is no notification for this conversation, then there is nothing to update

            val messagesStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(currentNotification)
                ?: return // if notification is not in MessagingStyle, then we can't update it

            val sender = Person.Builder()
                .setName(context.getString(R.string.notification_receiver_name))
                .build()

            replyText?.let {
                val replyMessage = NotificationCompat.MessagingStyle.Message(replyText, System.currentTimeMillis(), sender)
                messagesStyle.addMessage(replyMessage)
            }

            val notification = setUpNotificationBuilder(context).apply {
                setContentIntent(messagePendingIntent(context, conversationId, userId))
                addAction(getActionCall(context, conversationId, userId))
                addAction(getActionReply(context, conversationId, userId))

                setWhen(System.currentTimeMillis())

                setLargeIcon((currentNotification.getLargeIcon()?.loadDrawable(context) as BitmapDrawable?)?.bitmap)

                setStyle(messagesStyle)
            }.build()

            NotificationManagerCompat.from(context).notify(conversationNotificationId, notification)
        }

        /**
         * Create NotificationBuilder and set all the parameters that are common for any MessageNotification
         * @return resulted [NotificationCompat.Builder] so we can set other specific parameters and build it.
         */
        private fun setUpNotificationBuilder(context: Context) =
            NotificationCompat.Builder(context, NotificationConstants.MESSAGE_CHANNEL_ID).apply {
                setDefaults(NotificationCompat.DEFAULT_ALL)

                priority = NotificationCompat.PRIORITY_MAX
                setCategory(NotificationCompat.CATEGORY_MESSAGE)

                setSmallIcon(R.drawable.notification_icon_small)
                setGroup(NotificationConstants.MESSAGE_GROUP_KEY)
                setAutoCancel(true)
            }
    }
}
