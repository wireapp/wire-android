package com.wire.android.notification

import androidx.annotation.StringRes
import com.wire.android.R

data class NotificationData(val conversations: List<NotificationConversation>)

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
