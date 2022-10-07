package com.wire.android.ui.home.conversations.details.menu

import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

@Suppress("TooManyFunctions")
interface GroupConversationDetailsBottomSheetEventsHandler {
    fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus)
    fun onAddConversationToFavourites(conversationId: ConversationId? = null)
    fun onMoveConversationToFolder(conversationId: ConversationId? = null)
    fun onMoveConversationToArchive(conversationId: ConversationId? = null)
    fun onClearConversationContent(conversationId: ConversationId? = null)

    companion object {
        @Suppress("TooManyFunctions")
        val PREVIEW = object : GroupConversationDetailsBottomSheetEventsHandler {
            override fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus) {}
            override fun onAddConversationToFavourites(conversationId: ConversationId?) {}
            override fun onMoveConversationToFolder(conversationId: ConversationId?) {}
            override fun onMoveConversationToArchive(conversationId: ConversationId?) {}
            override fun onClearConversationContent(conversationId: ConversationId?) {}
        }
    }
}
