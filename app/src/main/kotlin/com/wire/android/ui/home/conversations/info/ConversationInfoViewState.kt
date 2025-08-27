/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.home.conversations.info

import com.wire.android.model.ImageAsset
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId

data class ConversationInfoViewState(
    val conversationId: QualifiedID,
    val conversationName: UIText = UIText.DynamicString(""),
    val conversationDetailsData: ConversationDetailsData = ConversationDetailsData.None(null),
    val conversationAvatar: ConversationAvatar = ConversationAvatar.None,
    val hasUserPermissionToEdit: Boolean = false,
    val conversationType: Conversation.Type = Conversation.Type.OneOnOne,
    val protocolInfo: Conversation.ProtocolInfo? = null,
    val mlsVerificationStatus: Conversation.VerificationStatus? = null,
    val proteusVerificationStatus: Conversation.VerificationStatus? = null,
    val legalHoldStatus: Conversation.LegalHoldStatus = Conversation.LegalHoldStatus.UNKNOWN,
    val accentId: Int = -1,
    val isWireCellEnabled: Boolean = false,
    val notFound: Boolean = false,
    val isBubble: Boolean = false
) {
    val showHistoryLoadingIndicator: Boolean get() = conversationType == Conversation.Type.Group.Channel
}

sealed class ConversationDetailsData(open val conversationProtocol: Conversation.ProtocolInfo?) {
    data class None(override val conversationProtocol: Conversation.ProtocolInfo?) : ConversationDetailsData(conversationProtocol)
    data class OneOne(
        override val conversationProtocol: Conversation.ProtocolInfo?,
        val otherUserId: UserId,
        val otherUserName: String?,
        val connectionState: ConnectionState,
        val isBlocked: Boolean,
        val isDeleted: Boolean
    ) : ConversationDetailsData(conversationProtocol)

    data class Group(
        override val conversationProtocol: Conversation.ProtocolInfo?,
        val conversationId: QualifiedID
    ) : ConversationDetailsData(conversationProtocol)
}

sealed interface ConversationAvatar {
    data object None : ConversationAvatar
    data class OneOne(val avatarAsset: ImageAsset.UserAvatarAsset?, val status: UserAvailabilityStatus) : ConversationAvatar
    sealed interface Group : ConversationAvatar {
        val conversationId: QualifiedID

        data class Regular(override val conversationId: QualifiedID) : Group
        data class Channel(override val conversationId: QualifiedID, val isPrivate: Boolean) : Group
    }
}
