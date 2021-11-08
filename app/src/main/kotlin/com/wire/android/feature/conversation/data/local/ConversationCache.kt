package com.wire.android.feature.conversation.data.local

import com.wire.android.feature.conversation.ConversationID

class ConversationCache {

    private var currentOpenedConversationId = ConversationID.blankID()

    fun currentOpenedConversationId() = currentOpenedConversationId

    fun updateConversationId(conversationId: ConversationID) {
        currentOpenedConversationId = conversationId
    }
}
