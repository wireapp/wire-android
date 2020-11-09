package com.wire.android.feature.conversation.data

import com.wire.android.feature.conversation.data.remote.ConversationsResponse
import com.wire.android.feature.conversation.list.usecase.Conversation

class ConversationMapper {
    fun fromConversationResponse(conversationResponse: ConversationsResponse): List<Conversation> = emptyList()
}
