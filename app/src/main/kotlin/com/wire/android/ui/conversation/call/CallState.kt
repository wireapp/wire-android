package com.wire.android.ui.conversation.call

import com.wire.android.ui.conversation.call.model.CallEvent

data class CallState(val missedCalls: List<CallEvent> = emptyList(), val callHistory: List<CallEvent> = emptyList())



