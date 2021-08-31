package com.wire.android.feature.conversation.content

import java.time.OffsetDateTime

data class Message(
    val id: String,
    val conversationId: String,
    val senderUserId: String,
    val clientId: String?,
    val content: Content,
    val state: MessageState,
    val time: OffsetDateTime,
    val isRead: Boolean
)

sealed class Content {
    data class Text(val value: String) : Content()
}
