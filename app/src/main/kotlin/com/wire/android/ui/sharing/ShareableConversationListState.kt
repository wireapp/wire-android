package com.wire.android.ui.sharing

import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.collections.immutable.persistentListOf

data class ShareableConversationListState(
    val searchQuery: String = "",
    val hasNoConversations: Boolean = false,
    val searchResult: List<ConversationItem> = persistentListOf(),
    private val initialConversations: List<ConversationItem> = persistentListOf(),
) {
    fun findConversationById(conversationId: ConversationId): ConversationItem? =
        initialConversations.firstOrNull { it.conversationId == conversationId }
}
