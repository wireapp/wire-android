package com.wire.android.core.events

sealed class Event {
    sealed class Conversation : Event() {
        data class Message(
            val id: String,
            val conversationId: String,
            val sender: String,
            val userId: String,
            val content: String,
            val time: String
        ) : Conversation()

        companion object {
            const val NEW_MESSAGE_TYPE = "conversation.otr-message-add"
        }
    }
    object Unknown : Event()
}
