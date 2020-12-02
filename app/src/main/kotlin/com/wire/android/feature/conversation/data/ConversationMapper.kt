package com.wire.android.feature.conversation.data

import com.wire.android.core.extension.EMPTY
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.remote.ConversationsResponse
import com.wire.android.feature.conversation.list.datasources.local.ConversationEntity
import com.wire.android.feature.conversation.members.datasources.local.ConversationMemberEntity

class ConversationMapper {

    fun fromConversationResponseToEntityList(response: ConversationsResponse): List<ConversationEntity> =
        response.conversations.map {
            ConversationEntity(id = it.id, name = it.name ?: String.EMPTY) //TODO consider null name on db
        }

    fun fromConversationResponseToConversationMembers(response: ConversationsResponse): List<ConversationMemberEntity> =
        response.conversations.flatMap { conversation ->
            val memberIds = conversation.members.otherMembers.map { it.userId }.filter { it.isNotEmpty() }
            memberIds.map {
                ConversationMemberEntity(conversationId = conversation.id, contactId = it)
            }
        }

    fun fromEntity(entity: ConversationEntity) = Conversation(
        id = entity.id,
        name = entity.name
    )
}
