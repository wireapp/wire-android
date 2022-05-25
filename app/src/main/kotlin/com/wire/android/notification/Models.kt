package com.wire.android.notification

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.kalium.logic.data.notification.LocalNotificationCommentType
import com.wire.kalium.logic.data.notification.LocalNotificationConversation
import com.wire.kalium.logic.data.notification.LocalNotificationMessage
import com.wire.kalium.logic.util.toTimeInMillis

data class NotificationConversation(
    val id: String,
    val name: String,
    val image: ByteArray?,
    val messages: List<NotificationMessage>,
    val isOneToOneConversation: Boolean,
    val lastMessageTime: Long
)

sealed class NotificationMessage(open val author: NotificationMessageAuthor, open val time: Long) {
    data class Text(override val author: NotificationMessageAuthor, override val time: Long, val text: String) :
        NotificationMessage(author, time)

    //shared file, picture, reaction
    data class Comment(override val author: NotificationMessageAuthor, override val time: Long, val textResId: CommentResId) :
        NotificationMessage(author, time)
}

data class NotificationMessageAuthor(val name: String, val image: ByteArray?)

enum class CommentResId(@StringRes val value: Int) {
    PICTURE(R.string.notification_shared_picture),
    FILE(R.string.notification_shared_file),
    REACTION(R.string.notification_reacted)
}

fun LocalNotificationConversation.intoNotificationConversation() : NotificationConversation{

    val notificationMessages = this.messages.map { it.intoNotificationMessage() }.sortedBy { it.time }
    val lastMessageTime = this.messages.maxOfOrNull { it.time.toTimeInMillis() } ?: 0

    return NotificationConversation(
        id = id.toString(),
        name = conversationName,
        image = null, //TODO
        messages = notificationMessages,
        isOneToOneConversation = isOneToOneConversation,
        lastMessageTime = lastMessageTime
    )
}

fun LocalNotificationMessage.intoNotificationMessage(): NotificationMessage {

    val notificationMessageAuthor = NotificationMessageAuthor(author.name, null) //TODO image
    val notificationMessageTime = time.toTimeInMillis()

    return when (this) {
        is LocalNotificationMessage.Text -> NotificationMessage.Text(notificationMessageAuthor, notificationMessageTime, text)
        is LocalNotificationMessage.Comment -> NotificationMessage.Comment(
            notificationMessageAuthor,
            notificationMessageTime,
            type.intoCommentResId()
        )
    }
}

fun LocalNotificationCommentType.intoCommentResId(): CommentResId =
    when (this) {
        LocalNotificationCommentType.PICTURE -> CommentResId.PICTURE
        LocalNotificationCommentType.FILE -> CommentResId.FILE
        LocalNotificationCommentType.REACTION -> CommentResId.REACTION
    }
