package com.wire.android.ui.home.conversationslist.model

import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationTypeDetail
import com.wire.kalium.logic.data.id.ConversationId

data class GroupDialogState(val conversationId: ConversationId, val conversationName: String)

data class DialogState(val conversationId: ConversationId, val conversationName: String, val conversation: ConversationTypeDetail)
