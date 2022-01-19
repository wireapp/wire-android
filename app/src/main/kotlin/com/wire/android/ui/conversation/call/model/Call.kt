package com.wire.android.ui.conversation.call.model

import com.wire.android.R
import com.wire.android.ui.conversation.all.model.Conversation

data class Call(val callInfo: CallInfo, val conversation: Conversation)

data class CallTime(val date: String, val time: String) {
    fun toLabel(): String {
        return "$date, $time"
    }
}

data class CallInfo(val callTime: CallTime, val callEvent: CallEvent)

enum class CallEvent(val drawableResourceId: Int) {
    MissedCall(R.drawable.ic_missed_call), OutgoingCall(R.drawable.ic_outgoing_call), NoAnswerCall(R.drawable.ic_no_answer_call)
}
