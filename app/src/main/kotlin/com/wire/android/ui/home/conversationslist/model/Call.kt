package com.wire.android.ui.home.conversationslist.model

import androidx.annotation.DrawableRes
import com.wire.android.R

//TODO: This could be a Long timestamp,
// waiting for the Kalium back-end to make decision
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
