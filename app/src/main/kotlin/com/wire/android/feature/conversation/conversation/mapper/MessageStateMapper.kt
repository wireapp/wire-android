package com.wire.android.feature.conversation.conversation.mapper

import com.wire.android.feature.conversation.conversation.MessageState
import com.wire.android.feature.conversation.conversation.Default
import com.wire.android.feature.conversation.conversation.Deleted
import com.wire.android.feature.conversation.conversation.FailedRead
import com.wire.android.feature.conversation.conversation.Sent
import com.wire.android.feature.conversation.conversation.Delivered
import com.wire.android.feature.conversation.conversation.Failed
import com.wire.android.feature.conversation.conversation.Pending

class MessageStateMapper {

    fun fromStringValue(state: String): MessageState =
        when (state) {
            "sent" -> Sent
            "pending" -> Pending
            "delivered" -> Delivered
            "failed" -> Failed
            "failed_read" -> FailedRead
            "deleted" -> Deleted
            else -> Default
        }

    fun fromValueToString(value: MessageState): String =
        when (value) {
            Sent -> "sent"
            Pending -> "pending"
            Delivered -> "delivered"
            Failed -> "failed"
            FailedRead -> "failed_read"
            Deleted -> "deleted"
            else -> "default"
        }
}
