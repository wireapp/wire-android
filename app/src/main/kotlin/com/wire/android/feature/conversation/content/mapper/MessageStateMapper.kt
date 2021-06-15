package com.wire.android.feature.conversation.content.mapper

import com.wire.android.feature.conversation.content.MessageState
import com.wire.android.feature.conversation.content.Default
import com.wire.android.feature.conversation.content.Deleted
import com.wire.android.feature.conversation.content.Sent
import com.wire.android.feature.conversation.content.Delivered
import com.wire.android.feature.conversation.content.Failed
import com.wire.android.feature.conversation.content.Pending

class MessageStateMapper {

    fun fromStringValue(state: String): MessageState =
        when (state) {
            "sent" -> Sent
            "pending" -> Pending
            "delivered" -> Delivered
            "failed" -> Failed
            "deleted" -> Deleted
            else -> Default
        }

    fun fromValueToString(value: MessageState): String =
        when (value) {
            Sent -> "sent"
            Pending -> "pending"
            Delivered -> "delivered"
            Failed -> "failed"
            Deleted -> "deleted"
            else -> "default"
        }
}
