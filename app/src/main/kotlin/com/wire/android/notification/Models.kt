package com.wire.android.notification

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.kalium.logic.data.id.asString
import com.wire.kalium.logic.data.notification.LocalNotificationCommentType
import com.wire.kalium.logic.data.notification.LocalNotificationConversation
import com.wire.kalium.logic.data.notification.LocalNotificationMessage
import com.wire.kalium.logic.util.toTimeInMillis

data class MessagesNotificationData(val conversations: List<NotificationConversation>)

data class NotificationConversation(
    val id: String,
    val name: String,
    val image: ByteArray?,
    val messages: List<NotificationMessage>,
    val isOneToOneConversation: Boolean,
    val lastMessageTime: Long
) {
    companion object {
        fun fromDbData(dbData: LocalNotificationConversation): NotificationConversation {

            val messages = dbData.messages.map { NotificationMessage.fromDbData(it) }.sortedBy { it.time }
            val lastMessageTime = dbData.messages.maxOfOrNull { it.time.toTimeInMillis() } ?: 0

            return NotificationConversation(
                id = dbData.id.asString(),
                name = dbData.name,
                image = null, //TODO
                messages = messages,
                isOneToOneConversation = dbData.isOneToOneConversation,
                lastMessageTime = lastMessageTime
            )
        }
    }
}

sealed class NotificationMessage(open val author: NotificationMessageAuthor, open val time: Long) {
    data class Text(override val author: NotificationMessageAuthor, override val time: Long, val text: String) :
        NotificationMessage(author, time)

    //shared file, picture, reaction
    data class Comment(override val author: NotificationMessageAuthor, override val time: Long, val textResId: CommentResId) :
        NotificationMessage(author, time)

    companion object {
        fun fromDbData(dbData: LocalNotificationMessage): NotificationMessage {

            val author = NotificationMessageAuthor(dbData.author.name, null) //TODO image
            val time = dbData.time.toTimeInMillis()

            return when (dbData) {
                is LocalNotificationMessage.Text -> Text(author, time, dbData.text)
                is LocalNotificationMessage.Comment -> Comment(author, time, dbTypeIntoCommentResId(dbData.type))
            }
        }

        private fun dbTypeIntoCommentResId(dbType: LocalNotificationCommentType): CommentResId =
            when (dbType) {
                LocalNotificationCommentType.PICTURE -> CommentResId.PICTURE
                LocalNotificationCommentType.FILE -> CommentResId.FILE
                LocalNotificationCommentType.REACTION -> CommentResId.REACTION
            }
    }
}

data class NotificationMessageAuthor(val name: String, val image: ByteArray?)

enum class CommentResId(@StringRes val value: Int) {
    PICTURE(R.string.notification_shared_picture),
    FILE(R.string.notification_shared_file),
    REACTION(R.string.notification_reacted)
}
