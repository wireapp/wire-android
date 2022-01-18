package com.wire.android.ui.conversation.call.model

import com.wire.android.ui.conversation.all.model.Conversation

data class CallEvent(val callTime: CallTime, val conversation: Conversation)

data class CallTime(val date: String, val time: String) {
    fun toLabel(): String {
        return "$date,$time"
    }
}
