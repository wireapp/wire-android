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

package com.wire.android.ui.home.conversations.details.participants.usecase

import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.isTeamAdmin
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * TODO: Move to Kalium's logic or similar
 */
class ObserveConversationRoleForUserUseCase @Inject constructor(
    private val observeConversationMembers: ObserveConversationMembersUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeSelfUser: ObserveSelfUserUseCase
) {
    suspend operator fun invoke(conversationId: ConversationId, userId: UserId): Flow<ConversationRoleData> =
        combine(
            observeSelfUser(),
            observeConversationDetails(conversationId)
                .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>() // TODO handle StorageFailure
                .map { it.conversationDetails },
            observeConversationMembers(conversationId)
        ) { selfUser: SelfUser, conversationDetails: ConversationDetails, memberDetailsList: List<MemberDetails> ->
            val regularSelfRole = memberDetailsList.firstOrNull { it.user.id == selfUser.id }?.role
            val isSelfTeamAdmin = selfUser.userType.isTeamAdmin()
            val isConversationInSameTeamAsSelfUser = selfUser.teamId == conversationDetails.conversation.teamId
            val isChannel = conversationDetails is ConversationDetails.Group.Channel
            val isSelfChannelAdmin = isChannel && isSelfTeamAdmin && isConversationInSameTeamAsSelfUser
            val selfRole = if (isSelfChannelAdmin) {
                Conversation.Member.Role.Admin
            } else {
                regularSelfRole
            }
            ConversationRoleData(
                conversationName = conversationDetails.conversation.name.orEmpty(),
                userRole = memberDetailsList.firstOrNull { it.user.id == userId }?.role,
                selfRole = selfRole,
                conversationId = conversationId,
            )
        }
}

data class ConversationRoleData(
    val conversationName: String,
    val userRole: Conversation.Member.Role?,
    val selfRole: Conversation.Member.Role?,
    val conversationId: ConversationId
)
