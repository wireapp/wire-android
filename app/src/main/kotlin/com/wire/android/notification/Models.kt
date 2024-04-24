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

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.kalium.logic.data.notification.LocalNotificationCommentType
import com.wire.kalium.logic.data.notification.LocalNotification
import com.wire.kalium.logic.data.notification.LocalNotificationMessage

data class NotificationConversation(
    val id: String,
    val name: String?,
    val messages: List<NotificationMessage>,
    val isOneToOneConversation: Boolean,
    val lastMessageTime: Long
)

sealed class NotificationMessage(open val messageId: String, open val author: NotificationMessageAuthor?, open val time: Long) {
    data class ObfuscatedMessage(
        override val messageId: String,
        override val time: Long
    ) : NotificationMessage(messageId, null, time)

    data class ObfuscatedKnock(
        override val messageId: String,
        override val time: Long
    ) : NotificationMessage(messageId, null, time)

    data class Text(
        override val messageId: String,
        override val author: NotificationMessageAuthor,
        override val time: Long,
        val text: String,
        val isQuotingSelfUser: Boolean
    ) : NotificationMessage(messageId, author, time)

    // shared file, picture, reaction
    data class Comment(
        override val messageId: String,
        override val author: NotificationMessageAuthor,
        override val time: Long,
        val textResId: CommentResId
    ) :
        NotificationMessage(messageId, author, time)

    data class Knock(override val messageId: String, override val author: NotificationMessageAuthor, override val time: Long) :
        NotificationMessage(messageId, author, time)

    data class ConnectionRequest(
        override val messageId: String,
        override val author: NotificationMessageAuthor,
        override val time: Long,
        val authorId: String
    ) : NotificationMessage(messageId, author, time)

    data class ConversationDeleted(
        override val messageId: String,
        override val author: NotificationMessageAuthor,
        override val time: Long
    ) : NotificationMessage(messageId, author, time)
}

data class NotificationMessageAuthor(val name: String)

enum class CommentResId(@StringRes val value: Int) {
    PICTURE(R.string.notification_shared_picture),
    FILE(R.string.notification_shared_file),
    REACTION(R.string.notification_reacted),
    MISSED_CALL(R.string.notification_missed_call),
    NOT_SUPPORTED(R.string.notification_not_supported_issue),
    KNOCK(R.string.notification_knock),
    LOCATION(R.string.notification_shared_location),
}

fun LocalNotification.Conversation.intoNotificationConversation(): NotificationConversation {

    val notificationMessages = this.messages.map { it.intoNotificationMessage() }.sortedBy { it.time }
    val lastMessageTime = this.messages.maxOfOrNull { it.time.toEpochMilliseconds() } ?: 0

    return NotificationConversation(
        id = id.toString(),
        name = conversationName,
        messages = notificationMessages,
        isOneToOneConversation = isOneToOneConversation,
        lastMessageTime = lastMessageTime
    )
}

@Suppress("LongMethod")
fun LocalNotificationMessage.intoNotificationMessage(): NotificationMessage {

    val notificationMessageTime = time.toEpochMilliseconds()

    return when (this) {
        is LocalNotificationMessage.Text -> {
            val notificationMessageAuthor = NotificationMessageAuthor(author.name)
            NotificationMessage.Text(
                messageId = messageId,
                author = notificationMessageAuthor,
                time = notificationMessageTime,
                text = text,
                isQuotingSelfUser = isQuotingSelfUser
            )
        }

        is LocalNotificationMessage.Comment -> {
            val notificationMessageAuthor = NotificationMessageAuthor(author.name)
            NotificationMessage.Comment(
                messageId,
                notificationMessageAuthor,
                notificationMessageTime,
                type.intoCommentResId()
            )
        }

        is LocalNotificationMessage.ConnectionRequest -> {
            val notificationMessageAuthor = NotificationMessageAuthor(author.name)
            NotificationMessage.ConnectionRequest(
                messageId,
                notificationMessageAuthor,
                notificationMessageTime,
                this.authorId.toString()
            )
        }

        is LocalNotificationMessage.ConversationDeleted -> {
            val notificationMessageAuthor = NotificationMessageAuthor(author.name)
            NotificationMessage.ConversationDeleted(
                messageId,
                notificationMessageAuthor,
                notificationMessageTime
            )
        }

        is LocalNotificationMessage.Knock -> {
            val notificationMessageAuthor = NotificationMessageAuthor(author.name)
            NotificationMessage.Knock(
                messageId,
                notificationMessageAuthor,
                notificationMessageTime
            )
        }

        is LocalNotificationMessage.SelfDeleteKnock -> {
            NotificationMessage.ObfuscatedKnock(
                messageId = messageId,
                time = notificationMessageTime
            )
        }

        is LocalNotificationMessage.SelfDeleteMessage -> NotificationMessage.ObfuscatedMessage(
            messageId,
            notificationMessageTime
        )
    }
}

fun LocalNotificationCommentType.intoCommentResId(): CommentResId =
    when (this) {
        LocalNotificationCommentType.PICTURE -> CommentResId.PICTURE
        LocalNotificationCommentType.FILE -> CommentResId.FILE
        LocalNotificationCommentType.REACTION -> CommentResId.REACTION
        LocalNotificationCommentType.MISSED_CALL -> CommentResId.MISSED_CALL
        LocalNotificationCommentType.NOT_SUPPORTED_YET -> CommentResId.NOT_SUPPORTED
        LocalNotificationCommentType.LOCATION -> CommentResId.LOCATION
    }
