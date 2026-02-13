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

package com.wire.android.ui.home.conversations.usecase

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.flatMap
import com.wire.android.mapper.MessageMapper
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.message.GetPaginatedFlowOfMessagesByConversationUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.lang.Integer.max
import javax.inject.Inject

class GetMessagesForConversationUseCase @Inject constructor(
    private val getMessages: GetPaginatedFlowOfMessagesByConversationUseCase,
    private val getUsersForMessage: GetUsersForMessageUseCase,
    private val messageMapper: MessageMapper,
    private val dispatchers: DispatcherProvider,
) {

    suspend operator fun invoke(
        conversationId: ConversationId,
        lastReadIndex: Int,
        threadId: String? = null,
    ): Flow<PagingData<UIMessage>> {
        val pagingConfig = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            initialLoadSize = INITIAL_LOAD_SIZE,
            enablePlaceholders = true,
        )
        return getMessages(
            conversationId,
            threadId = threadId,
            pagingConfig = pagingConfig,
            startingOffset = max(0, lastReadIndex - PREFETCH_DISTANCE).toLong()
        ).map { pagingData ->
            pagingData.flatMap { messageItem ->
                val usersForMessage = getUsersForMessage(messageItem)
                messageMapper.toUIMessage(usersForMessage, messageItem)?.let { listOf(it) } ?: emptyList()
            }
        }.flowOn(dispatchers.io())
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val INITIAL_LOAD_SIZE = 60
        const val PREFETCH_DISTANCE = 30
    }
}
