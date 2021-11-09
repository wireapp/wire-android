package com.wire.android.feature.conversation

data class Conversation(
    val id: ConversationID,
    val name: String? = null,
    val type: ConversationType
)

data class ConversationID(val value: String, val domain: String)
