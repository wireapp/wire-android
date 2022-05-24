package com.wire.android.ui.home.conversations

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.home.conversations.model.MessageViewWrapper
import com.wire.kalium.logic.data.team.Team

data class ConversationViewState(
    val conversationName: String = "",
    val conversationAvatar: ConversationAvatar = ConversationAvatar.None,
    val messages: List<MessageViewWrapper> = emptyList(),
    val onSnackbarMessage: ConversationSnackbarMessages? = null,
    val messageText: String = "",
    val downloadedAssetDialogState: DownloadedAssetDialogVisibilityState = DownloadedAssetDialogVisibilityState.Hidden,
    val userTeam: Team? = null
)

sealed class ConversationAvatar {
    object None : ConversationAvatar()
    class OneOne(val avatarAsset: UserAvatarAsset?) : ConversationAvatar()
    class Group(val groupColorValue: Long) : ConversationAvatar()
}

sealed class DownloadedAssetDialogVisibilityState {
    object Hidden : DownloadedAssetDialogVisibilityState()
    class Displayed (val assetName: String?, val assetData: ByteArray, val messageId: String) : DownloadedAssetDialogVisibilityState()
}
