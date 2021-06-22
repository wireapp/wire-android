package com.wire.android.core.events

sealed class Event {
    data class UsernameChanged(val username: String) : Event()
    data class ConversationNameChanged(val name: String) : Event()
    sealed class Conversation : Event() {
        data class Message(val id: String, val conversationId: String, val sender: String, val content: String) : Conversation()

        companion object {
            const val NEW_MESSAGE_TYPE = "conversation.otr-message-add"
        }
    }
    object Unknown : Event()
}
