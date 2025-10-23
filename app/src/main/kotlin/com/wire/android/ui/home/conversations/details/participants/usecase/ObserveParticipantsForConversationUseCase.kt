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
import com.wire.android.ui.home.conversations.userId
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.isAppOrBot
import com.wire.kalium.logic.data.user.type.isExternal
import com.wire.kalium.logic.data.user.type.isGuest
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetMembersE2EICertificateStatusesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import javax.inject.Inject

class ObserveParticipantsForConversationUseCase @Inject constructor(
    private val observeConversationMembers: ObserveConversationMembersUseCase,
    private val getMembersE2EICertificateStatuses: GetMembersE2EICertificateStatusesUseCase,
    private val uiParticipantMapper: UIParticipantMapper,
    private val dispatchers: DispatcherProvider
) {
    suspend operator fun invoke(conversationId: ConversationId, limit: Int = -1): Flow<ConversationParticipantsData> =
        observeConversationMembers(conversationId)
            .map { memberDetailList ->
                memberDetailList.sortedBy { it.name }.groupBy {
                    val isAdmin = it.role == Member.Role.Admin
                    val isService = (it.user as? OtherUser)?.userType?.isAppOrBot() == true
                    isAdmin && !isService
                }
            }
            .scan(
                ConversationParticipantsData() to emptyMap<UserId, Boolean>()
            ) { (_, previousMlsVerificationMap), sortedMemberList ->
                val allAdminsWithoutServices = sortedMemberList.getOrDefault(true, listOf())
                val visibleAdminsWithoutServices = allAdminsWithoutServices.limit(limit)
                val allParticipants = sortedMemberList.getOrDefault(false, listOf())
                val visibleParticipants = allParticipants.limit(limit)

                val visibleUserIds = visibleParticipants.map { it.userId }
                    .plus(visibleAdminsWithoutServices.map { it.userId })

                // only fetch certificate statuses for newly emitted users and get the rest from previous iterations
                val newlyEmittedVisibleUserIds = visibleUserIds - previousMlsVerificationMap.keys
                val mlsVerificationMap = previousMlsVerificationMap.plus(
                    if (newlyEmittedVisibleUserIds.isEmpty()) {
                        emptyMap()
                    } else {
                        getMembersE2EICertificateStatuses(conversationId, newlyEmittedVisibleUserIds)
                    }
                )

                val selfUser = (allParticipants + allAdminsWithoutServices).firstOrNull { it.user is SelfUser }

                ConversationParticipantsData(
                    admins = visibleAdminsWithoutServices
                        .map { uiParticipantMapper.toUIParticipant(it.user, mlsVerificationMap[it.user.id] ?: false) },
                    participants = visibleParticipants
                        .map { uiParticipantMapper.toUIParticipant(it.user, mlsVerificationMap[it.user.id] ?: false) },
                    allAdminsCount = allAdminsWithoutServices.size,
                    allParticipantsCount = allParticipants.size,
                    isSelfAnAdmin = allAdminsWithoutServices.any { it.user is SelfUser },
                    isSelfExternalMember = selfUser?.user?.userType?.isExternal() == true,
                    isSelfGuest = selfUser?.user?.userType?.isGuest() == true,
                ) to mlsVerificationMap
            }
            .drop(1) // ignore the initial value from scan
            .map { (data, _) -> data }
            .flowOn(dispatchers.io())

    private fun <T> List<T>.limit(limit: Int = -1) = when {
        limit > 0 -> this.take(limit)
        limit == 0 -> listOf()
        else -> this
    }
}
