package com.wire.android.ui.conversation.mention

import com.wire.android.ui.conversation.mention.model.Mention

data class MentionState(
    val unreadMentions: List<Mention> = emptyList(),
    val allMentions: List<Mention> = emptyList()
)
