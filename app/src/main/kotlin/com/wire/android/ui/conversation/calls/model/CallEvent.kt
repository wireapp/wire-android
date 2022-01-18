package com.wire.android.ui.conversation.calls.model

import com.wire.android.ui.conversation.all.model.Conversation

data class CallEvent(val dateLabel: DateLabel, val conversation: Conversation)

data class DateLabel(val date: String, val time: String)
