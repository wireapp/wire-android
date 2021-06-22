package com.wire.android.feature.conversation.content.mapper

import com.wire.android.core.date.DateStringMapper
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.datasources.local.MessageEntity

class MessageMapper(
    private val messageTypeMapper: MessageTypeMapper,
    private val messageStateMapper: MessageStateMapper,
    private val dateStringMapper: DateStringMapper
) {

    fun fromEntityToMessage(messageEntity: MessageEntity) = with(messageEntity) {
        val messageType = messageTypeMapper.fromStringValue(type)
        val messageState = messageStateMapper.fromStringValue(state)
        Message(id, conversationId, content, messageType, messageState, dateStringMapper.fromStringToOffsetDateTime(time))
    }

    fun fromMessageToEntity(message: Message) = with(message) {
        val messageType = messageTypeMapper.fromValueToString(type)
        val messageState = messageStateMapper.fromValueToString(state)
        MessageEntity(id, conversationId, messageType, content, messageState, dateStringMapper.fromOffsetDateTimeToString(time))
    }
}
