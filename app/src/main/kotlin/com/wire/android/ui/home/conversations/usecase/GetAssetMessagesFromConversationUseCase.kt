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
import androidx.paging.insertSeparators
import com.wire.android.mapper.MessageMapper
import com.wire.android.ui.common.monthYearHeader
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetPaginatedFlowOfAssetMessageByConversationIdUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.math.max

class GetAssetMessagesFromConversationUseCase @Inject constructor(
    private val getAssetMessages: GetPaginatedFlowOfAssetMessageByConversationIdUseCase,
    private val getUsersForMessage: GetUsersForMessageUseCase,
    private val messageMapper: MessageMapper,
    private val dispatchers: DispatcherProvider
) {

    /**
     * This operation combines asset messages from a conversation and its respective user to UI
     * @param conversationId The conversation ID that it will look for asset messages in.
     *
     * @return A [PagingData<UIMessage>>] indicating the success of the operation.
     */
    suspend operator fun invoke(
        conversationId: ConversationId,
        initialOffset: Int
    ): Flow<PagingData<UIPagingItem>> {
        val pagingConfig = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            initialLoadSize = INITIAL_LOAD_SIZE
        )

        return getAssetMessages(
            conversationId = conversationId,
            startingOffset = max(0, initialOffset - PREFETCH_DISTANCE).toLong(),
            pagingConfig = pagingConfig
        ).map { pagingData ->
            val currentTime = TimeZone.currentSystemDefault()
            val uiMessagePagingData: PagingData<UIPagingItem> = pagingData.flatMap { messageItem ->
                val usersForMessage = getUsersForMessage(messageItem)
                messageMapper.toUIMessage(usersForMessage, messageItem)
                    ?.let { listOf(UIPagingItem.Message(it, messageItem.date)) }
                    ?: emptyList()
            }.insertSeparators { before: UIPagingItem.Message?, after: UIPagingItem.Message? ->
                if (before == null && after != null) {
                    val localDateTime = after.date.toLocalDateTime(currentTime)
                    UIPagingItem.Label(monthYearHeader(year = localDateTime.year, month = localDateTime.monthNumber))
                } else if (before != null && after != null) {
                    val beforeDateTime = before.date.toLocalDateTime(currentTime)
                    val afterDateTime = after.date.toLocalDateTime(currentTime)

                    if (beforeDateTime.year != afterDateTime.year
                        || beforeDateTime.month != afterDateTime.month
                    ) {
                        UIPagingItem.Label(monthYearHeader(year = afterDateTime.year, month = afterDateTime.monthNumber))
                    } else {
                        null
                    }
                } else {
                    // no separator - either end of list, or first
                    // letters of items are the same
                    null
                }
            }
            uiMessagePagingData
        }.flowOn(dispatchers.io())
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val INITIAL_LOAD_SIZE = 20
        const val PREFETCH_DISTANCE = 30
    }
}

sealed class UIPagingItem {

    data class Message(val uiMessage: UIMessage, val date: Instant) : UIPagingItem()

    data class Label(val date: String) : UIPagingItem()
}
