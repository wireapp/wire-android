package com.wire.android.feature.conversation.data

import com.wire.android.core.extension.EMPTY
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.local.ConversationEntity
import com.wire.android.feature.conversation.data.remote.ConversationResponse
import com.wire.android.feature.conversation.members.datasources.local.ConversationMemberEntity

class ConversationMapper {

    fun fromConversationResponseListToEntityList(responseList: List<ConversationResponse>): List<ConversationEntity> =
        responseList.map {
            ConversationEntity(id = it.id, name = it.name ?: String.EMPTY) //TODO consider null name on db
        }

    fun fromConversationResponseListToConversationMembers(responseList: List<ConversationResponse>): List<ConversationMemberEntity> =
        responseList.flatMap { conversation ->
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
