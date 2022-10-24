package com.wire.android.ui.home.conversations.info

import com.wire.android.ui.home.conversations.ConversationAvatar
import com.wire.android.ui.home.conversations.ConversationDetailsData
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation

data class ConversationInfoViewState(
    val conversationName: UIText = UIText.DynamicString(""),
    val conversationDetailsData: ConversationDetailsData = ConversationDetailsData.None,
    val conversationAvatar: ConversationAvatar = ConversationAvatar.None,
    val isUserBlocked: Boolean = false,
    val hasUserPermissionToEdit : Boolean = false,
    val conversationType: Conversation.Type = Conversation.Type.ONE_ON_ONE
)
