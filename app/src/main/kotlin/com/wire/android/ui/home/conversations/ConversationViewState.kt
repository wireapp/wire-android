/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.home.conversations

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.kalium.logic.configuration.SelfDeletingMessagesStatus
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import com.wire.kalium.logic.data.id.QualifiedID as ConversationId

data class ConversationViewState(
    val userTeam: Team? = null,
    val isFileSharingEnabled: Boolean = true,
    val securityClassificationType: SecurityClassificationType = SecurityClassificationType.NONE,
    val selfDeletingMessagesStatus: SelfDeletingMessagesStatus = SelfDeletingMessagesStatus(false, null, null),
)

sealed class ConversationAvatar {
    object None : ConversationAvatar()
    data class OneOne(val avatarAsset: UserAvatarAsset?, val status: UserAvailabilityStatus) : ConversationAvatar()
    data class Group(val conversationId: ConversationId) : ConversationAvatar()
}

sealed class DownloadedAssetDialogVisibilityState {
    object Hidden : DownloadedAssetDialogVisibilityState()
    data class Displayed(val assetData: AssetBundle, val messageId: String) : DownloadedAssetDialogVisibilityState()
}

sealed class ConversationDetailsData {
    object None : ConversationDetailsData()
    data class OneOne(val otherUserId: UserId, val connectionState: ConnectionState, val isBlocked: Boolean, val isDeleted: Boolean) :
        ConversationDetailsData()

    data class Group(val conversationId: QualifiedID) : ConversationDetailsData()
}
