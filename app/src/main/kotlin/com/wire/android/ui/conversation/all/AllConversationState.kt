package com.wire.android.ui.conversation.all

import com.wire.android.ui.conversation.all.model.Conversation
import com.wire.android.ui.conversation.all.model.ConversationFolder
import com.wire.android.ui.conversation.all.model.NewActivity

data class AllConversationState(
    val newActivities: List<NewActivity> = emptyList(),
    val conversations: Map<ConversationFolder, List<Conversation>> = emptyMap()
)


