package com.wire.android.feature.conversation.data.local

import com.wire.android.core.extension.EMPTY

class ConversationCache {

    private var currentOpenedConversationId = String.EMPTY

    fun currentOpenedConversationId() = currentOpenedConversationId

    fun updateConversationId(conversationId: String) {
        currentOpenedConversationId = conversationId
    }
}
