package com.wire.android.ui.home.conversations

import com.wire.android.ui.home.conversations.model.Message

data class ConversationViewState(
    val conversationName: String = "",
    val messages: List<Message> = emptyList()
)
