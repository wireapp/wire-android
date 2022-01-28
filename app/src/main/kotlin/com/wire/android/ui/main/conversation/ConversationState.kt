package com.wire.android.ui.main.conversation

import com.wire.android.ui.main.conversation.model.Conversation
import com.wire.android.ui.main.conversation.model.ConversationFolder
import com.wire.android.ui.main.conversation.model.NewActivity
import com.wire.android.ui.main.conversation.model.Call
import com.wire.android.ui.main.conversation.model.Mention

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
