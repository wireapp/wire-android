package com.wire.android.core.storage.cache

import com.wire.android.core.extension.EMPTY

class InMemoryCache {

    private var currentOpenedConversationId = String.EMPTY

    fun currentOpenedConversationId() = currentOpenedConversationId

    fun updateConversationId(conversationId: String) {
        currentOpenedConversationId = conversationId
    }
}
