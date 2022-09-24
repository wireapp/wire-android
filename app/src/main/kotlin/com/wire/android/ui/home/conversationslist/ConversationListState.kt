package com.wire.android.ui.home.conversationslist

import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationItem

data class ConversationListState(
    val conversations: Map<ConversationFolder, List<ConversationItem>> = emptyMap(),
    val missedCalls: List<ConversationItem> = emptyList(),
    val callHistory: List<ConversationItem> = emptyList(),
    val unreadMentions: List<ConversationItem> = emptyList(),
    val allMentions: List<ConversationItem> = emptyList(),
    val newActivityCount: Long = 0,
    val missedCallsCount: Long = 0,
    val unreadMentionsCount: Long = 0,
    val shouldShowEmptyState: Boolean = false
)
