package com.wire.android.core.events

sealed class Event {
    sealed class Conversation : Event() {
        data class MessageEvent(
            val id: String,
            val conversationId: String,
            val senderClientId: String,
            val senderUserId: String,
            val content: String,
            val time: String
        ) : Conversation()
    }
    object Unknown : Event()
}
