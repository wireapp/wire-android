package com.wire.android.feature.conversation.content.mapper

import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.datasources.local.MessageEntity

class MessageMapper(
    private val messageTypeMapper: MessageTypeMapper,
    private val messageStateMapper: MessageStateMapper,
    private val messageTimeMapper: MessageTimeMapper
) {

    fun fromEntityToMessage(messageEntity: MessageEntity) = with(messageEntity) {
        val messageType = messageTypeMapper.fromStringValue(type)
        val messageState = messageStateMapper.fromStringValue(state)
        Message(id, conversationId, content, messageType, messageState, messageTimeMapper.fromStringToOffsetDateTime(time))
    }

    fun fromMessageToEntity(message: Message) = with(message) {
        val messageType = messageTypeMapper.fromValueToString(type)
        val messageState = messageStateMapper.fromValueToString(state)
        MessageEntity(id, conversationId, messageType, content, messageState, messageTimeMapper.fromOffsetDateTimeToString(time))
    }
}
