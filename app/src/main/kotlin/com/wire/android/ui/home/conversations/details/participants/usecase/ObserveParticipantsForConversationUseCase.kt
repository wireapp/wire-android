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

import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.name
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveParticipantsForConversationUseCase @Inject constructor(
    private val observeConversationMembers: ObserveConversationMembersUseCase,
    private val uiParticipantMapper: UIParticipantMapper,
    private val dispatchers: DispatcherProvider
) {
    suspend operator fun invoke(conversationId: ConversationId, limit: Int = -1): Flow<ConversationParticipantsData> =
        observeConversationMembers(conversationId)
            .map { memberDetailList ->
                memberDetailList.sortedBy { it.name }.groupBy {
                    val isAdmin = it.role == Member.Role.Admin
                    val isService = (it.user as? OtherUser)?.userType == UserType.SERVICE
                    isAdmin && !isService
                }
            }

            .map { sortedMemberList ->
                val allAdminsWithoutServices = sortedMemberList.getOrDefault(true, listOf())
                val allParticipants = sortedMemberList.getOrDefault(false, listOf())
                ConversationParticipantsData(
                    admins = allAdminsWithoutServices.limit(limit).map { uiParticipantMapper.toUIParticipant(it.user) },
                    participants = allParticipants.limit(limit).map { uiParticipantMapper.toUIParticipant(it.user) },
                    allAdminsCount = allAdminsWithoutServices.size,
                    allParticipantsCount = allParticipants.size,
                    isSelfAnAdmin = allAdminsWithoutServices.any { it.user is SelfUser }
                )
            }
            .flowOn(dispatchers.io())

    private fun <T> List<T>.limit(limit: Int = -1) = when {
        limit > 0 -> this.take(limit)
        limit == 0 -> listOf()
        else -> this
    }
}
