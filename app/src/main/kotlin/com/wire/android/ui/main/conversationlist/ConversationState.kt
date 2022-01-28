package com.wire.android.ui.main.conversationlist


import com.wire.android.ui.main.conversationlist.model.Call
import com.wire.android.ui.main.conversationlist.model.Conversation
import com.wire.android.ui.main.conversationlist.model.ConversationFolder
import com.wire.android.ui.main.conversationlist.model.Mention
import com.wire.android.ui.main.conversationlist.model.NewActivity

data class ConversationState(
    val newActivities: List<NewActivity> = emptyList(),
    val conversations: Map<ConversationFolder, List<Conversation>> = emptyMap(),
    val missedCalls: List<Call> = emptyList(),
    val callHistory: List<Call> = emptyList(),
    val unreadMentions: List<Mention> = emptyList(),
    val allMentions: List<Mention> = emptyList(),
    val newActivityCount: Int = 0,
    val missedCallsCount: Int = 0,
    val unreadMentionsCount: Int = 0
)
