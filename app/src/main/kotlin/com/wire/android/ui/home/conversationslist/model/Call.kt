package com.wire.android.ui.home.conversationslist.model

import androidx.annotation.DrawableRes
import com.wire.android.R

data class Call(
    val callInfo: CallInfo,
    override val conversationType: ConversationType
) : Conversation()

data class CallTime(
    val date: String,
    val time: String
) {
    fun toLabel(): String {
        return "$date, $time"
    }
}

data class CallInfo(
    val callTime: CallTime,
    val callEvent: CallEvent
)

enum class CallEvent(
    @DrawableRes val drawableResourceId: Int
) {
    MissedCall(R.drawable.ic_missed_call),
    OutgoingCall(R.drawable.ic_outgoing_call),
    NoAnswerCall(R.drawable.ic_no_answer_call);
}
