package com.wire.android.ui.home.conversationslist

import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.NewActivity

data class ConversationListState(
    val newActivities: List<NewActivity> = emptyList(),
    val conversations: Map<ConversationFolder, List<ConversationItem>> = emptyMap(),
    val missedCalls: List<ConversationItem> = emptyList(),
    val callHistory: List<ConversationItem> = emptyList(),
    val unreadMentions: List<ConversationItem> = emptyList(),
    val allMentions: List<ConversationItem> = emptyList(),
    val newActivityCount: Int = 0,
    val missedCallsCount: Int = 0,
    val unreadMentionsCount: Int = 0,
    val blockUserDialogSate: BlockUserDialogState? = null
)
