package com.wire.android.ui.home.conversations.messagedetails.usecase

import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReactionsData
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.message.ObserveMessageReactionsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveReactionsForMessageUseCase @Inject constructor(
    private val observeMessageReactions: ObserveMessageReactionsUseCase,
    private val uiParticipantMapper: UIParticipantMapper,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(conversationId: ConversationId, messageId: String): Flow<MessageDetailsReactionsData> =
        observeMessageReactions(
            conversationId = conversationId,
            messageId = messageId
        ).map { messageReactionsList ->
            messageReactionsList
                .groupBy { it.emoji }
                .toList()
                .sortedBy { it.second.size }
                .reversed()
                .toMap()
        }.map { mapResult ->
            val result = mutableMapOf<String, List<UIParticipant>>()

            mapResult.forEach {
                result[it.key] = it.value.map(uiParticipantMapper::toUIParticipant)
            }

            MessageDetailsReactionsData(
                reactions = result
            )
        }.flowOn(dispatchers.io())
}
