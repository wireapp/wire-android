package com.wire.android.feature.conversation.content.mapper

import com.wire.android.feature.conversation.content.MessageType
import com.wire.android.feature.conversation.content.Text
import com.wire.android.feature.conversation.content.Unknown

class MessageTypeMapper {
    fun fromStringValue(type: String): MessageType =
        when (type) {
            "text" -> Text
            else -> Unknown
        }

    fun fromValueToString(value: MessageType): String =
        when (value) {
            Text -> "text"
            else -> "unknown"
        }
}
