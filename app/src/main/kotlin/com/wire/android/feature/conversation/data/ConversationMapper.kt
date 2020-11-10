package com.wire.android.feature.conversation.data

import com.wire.android.feature.conversation.data.remote.ConversationResponse
import com.wire.android.feature.conversation.data.remote.ConversationsResponse
import com.wire.android.feature.conversation.list.usecase.Conversation

class ConversationMapper {

    fun fromConversationsResponse(conversationsResponse: ConversationsResponse): List<Conversation> =
        conversationsResponse.conversationResponses.map {
            fromConversationResponse(it)
        }

    //TODO add more fields as we build up the UI
    private fun fromConversationResponse(conversationResponse: ConversationResponse): Conversation =
        Conversation(
            name = conversationResponse.name,
            id = conversationResponse.id
        )
}
