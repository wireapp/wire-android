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

package com.wire.android.ui.userprofile.other

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.BotService
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId

data class OtherUserProfileState(
    val userId: UserId,
    val conversationId: ConversationId? = null,
    val userAvatarAsset: UserAvatarAsset? = null,
    val isDataLoading: Boolean = false,
    val isAvatarLoading: Boolean = false,
    val fullName: String = "",
    val userName: String = "",
    val teamName: String = "",
    val email: String = "",
    val phone: String = "",
    val connectionState: ConnectionState = ConnectionState.NOT_CONNECTED,
    val membership: Membership = Membership.None,
    val groupState: OtherUserProfileGroupState? = null,
    val botService: BotService? = null,
    val conversationSheetContent: ConversationSheetContent? = null,
    val otherUserDevices: List<Device> = listOf(),
    val blockingState: BlockingState = BlockingState.CAN_NOT_BE_BLOCKED,
    val isProteusVerified: Boolean = false,
    val isMLSVerified: Boolean = false
) {
    fun updateMuteStatus(status: MutedConversationStatus): OtherUserProfileState {
        return conversationSheetContent?.let {
            val newConversationSheetContent = conversationSheetContent.copy(mutingConversationState = status)
            copy(conversationSheetContent = newConversationSheetContent)
        } ?: this
    }

    companion object {
        val PREVIEW = OtherUserProfileState(
            userId = UserId("some_user", "domain.com"),
            fullName = "name",
            userName = "username",
            teamName = "team",
            email = "email",
            groupState = OtherUserProfileGroupState(
                "group name", Member.Role.Member, true, ConversationId("some_user", "domain.com")
            )
        )
    }

    fun isMetadataEmpty(): Boolean {
        return fullName.isEmpty() || userName.isEmpty()
    }

    fun shouldShowSearchButton(): Boolean = (groupState == null
            && connectionState in listOf(
        ConnectionState.ACCEPTED,
        ConnectionState.BLOCKED,
        ConnectionState.MISSING_LEGALHOLD_CONSENT
    ))
}

data class OtherUserProfileGroupState(
    val groupName: String,
    val role: Member.Role,
    val isSelfAdmin: Boolean,
    val conversationId: ConversationId
)
