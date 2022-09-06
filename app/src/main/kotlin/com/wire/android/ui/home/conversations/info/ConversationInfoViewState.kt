package com.wire.android.ui.home.conversations.info

import com.wire.android.ui.home.conversations.ConversationAvatar
import com.wire.android.ui.home.conversations.ConversationDetailsData
import com.wire.android.util.ui.UIText

data class ConversationInfoViewState(
    val conversationName: UIText = UIText.DynamicString(""),
    val conversationDetailsData: ConversationDetailsData = ConversationDetailsData.None,
    val conversationAvatar: ConversationAvatar = ConversationAvatar.None,
)
