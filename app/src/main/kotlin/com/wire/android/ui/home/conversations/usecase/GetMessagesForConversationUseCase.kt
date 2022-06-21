package com.wire.android.ui.home.conversations.usecase

import com.wire.android.mapper.MessageMapper
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.ObserveMemberDetailsByIdsUseCase
import com.wire.kalium.logic.feature.message.GetRecentMessagesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMessagesForConversationUseCase
@Inject constructor(
    private val getMessages: GetRecentMessagesUseCase,
    private val observeMemberDetailsByIds: ObserveMemberDetailsByIdsUseCase,
    private val messageMapper: MessageMapper,
    private val dispatchers: DispatcherProvider,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend operator fun invoke(conversationId: ConversationId): Flow<List<UIMessage>> =
        getMessages(conversationId)
            .flatMapLatest { messages ->
                observeMemberDetailsByIds(messageMapper.memberIdList(messages))
                    .map { members -> messageMapper.toUIMessages(members, messages) }
            }.flowOn(dispatchers.io())

}
