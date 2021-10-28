package com.wire.android.feature.conversation.data

import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.local.ConversationEntity
import com.wire.android.feature.conversation.data.remote.ConversationResponse
import com.wire.android.feature.conversation.members.datasources.local.ConversationMemberEntity

class ConversationMapper(private val conversationTypeMapper: ConversationTypeMapper) {

    fun fromConversationResponseListToEntityList(responseList: List<ConversationResponse>): List<ConversationEntity> =
        responseList.map {
            ConversationEntity(id = it.id, name = it.name, type = it.type)
        }

    fun fromConversationResponseListToConversationMembers(responseList: List<ConversationResponse>): List<ConversationMemberEntity> =
        responseList.flatMap { conversation ->
            val memberIds = (conversation.members.otherMembers + conversation.members.self)
                .map { it.userId }.filter { it.isNotEmpty() }
            memberIds.map {
                ConversationMemberEntity(conversationId = conversation.id, contactId = it)
            }
        }

    fun fromEntity(entity: ConversationEntity) = Conversation(
        id = entity.id,
        name = entity.name,
        type = conversationTypeMapper.fromIntValue(entity.type)
    )

    fun toEntityList(conversationList: List<Conversation>) : List<ConversationEntity> =
        conversationList.map { toEntity(it) }

    private fun toEntity(conversation: Conversation) = ConversationEntity(
        id = conversation.id,
        name = conversation.name,
        type = conversationTypeMapper.toIntValue(conversation.type)
    )
}
