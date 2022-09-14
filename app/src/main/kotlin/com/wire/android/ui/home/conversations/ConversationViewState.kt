package com.wire.android.ui.home.conversations

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import okio.Path
import com.wire.kalium.logic.data.id.QualifiedID as ConversationId

data class ConversationViewState(
    val snackbarMessage: ConversationSnackbarMessages? = null,
    val userTeam: Team? = null,
    val isFileSharingEnabled: Boolean = true,
    val securityClassificationType: SecurityClassificationType = SecurityClassificationType.NONE
)

sealed class ConversationAvatar {
    object None : ConversationAvatar()
    data class OneOne(val avatarAsset: UserAvatarAsset?, val status: UserAvailabilityStatus) : ConversationAvatar()
    data class Group(val conversationId: ConversationId) : ConversationAvatar()
}

sealed class DownloadedAssetDialogVisibilityState {
    object Hidden : DownloadedAssetDialogVisibilityState()
    data class Displayed(val assetName: String, val assetDataPath: Path, val assetSize: Long, val messageId: String) :
        DownloadedAssetDialogVisibilityState()
}

sealed class ConversationDetailsData {
    object None : ConversationDetailsData()
    data class OneOne(val otherUserId: UserId, val connectionState: ConnectionState) : ConversationDetailsData()
    data class Group(val conversationId: QualifiedID) : ConversationDetailsData()
}
