package com.wire.android.ui.home.conversationslist

import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationMissedCall
import com.wire.android.ui.home.conversationslist.model.ConversationUnreadMention
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.NewActivity
import java.util.UUID

data class ConversationListState(
    val newActivities: List<NewActivity> = emptyList(),
    val conversations: Map<ConversationFolder, List<GeneralConversation>> = emptyMap(),
    val missedCalls: List<ConversationMissedCall> = emptyList(),
    val callHistory: List<ConversationMissedCall> = emptyList(),
    val unreadMentions: List<ConversationUnreadMention> = emptyList(),
    val allMentions: List<ConversationUnreadMention> = emptyList(),
    val newActivityCount: Int = 0,
    val missedCallsCount: Int = 0,
    val unreadMentionsCount: Int = 0
)

/**
 * We are adding a [randomEventIdentifier] as [UUID], so the error can be discarded every time after being generated.
 */
sealed class ConversationOperationErrorState(private val randomEventIdentifier: UUID) {
    class MutingOperationErrorState : ConversationOperationErrorState(UUID.randomUUID())
}
