package com.wire.android.feature.conversation.data.local

import com.wire.android.feature.conversation.ConversationID

class ConversationCache {

    private var currentOpenedConversationId: ConversationID? = null

    fun currentOpenedConversationId() = currentOpenedConversationId

    fun resetCurrentConversationID() {
        currentOpenedConversationId = null
    }

    fun updateConversationId(conversationId: ConversationID) {
        currentOpenedConversationId = conversationId
    }
}
