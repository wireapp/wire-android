package com.wire.android.notification

import androidx.annotation.StringRes
import com.wire.android.R

data class NotificationData(val conversations: List<NotificationConversation>)

data class NotificationConversation(
    val id: String,
    val name: String,
    val image: ByteArray?,
    val messages: List<NotificationMessage>,
    val isDirectChat: Boolean,
    val lastMessageTime: Long
)

sealed class NotificationMessage(open val author: String) {
    data class Text(override val author: String, val text: String) : NotificationMessage(author)

    //shared file, picture, reaction
    data class Comment(override val author: String, val textResId: CommentResId) : NotificationMessage(author)
}

enum class CommentResId(@StringRes val value: Int) {
    PICTURE(R.string.notification_shared_picture),
    FILE(R.string.notification_shared_file),
    REACTION(R.string.notification_reacted)
}
