package com.wire.android.ui.home.conversations.banner.usecase

import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveConversationMembersByTypesUseCase @Inject constructor(
    private val observeConversationMembers: ObserveConversationMembersUseCase,
    private val dispatchers: DispatcherProvider
) {
    suspend operator fun invoke(conversationId: ConversationId): Flow<Set<UserType>> =
        observeConversationMembers(conversationId)
            .map { memberDetails ->
                memberDetails.map {it.user}
                    .filter { (it is OtherUser) }
                    .map { (it as OtherUser).userType }
                    .toSet()
            }
            .flowOn(dispatchers.io())

}
