package com.wire.android.feature.conversation.content.mapper

import com.waz.model.Messages
import com.wire.android.core.crypto.model.PlainMessage
import com.wire.android.feature.conversation.content.Content

class MessageContentMapper {

    fun fromProtobufData(protobufData: ByteArray): Content {
        val genericMessage = Messages.GenericMessage.parseFrom(protobufData)

        //TODO Handle other message types
        return if (genericMessage.hasText()) {
            Content.Text(genericMessage.text.content)
        } else {
            Content.Text("This message content is UNKNOWN")
        }
    }

    fun fromContentToPlainMessage(messageUid: String, content: Content): PlainMessage {
        val builder = Messages.GenericMessage.newBuilder()
            .setMessageId(messageUid)

        when (content) {
            is Content.Text -> {
                val text = Messages.Text.newBuilder()
                    .setContent(content.value)
                    .build()
                builder.text = text
            }
        }
        return PlainMessage(builder.build().toByteArray())
    }

    fun fromStringToContent(type: String, rawContent: String): Content = when (type) {
        "text" -> Content.Text(rawContent)
        else -> Content.Text("unknown content")
    }

    fun fromContentToStringType(content: Content): String = when (content) {
        is Content.Text -> "text"
    }

    fun fromContentToString(content: Content): String = when (content) {
        is Content.Text -> content.value
    }
}
