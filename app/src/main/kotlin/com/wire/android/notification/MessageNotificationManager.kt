/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.service.notification.StatusBarNotification
import android.text.Spannable
import android.text.style.StyleSpan
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.text.toSpannable
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.notification.NotificationConstants.getConversationNotificationId
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.notification.LocalNotification
import com.wire.kalium.logic.data.notification.LocalNotificationUpdateMessageAction
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Singleton
class MessageNotificationManager
@Inject constructor(
    private val context: Context,
    private val notificationManagerCompat: NotificationManagerCompat,
    private val notificationManager: NotificationManager,
    private val lockCodeTimeManager: LockCodeTimeManager
) {

    fun handleNotification(newNotifications: List<LocalNotification>, userId: QualifiedID, userName: String) {
        if (newNotifications.isEmpty()) return

        addNotifications(newNotifications, userId, userName)
        updateNotifications(newNotifications, userId)
        removeSeenNotifications(newNotifications, userId)

        appLogger.i("$TAG: handled notifications: newNotifications size ${newNotifications.size}; ")
    }

    private fun addNotifications(newNotifications: List<LocalNotification>, userId: QualifiedID, userName: String) {
        val notificationsToAdd: List<LocalNotification.Conversation> = newNotifications
            .filterIsInstance(LocalNotification.Conversation::class.java)

        val activeNotifications: Array<StatusBarNotification> = notificationManager.activeNotifications ?: arrayOf()

        notificationsToAdd.forEach {
            showConversationNotification(it, userId, activeNotifications)
        }

        showSummaryIfNeeded(userId, activeNotifications, userName)

        appLogger.i("$TAG: added notifications: newNotifications size ${notificationsToAdd.size}; ")
    }

    private fun updateNotifications(newNotifications: List<LocalNotification>, userId: QualifiedID) {
        val notificationsToUpdate: List<LocalNotification.UpdateMessage> = newNotifications
            .filterIsInstance(LocalNotification.UpdateMessage::class.java)

        val activeNotifications: Array<StatusBarNotification> = notificationManager.activeNotifications ?: return

        notificationsToUpdate.groupBy { it.conversationId }.forEach { (conversationId, updateMessages) ->
            updateConversationNotification(conversationId, updateMessages, userId, activeNotifications)
        }

        removeSummaryIfNeeded(userId)

        appLogger.i("$TAG: updated notifications: newNotifications size ${notificationsToUpdate.size}; ")
    }

    private fun removeSeenNotifications(newNotifications: List<LocalNotification>, userId: QualifiedID) {
        val notificationsToUpdate: List<LocalNotification.ConversationSeen> = newNotifications
            .filterIsInstance(LocalNotification.ConversationSeen::class.java)

        notificationsToUpdate.groupBy { it.conversationId }.forEach { (conversationId, _) ->
            hideNotification(conversationId, userId)
        }

        removeSummaryIfNeeded(userId)

        appLogger.i("$TAG: removed ${notificationsToUpdate.size} notifications, it was seen;")
    }

    fun hideNotification(conversationsId: ConversationId, userId: QualifiedID) {
        val notificationId = getConversationNotificationId(conversationsId.toString(), userId.toString())

        if (isThereAnyOtherWireNotification(userId, notificationId)) {
            notificationManagerCompat.cancel(notificationId)
        } else {
            hideAllNotificationsForUser(userId)
        }
    }

    fun hideAllNotifications() {
        notificationManager.cancelAll()
    }

    fun hideAllNotificationsForUser(userId: QualifiedID) {
        // removing groupSummary removes all the notifications in a group
        notificationManagerCompat.cancel(NotificationConstants.getMessagesSummaryId(userId))
    }

    private fun showSummaryIfNeeded(userId: QualifiedID, activeNotifications: Array<StatusBarNotification>, userName: String) {
        if (activeNotifications.find { it.id == NotificationConstants.getMessagesSummaryId(userId) } != null) return

        appLogger.i("$TAG adding groupSummary")
        notificationManager.notify(NotificationConstants.getMessagesSummaryId(userId), getSummaryNotification(userId, userName))
    }

    private fun removeSummaryIfNeeded(userId: QualifiedID) {
        if (!isThereAnyOtherWireNotification(userId)) {
            hideAllNotificationsForUser(userId)
        }
    }

    @SuppressLint("MissingPermission")
    // TODO(permissions): Check for permission before calling notificationManagerCompat.notify
    private fun showConversationNotification(
        localConversation: LocalNotification.Conversation,
        userId: QualifiedID,
        activeNotifications: Array<StatusBarNotification>
    ) {
        val conversation = localConversation.intoNotificationConversation()
        val notificationId = getConversationNotificationId(conversation.id, userId.toString())
        getConversationNotification(conversation, userId, activeNotifications)?.let { notification ->
            appLogger.i("$TAG adding ConversationNotification")
            notificationManagerCompat.notify(notificationId, notification)
        }
    }

    // TODO(permissions): Check for permission before calling notificationManagerCompat.notify
    @SuppressLint("MissingPermission")
    private fun updateConversationNotification(
        conversationId: ConversationId,
        updateMessages: List<LocalNotification.UpdateMessage>,
        userId: QualifiedID,
        activeNotifications: Array<StatusBarNotification>
    ) {
        val notificationId = getConversationNotificationId(conversationId.toString(), userId.toString())
        getUpdatedConversationNotification(notificationId, userId, updateMessages, activeNotifications)?.let { notification ->
            appLogger.i("$TAG updating ConversationNotification")
            notificationManagerCompat.notify(notificationId, notification)
        } ?: notificationManagerCompat.cancel(notificationId)
    }

    private fun getSummaryNotification(userId: QualifiedID, userName: String): Notification {
        val channelId = NotificationConstants.getMessagesChannelId(userId)
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.wire.android.feature.notification.R.drawable.notification_icon_small)
            .setGroup(NotificationConstants.getMessagesGroupKey(userId))
            .setStyle(NotificationCompat.InboxStyle().setSummaryText(userName))
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(summaryMessagePendingIntent(context))
            .build()
    }

    /**
     * Creates or updates existing [Notification]
     * @param [conversation] the notification object containing all the messages for the given conversation
     * @param [userId] the id of the user receiving the notifications
     * @param [activeNotifications] a list with the notifications that are already be displayed to the user
     * @return [Notification] for the conversation with all the messages in it (including previous messages as well)
     * OR null if there is no new messages in conversation and no need to update the existed notification.
     */
    @Suppress("LongMethod", "ComplexMethod", "NestedBlockDepth")
    private fun getConversationNotification(
        conversation: NotificationConversation,
        userId: QualifiedID,
        activeNotifications: Array<StatusBarNotification>
    ): Notification? {

        val userIdString = userId.toString()
        val updatedMessageStyle = activeNotifications
            .find { it.id == getConversationNotificationId(conversation.id, userIdString) }
            ?.notification
            ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
            .addMessages(conversation)
            ?: return null

        return setUpNotificationBuilder(context, userId)
            .apply {
                conversation.messages.firstOrNull()
                    .let {
                        when (it) {
                            is NotificationMessage.ConnectionRequest -> {
                                setContentIntent(otherUserProfilePendingIntent(context, it.authorId, userIdString))
                            }

                            is NotificationMessage.ConversationDeleted -> {
                                setContentIntent(openAppPendingIntent(context))
                            }

                            is NotificationMessage.Comment -> {
                                if (conversation.isReplyAllowed) {
                                    val isAppLocked = lockCodeTimeManager.isAppLocked()
                                    addAction(getActionReply(context, conversation.id, userIdString, isAppLocked))
                                }
                                setContentIntent(messagePendingIntent(context, conversation.id, userIdString))
                            }

                            is NotificationMessage.Knock -> {
                                setChannelId(NotificationConstants.getPingsChannelId(userId))
                                setContentIntent(messagePendingIntent(context, conversation.id, userIdString))
                            }

                            is NotificationMessage.Text -> {
                                if (conversation.isReplyAllowed) {
                                    val isAppLocked = lockCodeTimeManager.isAppLocked()
                                    addAction(getActionReply(context, conversation.id, userIdString, isAppLocked))
                                }
                                setContentIntent(messagePendingIntent(context, conversation.id, userIdString))
                            }

                            is NotificationMessage.ObfuscatedMessage -> {
                                if (conversation.isReplyAllowed) {
                                    val isAppLocked = lockCodeTimeManager.isAppLocked()
                                    addAction(getActionReply(context, conversation.id, userIdString, isAppLocked))
                                }
                                setContentIntent(messagePendingIntent(context, conversation.id, userIdString))
                            }

                            is NotificationMessage.ObfuscatedKnock -> {
                                setChannelId(NotificationConstants.getPingsChannelId(userId))
                                setContentIntent(messagePendingIntent(context, conversation.id, userIdString))
                            }

                            null -> {
                                if (conversation.isReplyAllowed) {
                                    val isAppLocked = lockCodeTimeManager.isAppLocked()
                                    addAction(getActionReply(context, conversation.id, userIdString, isAppLocked))
                                }
                                setContentIntent(messagePendingIntent(context, conversation.id, userIdString))
                            }
                        }
                    }

                setWhen(conversation.lastMessageTime)

                setStyle(updatedMessageStyle)
            }.build()
    }

    /**
     * Updates existing [Notification] by applying changes to it's messages
     * @param [notificationId] ID of the notification that should be updated
     * @param [userId] the id of the user receiving the notifications
     * @param [updateMessages] list of changes that should be applied to the messages in that notification
     * @param [activeNotifications] a list with the notifications that are already be displayed to the user
     * @return [Notification] for the conversation with updated messages in it
     * OR null if all the messages in notification were removed and Notification should be removed too.
     */
    private fun getUpdatedConversationNotification(
        notificationId: Int,
        userId: QualifiedID,
        updateMessages: List<LocalNotification.UpdateMessage>,
        activeNotifications: Array<StatusBarNotification>
    ): Notification? {
        val currentNotification = activeNotifications
            .find { it.id == notificationId }
            ?.notification

        val updatedMessageStyle = currentNotification?.updateMessages(updateMessages)

        if (updatedMessageStyle == null || updatedMessageStyle.messages.isEmpty()) return null

        return setUpNotificationBuilder(context, userId).apply {
            setContentIntent(currentNotification.contentIntent)
            currentNotification.actions?.forEach { addAction(getActionFromOldOne(it)) }

            setWhen(System.currentTimeMillis())

            setLargeIcon((currentNotification.getLargeIcon()?.loadDrawable(context) as BitmapDrawable?)?.bitmap)

            setStyle(updatedMessageStyle)
        }.build()
    }

    /**
     * @return [NotificationCompat.Style] that should be set to Notification with all the messages in it,
     * OR null if there is no new messages in conversation and no need to update the existed notification
     */
    private fun NotificationCompat.MessagingStyle?.addMessages(conversation: NotificationConversation): NotificationCompat.Style? {
        val messagesToAdd = conversation.messages
            .map { it.intoStyledMessage() }
            .filter { notificationMessage ->
                // to not notify about messages that are already there
                this?.messages
                    ?.find {
                        it.text.toString() == notificationMessage.text.toString()
                                && it.timestamp == notificationMessage.timestamp
                    } == null
            }

        if (messagesToAdd.isEmpty()) {
            // there is nothing to add to this notification
            return null
        }

        val receiver = youPerson(context)

        val notificationStyle = NotificationCompat.MessagingStyle(receiver)

        notificationStyle.conversationTitle = getConversationTitle(conversation)
        notificationStyle.isGroupConversation = !conversation.isOneToOneConversation

        this?.messages?.forEach { notificationMessage ->
            notificationStyle.addMessage(notificationMessage)
        }

        messagesToAdd.forEach { notificationMessage ->
            notificationStyle.addMessage(notificationMessage)
        }

        return notificationStyle
    }

    /**
     * @return [NotificationCompat.MessagingStyle] that should be set to Notification with all the messages in it,
     * OR null if there is no new messages in conversation and no need to update the existed notification
     */
    private fun Notification.updateMessages(updateMessages: List<LocalNotification.UpdateMessage>): NotificationCompat.MessagingStyle? {
        val activeStyledNotification = this
            .let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
            ?: return null

        val messagesToShow = mutableListOf<NotificationCompat.MessagingStyle.Message>()

        activeStyledNotification.messages
            .forEach { message ->
                val updateMessageAction = updateMessages
                    .firstOrNull { it.messageId == message.extras.getString(MESSAGE_ID_EXTRA) }
                    ?.action

                when (updateMessageAction) {
                    null -> messagesToShow.add(message)
                    is LocalNotificationUpdateMessageAction.Edit -> {
                        messagesToShow.add(
                            NotificationCompat.MessagingStyle.Message(
                                updateMessageAction.updateText,
                                message.timestamp,
                                message.person
                            ).apply {
                                extras.putString(MESSAGE_ID_EXTRA, updateMessageAction.newMessageId)
                            }
                        )
                    }

                    is LocalNotificationUpdateMessageAction.Delete -> {
                        // message was removed, do nothing
                    }
                }
            }

        val receiver = youPerson(context)

        val notificationStyle = NotificationCompat.MessagingStyle(receiver)

        notificationStyle.conversationTitle = activeStyledNotification.conversationTitle
        notificationStyle.isGroupConversation = activeStyledNotification.isGroupConversation

        messagesToShow.forEach { notificationMessage ->
            notificationStyle.addMessage(notificationMessage)
        }

        return notificationStyle
    }

    private fun getConversationTitle(conversation: NotificationConversation): String? =
        if (conversation.isOneToOneConversation) null else conversation.name

    private fun italicTextFromResId(@StringRes stringResId: Int): Spannable {
        return context.getString(stringResId)
            .let {
                // On some devices typeface used by default in notifications doesn't support italic, system will fake it by skewing the
                // glyphs using `textPaint.textSkewX = -0.25f`, but the width won't be adjusted so we have to add a space after the text.
                // https://saket.me/android-fake-vs-true-bold-and-italic/
                "$it "
            }
            .toSpannable()
            .apply {
                setSpan(StyleSpan(Typeface.ITALIC), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
    }

    private fun NotificationMessage.intoStyledMessage(): NotificationCompat.MessagingStyle.Message {
        val sender = Person.Builder()
            .apply {
                if (this@intoStyledMessage is NotificationMessage.ObfuscatedMessage) {
                    setName(context.getString(R.string.notification_obfuscated_message_title))
                } else {
                    author?.name.also {
                        setName(it)
                    }
                }
            }
            .build()

        val message = when (this@intoStyledMessage) {
            is NotificationMessage.Text -> if (isQuotingSelfUser) context.getString(R.string.notification_reply, text) else text
            is NotificationMessage.Comment -> italicTextFromResId(textResId.value)
            is NotificationMessage.ConnectionRequest -> italicTextFromResId(R.string.notification_connection_request)
            is NotificationMessage.ConversationDeleted -> italicTextFromResId(R.string.notification_conversation_deleted)
            is NotificationMessage.Knock -> italicTextFromResId(R.string.notification_knock)
            is NotificationMessage.ObfuscatedMessage,
            is NotificationMessage.ObfuscatedKnock -> italicTextFromResId(
                R.string.notification_obfuscated_message_content
            )
        }
        return NotificationCompat.MessagingStyle.Message(message, time, sender).apply {
            extras.putString(MESSAGE_ID_EXTRA, this@intoStyledMessage.messageId)
        }
    }

    /**
     * @return true if there is at least one Wire message notification except the Summary notification
     * and notifications with id [exceptNotificationIds]
     */
    private fun isThereAnyOtherWireNotification(userId: QualifiedID, vararg exceptNotificationIds: Int): Boolean {
        return notificationManager.activeNotifications
            ?.any {
                it.groupKey.endsWith(NotificationConstants.getMessagesGroupKey(userId))
                        && !exceptNotificationIds.contains(it.id)
                        && it.id != NotificationConstants.getMessagesSummaryId(userId)
            }
            ?: false
    }

    companion object {
        private const val TAG = "MessageNotificationManager"
        private const val MESSAGE_ID_EXTRA = "message_id"

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
        @SuppressLint("MissingPermission")
        fun updateNotificationAfterQuickReply(
            context: Context,
            conversationId: String,
            userId: QualifiedID,
            replyText: String?
        ) {
            val conversationNotificationId = getConversationNotificationId(conversationId, userId.toString())
            val userIdString = userId.toString()

            val currentNotification = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .activeNotifications
                .find { it.id == conversationNotificationId }
                ?.notification
                ?: return // if there is no notification for this conversation, then there is nothing to update

            val messagesStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(currentNotification)
                ?: return // if notification is not in MessagingStyle, then we can't update it

            val sender = youPerson(context)

            replyText?.let {
                val replyMessage = NotificationCompat.MessagingStyle.Message(replyText, System.currentTimeMillis(), sender)
                messagesStyle.addMessage(replyMessage)
            }

            val notification = setUpNotificationBuilder(context, userId, true).apply {
                setContentIntent(messagePendingIntent(context, conversationId, userIdString))
                addAction(getActionReply(context, conversationId, userIdString, false))

                setWhen(System.currentTimeMillis())

                setLargeIcon((currentNotification.getLargeIcon()?.loadDrawable(context) as BitmapDrawable?)?.bitmap)

                setStyle(messagesStyle)
            }.build()

            // TODO(permissions): Check for permission before calling notificationManagerCompat.notify
            NotificationManagerCompat.from(context).notify(conversationNotificationId, notification)
        }

        private fun youPerson(context: Context) = Person.Builder()
            .setName(context.getString(R.string.notification_receiver_name))
            .build()

        /**
         * Create NotificationBuilder and set all the parameters that are common for any MessageNotification
         * use [setOnlyAlertOnce] to trigger only once sound and vibrations for notification updates
         * @return resulted [NotificationCompat.Builder] so we can set other specific parameters and build it.
         */
        private fun setUpNotificationBuilder(
            context: Context,
            userId: QualifiedID,
            setOnlyAlertOnce: Boolean = false
        ): NotificationCompat.Builder {
            val channelId = NotificationConstants.getMessagesChannelId(userId)

            return NotificationCompat.Builder(context, channelId).apply {
                setDefaults(NotificationCompat.DEFAULT_ALL)
                setOnlyAlertOnce(setOnlyAlertOnce)
                priority = NotificationCompat.PRIORITY_MAX
                setCategory(NotificationCompat.CATEGORY_MESSAGE)

                setSmallIcon(com.wire.android.feature.notification.R.drawable.notification_icon_small)
                setGroup(NotificationConstants.getMessagesGroupKey(userId))
                setAutoCancel(true)
            }
        }
    }
}
