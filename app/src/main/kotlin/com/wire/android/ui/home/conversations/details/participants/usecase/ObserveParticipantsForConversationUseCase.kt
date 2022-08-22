package com.wire.android.ui.home.conversations.details.participants.usecase

import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.name
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.SelfUser
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
            .map { memberDetailList -> memberDetailList.sortedBy { it.name } }
            .map { sortedMemberList ->
                val allAdmins =
                    sortedMemberList.filter { it.role == Member.Role.Admin } as ArrayList<MemberDetails>
                val allParticipants = sortedMemberList.filter { it.role != Member.Role.Admin } as ArrayList<MemberDetails>
                val adminsWithoutService = arrayListOf<MemberDetails>()

                allAdmins.map {
                    if (uiParticipantMapper.toUIParticipant(it.user).membership == Membership.Service) {
                        allParticipants.add(it)
                    } else {
                        adminsWithoutService.add(it)
                    }
                }
                ConversationParticipantsData(
                    admins = adminsWithoutService.limit(limit).map { uiParticipantMapper.toUIParticipant(it.user) },
                    participants = allParticipants.limit(limit).map { uiParticipantMapper.toUIParticipant(it.user) },
                    allAdminsCount = adminsWithoutService.size,
                    allParticipantsCount = allParticipants.size,
                    isSelfAnAdmin = adminsWithoutService.any { it.user is SelfUser }
                )
            }
            .flowOn(dispatchers.io())

    private fun <T> List<T>.limit(limit: Int = -1) = when {
        limit > 0 -> this.take(limit)
        limit == 0 -> listOf()
        else -> this
    }
}
