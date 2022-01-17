package com.wire.android.ui.conversation

import com.wire.android.ui.conversation.model.Conversation
import com.wire.android.ui.conversation.model.ConversationFolder
import com.wire.android.ui.conversation.model.NewActivity

data class ConversationState(
    val newConservationActivities: List<NewActivity> = emptyList(),
    val conversations: Map<ConversationFolder, List<Conversation>> = emptyMap()
)


