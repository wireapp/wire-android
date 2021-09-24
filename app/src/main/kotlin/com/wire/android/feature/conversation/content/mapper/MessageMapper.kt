package com.wire.android.feature.conversation.content.mapper

import com.wire.android.core.crypto.model.CryptoClientId
import com.wire.android.core.crypto.model.CryptoSessionId
import com.wire.android.core.crypto.model.EncryptedMessage
import com.wire.android.core.crypto.model.PlainMessage
import com.wire.android.core.date.DateStringMapper
import com.wire.android.core.events.Event
import com.wire.android.feature.conversation.content.EncryptedMessageEnvelope
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.Sent
import com.wire.android.feature.conversation.content.datasources.local.MessageEntity
import com.wire.android.shared.user.QualifiedId

class MessageMapper(
    private val messageContentMapper: MessageContentMapper,
    private val messageStateMapper: MessageStateMapper,
    private val dateStringMapper: DateStringMapper
) {

    fun fromEntityToMessage(messageEntity: MessageEntity) = with(messageEntity) {
        val messageContent = messageContentMapper.fromStringToContent(type, content)
        val messageState = messageStateMapper.fromStringValue(state)
        Message(
            id = id,
            conversationId = conversationId,
            senderUserId = senderUserId,
            clientId = null,
            content = messageContent,
            state = messageState,
            time = dateStringMapper.fromStringToOffsetDateTime(time),
            isRead = isRead
        )
    }

    fun fromMessageToEntity(message: Message) = with(message) {
        val messageType = messageContentMapper.fromContentToStringType(content)
        val messageContent = messageContentMapper.fromContentToString(content)
        val messageState = messageStateMapper.fromValueToString(state)
        MessageEntity(
            id,
            conversationId,
            senderUserId,
            messageType,
            messageContent,
            messageState,
            dateStringMapper.fromOffsetDateTimeToString(time),
            isRead
        )
    }

    fun toDecryptedMessage(encryptedEnvelope: EncryptedMessageEnvelope, plainMessage: PlainMessage): Message = Message(
        encryptedEnvelope.id,
        encryptedEnvelope.conversationId,
        encryptedEnvelope.senderUserId,
        encryptedEnvelope.clientId,
        messageContentMapper.fromProtobufData(plainMessage.data),
        Sent,
        encryptedEnvelope.time,
        false
    )

    fun fromMessageEventToEncryptedMessageEnvelope(event: Event.Conversation.MessageEvent) =
        EncryptedMessageEnvelope(
            event.id,
            event.conversationId,
            event.senderUserId,
            event.senderClientId,
            event.content,
            dateStringMapper.fromStringToOffsetDateTime(event.time)
        )

    fun cryptoSessionFromEncryptedEnvelope(message: EncryptedMessageEnvelope) =
        CryptoSessionId(QualifiedId(TEMP_HARDCODED_DOMAIN, message.senderUserId), CryptoClientId(message.clientId!!))

    fun encryptedMessageFromDecodedContent(decodedContent: ByteArray) = EncryptedMessage(decodedContent)

    companion object {
        //TODO Remove hardcoded ID
        private const val TEMP_HARDCODED_DOMAIN = "domain"
    }
}
