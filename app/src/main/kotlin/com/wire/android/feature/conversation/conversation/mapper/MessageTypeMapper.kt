package com.wire.android.feature.conversation.conversation.mapper

import com.wire.android.feature.conversation.conversation.MessageType
import com.wire.android.feature.conversation.conversation.Text
import com.wire.android.feature.conversation.conversation.Unknown

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
