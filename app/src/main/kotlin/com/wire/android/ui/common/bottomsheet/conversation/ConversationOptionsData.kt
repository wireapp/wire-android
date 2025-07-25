/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.common.bottomsheet.conversation

import com.wire.android.R
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.home.conversationslist.model.BlockState
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.showLegalHoldIndicator
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationFolder
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.type.UserType

data class ConversationOptionsData(
    val title: UIText,
    val conversationId: ConversationId,
    val mutingConversationState: MutedConversationStatus,
    val conversationTypeDetail: ConversationTypeDetail,
    val selfRole: Conversation.Member.Role?,
    val isTeamConversation: Boolean,
    val isArchived: Boolean,
    val protocol: Conversation.ProtocolInfo,
    val mlsVerificationStatus: Conversation.VerificationStatus,
    val proteusVerificationStatus: Conversation.VerificationStatus,
    val isUnderLegalHold: Boolean,
    val isFavorite: Boolean?,
    val folder: ConversationFolder?,
) {

    private val isSelfUserMember: Boolean get() = selfRole != null

    private val isPrivateOtherThanBlocked: Boolean
        get() = conversationTypeDetail is ConversationTypeDetail.Private && conversationTypeDetail.blockingState != BlockingState.BLOCKED

    private val isPrivateWithNonDeletedUser: Boolean
        get() = conversationTypeDetail is ConversationTypeDetail.Private && !conversationTypeDetail.isUserDeleted

    private val isGroup: Boolean get() = conversationTypeDetail is ConversationTypeDetail.Group

    fun canEditNotifications(): Boolean = isSelfUserMember && ((isPrivateOtherThanBlocked && isPrivateWithNonDeletedUser) || isGroup)

    /**
     * TODO(refactor): All of this logic to figure out permissions should live in Kalium/Logic module, instead of in the presentation layer
     */
    fun canDeleteGroup(): Boolean = canDeleteChannel || canDeleteRegularGroup

    private val canDeleteRegularGroup: Boolean
        get() = conversationTypeDetail is ConversationTypeDetail.Group.Regular &&
                selfRole == Conversation.Member.Role.Admin &&
                conversationTypeDetail.isFromTheSameTeam && isTeamConversation

    private val canDeleteChannel: Boolean
        get() = conversationTypeDetail is ConversationTypeDetail.Group.Channel &&
                conversationTypeDetail.isFromTheSameTeam && isTeamConversation &&
                (selfRole == Conversation.Member.Role.Admin || conversationTypeDetail.isSelfUserTeamAdmin)

    fun canLeaveTheGroup(): Boolean = conversationTypeDetail is ConversationTypeDetail.Group && isSelfUserMember

    fun canDeleteGroupLocally(): Boolean = !isSelfUserMember

    fun canBlockUser(): Boolean {
        return conversationTypeDetail is ConversationTypeDetail.Private
                && conversationTypeDetail.blockingState == BlockingState.NOT_BLOCKED
                && !conversationTypeDetail.isUserDeleted
    }

    fun canUnblockUser(): Boolean =
        conversationTypeDetail is ConversationTypeDetail.Private && conversationTypeDetail.blockingState == BlockingState.BLOCKED

    fun canAddToFavourite(): Boolean = isFavorite != null && (isPrivateOtherThanBlocked || isGroup)
}

@Suppress("LongMethod")
fun ConversationDetails.toConversationOptionsData(selfUser: SelfUser): ConversationOptionsData? =
    when (this) {
        is ConversationDetails.Group -> ConversationOptionsData(
            conversationId = conversation.id,
            title = if (conversation.name.isNullOrEmpty()) {
                UIText.StringResource(R.string.member_name_deleted_label)
            } else {
                UIText.DynamicString(conversation.name.orEmpty())
            },
            mutingConversationState = conversation.mutedStatus,
            conversationTypeDetail = if (this is ConversationDetails.Group.Channel) {
                ConversationTypeDetail.Group.Channel(
                    conversationId = conversation.id,
                    isFromTheSameTeam = conversation.teamId == selfUser.teamId,
                    isPrivate = access == ConversationDetails.Group.Channel.ChannelAccess.PRIVATE,
                    isSelfUserTeamAdmin = selfUser.userType in arrayOf(UserType.ADMIN, UserType.OWNER),
                )
            } else {
                ConversationTypeDetail.Group.Regular(
                    conversationId = conversation.id,
                    isFromTheSameTeam = conversation.teamId == selfUser.teamId,
                )
            },
            isTeamConversation = conversation.teamId?.value != null,
            selfRole = selfRole,
            isArchived = conversation.archived,
            protocol = conversation.protocol,
            mlsVerificationStatus = conversation.mlsVerificationStatus,
            proteusVerificationStatus = conversation.proteusVerificationStatus,
            isUnderLegalHold = conversation.legalHoldStatus.showLegalHoldIndicator(),
            isFavorite = isFavorite,
            folder = folder,
        )

        is ConversationDetails.OneOne -> ConversationOptionsData(
            conversationId = conversation.id,
            title = if (otherUser.isUnavailableUser) {
                UIText.StringResource(R.string.username_unavailable_label)
            } else {
                UIText.DynamicString(otherUser.name.orEmpty())
            },
            mutingConversationState = conversation.mutedStatus,
            conversationTypeDetail = ConversationTypeDetail.Private(
                avatarAsset = otherUser.previewPicture?.let { UserAvatarAsset(it) },
                userId = otherUser.id,
                blockingState = otherUser.BlockState,
                isUserDeleted = otherUser.deleted,
            ),
            isTeamConversation = conversation.teamId?.value != null,
            selfRole = Conversation.Member.Role.Member,
            isArchived = conversation.archived,
            protocol = conversation.protocol,
            mlsVerificationStatus = conversation.mlsVerificationStatus,
            proteusVerificationStatus = conversation.proteusVerificationStatus,
            isUnderLegalHold = conversation.legalHoldStatus.showLegalHoldIndicator(),
            isFavorite = isFavorite,
            folder = folder,
        )

        is ConversationDetails.Connection -> ConversationOptionsData(
            conversationId = conversationId,
            title = UIText.DynamicString(otherUser?.name.orEmpty()),
            mutingConversationState = conversation.mutedStatus,
            conversationTypeDetail = ConversationTypeDetail.Connection(
                avatarAsset = otherUser?.previewPicture?.let { UserAvatarAsset(it) },
            ),
            isTeamConversation = conversation.teamId?.value != null,
            selfRole = Conversation.Member.Role.Member,
            isArchived = conversation.archived,
            protocol = conversation.protocol,
            mlsVerificationStatus = conversation.mlsVerificationStatus,
            proteusVerificationStatus = conversation.proteusVerificationStatus,
            isUnderLegalHold = conversation.legalHoldStatus.showLegalHoldIndicator(),
            isFavorite = null,
            folder = null,
        )

        is ConversationDetails.Self -> null
        is ConversationDetails.Team -> null
    }
