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

package com.wire.android.ui.home.conversations.details.options

import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAddPermissionType
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer

/**
 * State for the group conversation options screen.
 *
 * Fields related to updating should be set according to this table:
 * | for given option to be allowed...          | ...user needs to be                                |
 * |--------------------------------------------|----------------------------------------------------|
 * | add participants to group allowed          | group admin & not external team member             |
 * | group name change allowed                  | group admin & not external team member             |
 * | group guests option change allowed         | group admin & team member of the group owner team  |
 * | group services option change allowed       | group admin                                        |
 * | self deleting option change allowed        | group admin                                        |
 * | group read receipts option change allowed  | group admin & group created by a team member       |
 */
data class GroupConversationOptionsState(
    val conversationId: ConversationId,
    val groupName: String = "",
    val protocolInfo: Conversation.ProtocolInfo = Conversation.ProtocolInfo.Proteus,
    val mlsVerificationStatus: Conversation.VerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
    val proteusVerificationStatus: Conversation.VerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
    val legalHoldStatus: Conversation.LegalHoldStatus = Conversation.LegalHoldStatus.DISABLED,
    val areAccessOptionsAvailable: Boolean = false,
    val isGuestAllowed: Boolean = false,
    val isServicesAllowed: Boolean = false,
    val isReadReceiptAllowed: Boolean = false,
    val isUpdatingNameAllowed: Boolean = false,
    val isUpdatingGuestAllowed: Boolean = false,
    val isUpdatingServicesAllowed: Boolean = false,
    val isUpdatingSelfDeletingAllowed: Boolean = false,
    val isUpdatingReadReceiptAllowed: Boolean = false,
    val isUpdatingChannelAccessAllowed: Boolean = false,
    val shouldShowAddParticipantsButtonForChannel: Boolean = false,
    val changeGuestOptionConfirmationRequired: Boolean = false,
    val changeServiceOptionConfirmationRequired: Boolean = false,
    val loadingServicesOption: Boolean = false,
    val loadingReadReceiptOption: Boolean = false,
    val isChannel: Boolean = false,
    val isSelfTeamAdmin: Boolean = false,
    val channelAccessType: ChannelAccessType? = ChannelAccessType.PRIVATE,
    val channelAddPermissionType: ChannelAddPermissionType? = ChannelAddPermissionType.ADMINS,
    val error: Error = Error.None,
    val mlsEnabled: Boolean = false,
    val selfDeletionTimer: SelfDeletionTimer = SelfDeletionTimer.Disabled,
    val loadingWireCellState: Boolean = false,
    val isWireCellFeatureEnabled: Boolean = false,
    val isWireCellEnabled: Boolean = false,
) {

    sealed interface Error {
        data object None : Error
        class UpdateServicesError(val cause: CoreFailure) : Error
        class UpdateReadReceiptError(val cause: CoreFailure) : Error
    }

    fun isAbandonedOneOnOneConversation(participantsCount: Int): Boolean = groupName.isEmpty() && participantsCount == 1
}
