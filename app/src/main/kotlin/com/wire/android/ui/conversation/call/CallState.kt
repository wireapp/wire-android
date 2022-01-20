package com.wire.android.ui.conversation.call

import com.wire.android.ui.conversation.call.model.Call

data class CallState(val missedCalls: List<Call> = emptyList(), val callHistory: List<Call> = emptyList())



