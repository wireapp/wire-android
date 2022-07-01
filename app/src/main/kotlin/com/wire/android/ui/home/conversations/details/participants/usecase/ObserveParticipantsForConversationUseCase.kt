package com.wire.android.ui.home.conversations.details.participants.usecase

import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.name
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
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
            .map { it.sortedBy { it.name } }
            .map {
                val allAdmins = it.filter { false } // TODO count { it is Admin }
                val allParticipants = it.filter { true } // TODO count { it !is Admin }

                ConversationParticipantsData(
                    admins = allAdmins.limit(limit).map { uiParticipantMapper.toUIParticipant(it) },
                    participants = allParticipants.limit(limit).map { uiParticipantMapper.toUIParticipant(it) },
                    allAdminsCount = allAdmins.size,
                    allParticipantsCount = allParticipants.size
                )
            }
            .flowOn(dispatchers.io())

    private fun <T> List<T>.limit(limit: Int = -1) = when {
            limit > 0 -> this.take(limit)
            limit == 0 -> listOf()
            else -> this
        }
}
