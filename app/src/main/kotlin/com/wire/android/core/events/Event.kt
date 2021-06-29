package com.wire.android.core.events

sealed class Event {
    data class UsernameChanged(val username: String) : Event()
    data class ConversationNameChanged(val name: String) : Event()
    data class Message(val conversationId: Int, val text: String) : Event()
    object Unknown : Event()
}
