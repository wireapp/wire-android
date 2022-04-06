package com.wire.android.notification

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.kalium.logic.data.id.asString
import com.wire.kalium.logic.data.notification.DbNotificationCommentType
import com.wire.kalium.logic.data.notification.DbNotificationConversation
import com.wire.kalium.logic.data.notification.DbNotificationMessage
import com.wire.kalium.logic.util.toTimeInMillis

data class NotificationData(val conversations: List<NotificationConversation>)

data class NotificationConversation(
    val id: String,
    val name: String,
    val image: ByteArray?,
    val messages: List<NotificationMessage>,
    val isOneToOneConversation: Boolean,
    val lastMessageTime: Long
) {
    companion object {
        fun fromDbData(dbData: DbNotificationConversation): NotificationConversation {

            val messages = dbData.messages.map { NotificationMessage.fromDbData(it) }
            val lastMessageTime = dbData.messages.maxOf { it.time.toTimeInMillis() }

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
        fun fromDbData(dbData: DbNotificationMessage): NotificationMessage {

            val author = NotificationMessageAuthor(dbData.author.name, null) //TODO image
            val time = dbData.time.toTimeInMillis()

            return when (dbData) {
                is DbNotificationMessage.Text -> Text(author, time, dbData.text)
                is DbNotificationMessage.Comment -> Comment(author, time, dbTypeIntoCommentResId(dbData.type))
            }
        }

        private fun dbTypeIntoCommentResId(dbType: DbNotificationCommentType): CommentResId =
            when (dbType) {
                DbNotificationCommentType.PICTURE -> CommentResId.PICTURE
                DbNotificationCommentType.FILE -> CommentResId.FILE
                DbNotificationCommentType.REACTION -> CommentResId.REACTION
            }
    }
}

data class NotificationMessageAuthor(val name: String, val image: ByteArray?)

enum class CommentResId(@StringRes val value: Int) {
    PICTURE(R.string.notification_shared_picture),
    FILE(R.string.notification_shared_file),
    REACTION(R.string.notification_reacted)
}
