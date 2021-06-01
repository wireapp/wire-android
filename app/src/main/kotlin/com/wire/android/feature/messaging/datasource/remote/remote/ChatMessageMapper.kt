package com.wire.android.feature.messaging.datasource.remote.remote

import com.google.protobuf.MessageLite
import com.waz.model.Messages
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.feature.messaging.ChatMessage
import com.wire.android.feature.messaging.MessageContent
import com.wire.android.feature.messaging.datasource.remote.remote.mapper.ContentMapper
import com.wire.android.feature.messaging.datasource.remote.remote.mapper.TextMessageMapper

class ChatMessageMapper(private val textMapper: TextMessageMapper) {

    fun fromRawByteArray(byteArray: ByteArray): Either<Failure, ChatMessage<*>> =
        fromProtoBuf(Messages.GenericMessage.parseFrom(byteArray))

    fun fromProtoBuf(genericProto: Messages.GenericMessage): Either<Failure, ChatMessage<*>> {
        val uid = genericProto.messageId

        fun <R : MessageContent, T> messageFromMapper(mapper: ContentMapper<R, T>, rawContent: T): Either<Failure, ChatMessage<R>> =
            mapper.fromProtoBuf(rawContent).map { ChatMessage(uid, it) }

        return when (genericProto.contentCase.number) {
            Messages.GenericMessage.TEXT_FIELD_NUMBER -> messageFromMapper(textMapper, genericProto.text)
            else -> TODO("Different message types yet implemented")
        }
    }

    fun toProtoBuf(chatMessage: ChatMessage<*>): Either<Failure, MessageLite> {
        return when (val content = chatMessage.content) {
            is MessageContent.Text -> textMapper.toProtoBuf(content)
            else -> TODO("Different message types not yet implemented")
        }
    }
}
