/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home.conversationslist.model

import androidx.annotation.DrawableRes
import com.wire.android.R
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId

sealed class ConversationLastEvent {
    data class Call(
        val callTime: CallTime,
        val callEvent: CallEvent
    ) : ConversationLastEvent()

    data class Mention(val mentionMessage: MentionMessage) : ConversationLastEvent()

    data class Connection(val connectionState: ConnectionState, val userId: UserId) : ConversationLastEvent()

    object None : ConversationLastEvent()
}

// TODO: This could be a Long timestamp,
// waiting for the Kalium back-end to make decision
data class CallTime(
    val date: String,
    val time: String
) {
    fun toLabel(): String {
        return "$date, $time"
    }
}

enum class CallEvent(
    @DrawableRes val drawableResourceId: Int
) {
    MissedCall(R.drawable.ic_missed_call),
    OutgoingCall(R.drawable.ic_outgoing_call),
    NoAnswerCall(R.drawable.ic_no_answer_call)
}

// We can expand this to show more detailed messages for other cases
fun ConnectionState.toMessageId(): Int = when (this) {
    ConnectionState.PENDING -> R.string.connection_pending_message
    else -> -1
}

data class MentionMessage(val message: String) {
    fun toQuote(): String = "\"$message\""
}
