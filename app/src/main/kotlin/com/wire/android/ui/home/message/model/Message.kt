package com.wire.android.ui.main.message.model

import com.wire.android.R
import com.wire.android.ui.home.conversations.all.model.AvailabilityStatus
import com.wire.android.ui.home.conversations.all.model.Membership


data class MessageHeader(
    val username: String,
    val membership: Membership,
    val isLegalHold: Boolean,
    val time: String,
    val messageStatus: MessageStatus
)

enum class MessageStatus(val stringResourceId: Int) {
    Untouched(-1), Deleted(R.string.label_message_status_deleted), Edited(R.string.label_message_status_edited)
}

data class Message(
    val user: User,
    val messageHeader: MessageHeader,
    val messageContent: MessageContent,
) {
    val isDeleted = messageHeader.messageStatus == MessageStatus.Deleted
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
    val availabilityStatus: AvailabilityStatus,
)
