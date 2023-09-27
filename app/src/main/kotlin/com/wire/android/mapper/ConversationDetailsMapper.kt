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
 */
package com.wire.android.mapper

import com.wire.android.model.ImageAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.BlockState
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.parseConversationEventType
import com.wire.android.ui.home.conversationslist.parsePrivateConversationEventType
import com.wire.android.ui.home.conversationslist.showLegalHoldIndicator
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import javax.inject.Inject

class ConversationDetailsMapper
@Inject constructor(
    private val userTypeMapper: UserTypeMapper,
    private val wireSessionImageLoader: WireSessionImageLoader
) {

    @Suppress("LongMethod")
    fun toConversationItem(
        conversationDetails: ConversationDetails
    ): ConversationItem = when (conversationDetails) {
            is ConversationDetails.Group -> {
            ConversationItem.GroupConversation(
                groupName = conversationDetails.conversation.name.orEmpty(),
                conversationId = conversationDetails.conversation.id,
                mutedStatus = conversationDetails.conversation.mutedStatus,
                isLegalHold = conversationDetails.legalHoldStatus.showLegalHoldIndicator(),
                lastMessageContent = conversationDetails.lastMessage.toUIPreview(conversationDetails.unreadEventCount),
                badgeEventType = parseConversationEventType(
                    conversationDetails.conversation.mutedStatus,
                    conversationDetails.unreadEventCount
                ),
                hasOnGoingCall = conversationDetails.hasOngoingCall && conversationDetails.isSelfUserMember,
                isSelfUserCreator = conversationDetails.isSelfUserCreator,
                isSelfUserMember = conversationDetails.isSelfUserMember,
                teamId = conversationDetails.conversation.teamId,
                selfMemberRole = conversationDetails.selfRole
            )
        }

            is ConversationDetails.OneOne -> {
            ConversationItem.PrivateConversation(
                userAvatarData = UserAvatarData(
                    conversationDetails.otherUser.previewPicture?.let {
                        ImageAsset.UserAvatarAsset(
                            wireSessionImageLoader,
                            it
                        )
                    },
                    conversationDetails.otherUser.availabilityStatus,
                    conversationDetails.otherUser.connectionStatus
                ),
                conversationInfo = ConversationInfo(
                    name = conversationDetails.otherUser.name.orEmpty(),
                    membership = userTypeMapper.toMembership(conversationDetails.userType),
                    isSenderUnavailable = conversationDetails.otherUser.isUnavailableUser
                ),
                conversationId = conversationDetails.conversation.id,
                mutedStatus = conversationDetails.conversation.mutedStatus,
                isLegalHold = conversationDetails.legalHoldStatus.showLegalHoldIndicator(),
                lastMessageContent = conversationDetails.lastMessage.toUIPreview(conversationDetails.unreadEventCount),
                badgeEventType = parsePrivateConversationEventType(
                    conversationDetails.otherUser.connectionStatus,
                    conversationDetails.otherUser.deleted,
                    parseConversationEventType(
                        conversationDetails.conversation.mutedStatus,
                        conversationDetails.unreadEventCount
                    )
                ),
                userId = conversationDetails.otherUser.id,
                blockingState = conversationDetails.otherUser.BlockState,
                teamId = conversationDetails.otherUser.teamId
            )
        }

            is ConversationDetails.Connection -> {
            ConversationItem.ConnectionConversation(
                userAvatarData = UserAvatarData(
                    conversationDetails.otherUser?.previewPicture?.let {
                        ImageAsset.UserAvatarAsset(
                            wireSessionImageLoader,
                            it
                        )
                    },
                    conversationDetails.otherUser?.availabilityStatus ?: UserAvailabilityStatus.NONE
                ),
                conversationInfo = ConversationInfo(
                    name = conversationDetails.otherUser?.name.orEmpty(),
                    membership = userTypeMapper.toMembership(conversationDetails.userType)
                ),
                lastMessageContent = UILastMessageContent.Connection(
                    conversationDetails.connection.status,
                    conversationDetails.connection.qualifiedToId
                ),
                badgeEventType = parseConnectionEventType(conversationDetails.connection.status),
                conversationId = conversationDetails.conversation.id,
                mutedStatus = conversationDetails.conversation.mutedStatus
            )
        }

            is ConversationDetails.Self -> {
            throw IllegalArgumentException("Self conversations should not be visible to the user.")
        }

            else -> {
            throw IllegalArgumentException("$this conversations should not be visible to the user.")
        }
    }

    private fun parseConnectionEventType(connectionState: ConnectionState) =
        if (connectionState == ConnectionState.SENT) BadgeEventType.SentConnectRequest else BadgeEventType.ReceivedConnectionRequest
}
