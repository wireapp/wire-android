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

package com.wire.android.ui.home.conversationslist.model

import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversationslist.common.UserInfoLabel
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.isTeammate

sealed class ConversationItem {
    abstract val conversationId: ConversationId
    abstract val mutedStatus: MutedConversationStatus
    abstract val isLegalHold: Boolean
    abstract val lastMessageContent: UILastMessageContent?
    abstract val badgeEventType: BadgeEventType
    abstract val teamId: TeamId?
    abstract val isArchived: Boolean

    val isTeamConversation get() = teamId != null

    data class GroupConversation(
        val groupName: String,
        val hasOnGoingCall: Boolean = false,
        val isSelfUserCreator: Boolean = false,
        val selfMemberRole: Conversation.Member.Role?,
        val isSelfUserMember: Boolean = true,
        override val conversationId: ConversationId,
        override val mutedStatus: MutedConversationStatus,
        override val isLegalHold: Boolean = false,
        override val lastMessageContent: UILastMessageContent?,
        override val badgeEventType: BadgeEventType,
        override val teamId: TeamId?,
        override val isArchived: Boolean,
    ) : ConversationItem()

    data class PrivateConversation(
        val userAvatarData: UserAvatarData,
        val conversationInfo: ConversationInfo,
        val userId: UserId,
        val blockingState: BlockingState,
        override val conversationId: ConversationId,
        override val mutedStatus: MutedConversationStatus,
        override val isLegalHold: Boolean = false,
        override val lastMessageContent: UILastMessageContent?,
        override val badgeEventType: BadgeEventType,
        override val teamId: TeamId?,
        override val isArchived: Boolean
    ) : ConversationItem()

    data class ConnectionConversation(
        val userAvatarData: UserAvatarData,
        val conversationInfo: ConversationInfo,
        override val conversationId: ConversationId,
        override val mutedStatus: MutedConversationStatus,
        override val isLegalHold: Boolean = false,
        override val lastMessageContent: UILastMessageContent?,
        override val badgeEventType: BadgeEventType,
        override val isArchived: Boolean = false,
    ) : ConversationItem() {
        override val teamId: TeamId? = null
    }
}

data class ConversationInfo(
    val name: String,
    val membership: Membership = Membership.None,
    val isSenderUnavailable: Boolean = false
)

enum class BlockingState {
    CAN_NOT_BE_BLOCKED, // we should not be able to block our own team-members
    BLOCKED,
    NOT_BLOCKED
}

val OtherUser.BlockState: BlockingState
    get() =
        when {
            userType.isTeammate() -> BlockingState.CAN_NOT_BE_BLOCKED
            connectionStatus == ConnectionState.BLOCKED -> BlockingState.BLOCKED
            else -> BlockingState.NOT_BLOCKED
        }

fun ConversationItem.PrivateConversation.toUserInfoLabel() =
    UserInfoLabel(
        labelName = conversationInfo.name,
        isLegalHold = isLegalHold,
        membership = conversationInfo.membership,
        unavailable = conversationInfo.isSenderUnavailable
    )

fun ConversationItem.ConnectionConversation.toUserInfoLabel() =
    UserInfoLabel(
        labelName = conversationInfo.name,
        isLegalHold = isLegalHold,
        membership = conversationInfo.membership
    )
