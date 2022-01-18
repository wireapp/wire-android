package com.wire.android.ui.conversation.calls

import com.wire.android.ui.conversation.calls.model.CallEvent

data class CallState(val callEvents: List<CallEvent> = emptyList(), val callHistory: List<CallEvent> = emptyList())



