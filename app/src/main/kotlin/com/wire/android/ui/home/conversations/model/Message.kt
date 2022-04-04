package com.wire.android.ui.home.conversations.model

import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.home.conversationslist.model.Membership

data class MessageHeader(
    val username: String,
    val membership: Membership,
    val isLegalHold: Boolean,
    val time: String,
    val messageStatus: MessageStatus,
    val messageId: String
)

enum class MessageStatus(val stringResourceId: Int) {
    Untouched(-1), Deleted(R.string.label_message_status_deleted), Edited(R.string.label_message_status_edited)
}

data class Message(
    val user: User,
    val messageSource: MessageSource = MessageSource.CurrentUser,
    val messageHeader: MessageHeader,
    val messageContent: MessageContent,
) {
    val isDeleted: Boolean = messageHeader.messageStatus == MessageStatus.Deleted
}

sealed class MessageContent {
    data class TextMessage(val messageBody: MessageBody) : MessageContent()
    data class ImageMessage(val imageUrl: String) : MessageContent()
}

data class MessageBody(
    val message: String
)

data class User(
    val avatarUrl: String = "",
    val availabilityStatus: UserStatus,
)

enum class MessageSource {
    CurrentUser, OtherUser
}
