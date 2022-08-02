package com.wire.android.ui.home.conversationslist.model

sealed class EventType {
    data class UnreadMessage(val unreadMessageCount: Int) : EventType()
    object UnreadMention : EventType()
    object UnreadReply : EventType()
    object MissedCall : EventType()
    object ReceivedConnectionRequest : EventType()
    object SentConnectRequest : EventType()
    object Blocked : EventType()
}
