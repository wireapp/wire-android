package com.wire.android.ui.home.conversations

import com.wire.android.ui.home.conversations.model.MessageViewWrapper

data class ConversationViewState(
    val conversationName: String = "",
    val messages: List<MessageViewWrapper> = emptyList(),
    val messageText: String = ""
)
