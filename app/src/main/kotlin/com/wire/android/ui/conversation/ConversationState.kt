package com.wire.android.ui.conversation

import com.wire.android.ui.conversation.model.Conversation

data class ConversationState(
    val conversations: List<Conversation> = emptyList()
)
