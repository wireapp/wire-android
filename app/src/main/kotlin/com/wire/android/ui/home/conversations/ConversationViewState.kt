package com.wire.android.ui.home.conversations

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.home.conversations.model.MessageViewWrapper
import com.wire.kalium.logic.data.team.Team

data class ConversationViewState(
    val conversationName: String = "",
    val conversationAvatar: ConversationAvatar = ConversationAvatar.None,
    val messages: List<MessageViewWrapper> = emptyList(),
    val onError: ConversationErrors? = null,
    val messageText: String = "",
    val userTeam: Team? = null
)

sealed class ConversationAvatar {
    object None : ConversationAvatar()
    class OneOne(val avatarAsset: UserAvatarAsset?) : ConversationAvatar()
    class Group(val groupColorValue: Long) : ConversationAvatar()
}
