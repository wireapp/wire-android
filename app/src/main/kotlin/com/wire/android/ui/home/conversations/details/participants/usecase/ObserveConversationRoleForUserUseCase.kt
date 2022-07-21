package com.wire.android.ui.home.conversations.details.participants.usecase

import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveConversationRoleForUserUseCase  @Inject constructor(
    private val observeConversationMembers: ObserveConversationMembersUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val getSelfUser: GetSelfUserUseCase
) {
    suspend operator fun invoke(conversationId: ConversationId, userId: UserId): Flow<ConversationRoleData> =
        combine(
            getSelfUser(),
            observeConversationDetails(conversationId),
            observeConversationMembers(conversationId)
        ) { selfUser: SelfUser, conversationDetails: ConversationDetails, memberDetailsList: List<MemberDetails> ->
            ConversationRoleData(
                conversationDetails.conversation.name.orEmpty(),
                memberDetailsList.firstOrNull { it.user.id == userId }?.role,
                memberDetailsList.firstOrNull { it.user.id == selfUser.id }?.role
            )
        }
}

data class ConversationRoleData(
    val conversationName: String,
    val userRole: Member.Role?,
    val selfRole: Member.Role?
    )
