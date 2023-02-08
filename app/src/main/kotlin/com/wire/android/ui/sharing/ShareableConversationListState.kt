package com.wire.android.ui.sharing

import com.wire.android.ui.home.conversationslist.model.ConversationItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ShareableConversationListState(
    val initialConversations: List<ConversationItem> = persistentListOf(),
    val searchQuery: String = "",
    val hasNoConversations: Boolean = false,
    val searchResult: List<ConversationItem> = persistentListOf(),
    val conversationsAddedToGroup: ImmutableList<ConversationItem> = persistentListOf(),
)
