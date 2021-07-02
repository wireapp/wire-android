package com.wire.android.feature.conversation.content

import java.time.OffsetDateTime

data class Message(
    val id: String,
    val conversationId: String,
    val senderUserId: String,
    val clientId: String?,
    val content: String,
    val type: MessageType,
    val state: MessageState,
    val time: OffsetDateTime
)
