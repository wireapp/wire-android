package com.wire.android.ui.main.conversation.all.model

import com.wire.android.ui.main.conversation.model.Conversation

data class NewActivity(
    val eventType: EventType,
    val conversation: Conversation,
)

sealed class EventType {
    data class UnreadMessage(val unreadMessageCount: Int) : EventType()
    object UnreadMention : EventType()
    object UnreadReply : EventType()
    object MissedCall : EventType()
}
