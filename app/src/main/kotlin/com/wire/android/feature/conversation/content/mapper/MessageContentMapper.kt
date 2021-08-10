package com.wire.android.feature.conversation.content.mapper

import com.waz.model.Messages
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
