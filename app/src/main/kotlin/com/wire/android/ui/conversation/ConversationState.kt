package com.wire.android.ui.conversation

import com.wire.android.ui.conversation.model.Conversation
import com.wire.android.ui.conversation.model.ConversationFolder

data class ConversationState(
    val conversations: Map<ConversationFolder, List<Conversation>> = emptyMap()
)
