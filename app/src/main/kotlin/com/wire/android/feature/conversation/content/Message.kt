package com.wire.android.feature.conversation.content

data class Message(
    val id: String,
    val conversationId: String,
    val content: String,
    val type: MessageType,
    val state: MessageState,
    val time: String,
    val userId: String = "",
    val clientId: String = "",
)
