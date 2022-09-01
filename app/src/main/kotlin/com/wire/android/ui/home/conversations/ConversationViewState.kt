package com.wire.android.ui.home.conversations

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import okio.Path
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.id.QualifiedID as ConversationId
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

data class ConversationViewState(
    val conversationName: UIText = UIText.DynamicString(""),
    val conversationDetailsData: ConversationDetailsData = ConversationDetailsData.None,
    val conversationAvatar: ConversationAvatar = ConversationAvatar.None,
    val messages: List<UIMessage> = emptyList(),
    val onSnackbarMessage: ConversationSnackbarMessages? = null,
    val messageText: String = "",
    val downloadedAssetDialogState: DownloadedAssetDialogVisibilityState = DownloadedAssetDialogVisibilityState.Hidden,
    val userTeam: Team? = null,
    val isFileSharingEnabled: Boolean = true,
    val hasOngoingCall: Boolean = false,
    val hasEstablishedCall: Boolean = false,
    val lastUnreadMessage : UIMessage? = null,
    val securityClassificationType: SecurityClassificationType = SecurityClassificationType.CLASSIFIED
)

sealed class ConversationAvatar {
    object None : ConversationAvatar()
    class OneOne(val avatarAsset: UserAvatarAsset?, val status: UserAvailabilityStatus) : ConversationAvatar()
    class Group(val conversationId: ConversationId) : ConversationAvatar()
}

sealed class DownloadedAssetDialogVisibilityState {
    object Hidden : DownloadedAssetDialogVisibilityState()
    class Displayed(val assetName: String, val assetDataPath: Path, val assetSize: Long, val messageId: String) :
        DownloadedAssetDialogVisibilityState()
}

sealed class ConversationDetailsData {
    object None : ConversationDetailsData()
    data class OneOne(val otherUserId: UserId, val connectionState: ConnectionState) : ConversationDetailsData()
    data class Group(val conversationId: QualifiedID) : ConversationDetailsData()
}
