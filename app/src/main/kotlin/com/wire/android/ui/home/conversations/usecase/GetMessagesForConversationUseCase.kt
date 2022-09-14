package com.wire.android.ui.home.conversations.usecase

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.flatMap
import com.wire.android.mapper.MessageMapper
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.ObserveUserListByIdUseCase
import com.wire.kalium.logic.feature.message.GetPaginatedFlowOfMessagesByConversationUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

class GetMessagesForConversationUseCase @Inject constructor(
    private val getMessages: GetPaginatedFlowOfMessagesByConversationUseCase,
    private val observeMemberDetailsByIds: ObserveUserListByIdUseCase,
    private val messageMapper: MessageMapper,
    private val dispatchers: DispatcherProvider,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend operator fun invoke(conversationId: ConversationId): Flow<PagingData<UIMessage>> {
        val pagingConfig = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            initialLoadSize = INITIAL_LOAD_SIZE
        )
        return getMessages(
            conversationId,
            pagingConfig = pagingConfig
        ).map { pagingData ->
            pagingData.flatMap { messageItem ->
                observeMemberDetailsByIds(messageMapper.memberIdList(listOf(messageItem)))
                    .mapLatest {
                        messageMapper.toUIMessages(it, listOf(messageItem))
                    }.first()
            }
        }.flowOn(dispatchers.io())
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val INITIAL_LOAD_SIZE = 50
        const val PREFETCH_DISTANCE = 30
    }
}
