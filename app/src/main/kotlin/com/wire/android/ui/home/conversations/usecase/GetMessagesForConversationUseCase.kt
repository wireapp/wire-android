package com.wire.android.ui.home.conversations.usecase

import com.wire.android.mapper.MessageMapper
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.message.GetRecentMessagesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMessagesForConversationUseCase
@Inject constructor(
    private val getMessages: GetRecentMessagesUseCase,
    private val observeMemberDetails: ObserveConversationMembersUseCase,
    private val messageMapper: MessageMapper,
    private val dispatchers: DispatcherProvider,
) {

    suspend operator fun invoke(conversationId: ConversationId): Flow<List<UIMessage>> =
        getMessages(conversationId)
            .combine(observeMemberDetails(conversationId), ::Pair)
            .map { (messages, members) ->
                messageMapper.toUIMessages(members, messages)
            }.flowOn(dispatchers.io())

}
