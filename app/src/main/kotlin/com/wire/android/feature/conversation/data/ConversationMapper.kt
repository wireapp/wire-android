package com.wire.android.feature.conversation.data

import com.wire.android.core.extension.EMPTY
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.remote.ConversationsResponse
import com.wire.android.feature.conversation.list.datasources.local.ConversationEntity

class ConversationMapper {

    fun fromConversationResponseToEntityList(response: ConversationsResponse): List<ConversationEntity> =
        response.conversations.map {
            ConversationEntity(id = it.id, name = it.name ?: String.EMPTY) //TODO consider null name on db
        }

    fun fromEntity(entity: ConversationEntity) = Conversation(
        id = entity.id,
        name = entity.name
    )
}
