package com.wire.android.ui.conversation.call

import com.wire.android.ui.conversation.call.model.Call
import com.wire.android.ui.conversation.call.model.CallEvent
import com.wire.android.ui.conversation.call.model.CallInfo
import com.wire.android.ui.conversation.call.model.CallTime
import com.wire.android.ui.conversation.mockConversation

val mockCallInfo1 = CallInfo(CallTime("Today", "5:34 PM"), CallEvent.NoAnswerCall)
val mockCallInfo2 = CallInfo(CallTime("Yesterday", "1:00 PM"), CallEvent.OutgoingCall)
val mockCallInfo3 = CallInfo(CallTime("Today", "2:34 PM"), CallEvent.MissedCall)
val mockCallInfo4 = CallInfo(CallTime("Today", "5:34 PM"), CallEvent.NoAnswerCall)
val mockCallInfo5 = CallInfo(CallTime("Today", "6:59 PM"), CallEvent.NoAnswerCall)

val mockMissedCalls = listOf(
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
)

val mockCallHistory = listOf(
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo4, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo5, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo1, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo3, mockConversation),
    Call(mockCallInfo4, mockConversation),
    Call(mockCallInfo2, mockConversation),
    Call(mockCallInfo5, mockConversation),
)

