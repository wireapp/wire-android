/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
import androidx.paging.flatMap
import com.wire.android.appLogger
import com.wire.android.mapper.MessageMapper
import com.wire.android.ui.common.toGenericAssetGroupedByMonthAndYear
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.feature.asset.GetPaginatedFlowOfAssetMessageByConversationIdUseCase
import com.wire.kalium.logic.feature.conversation.ObserveUserListByIdUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.datetime.TimeZone
import javax.inject.Inject
import kotlin.math.max

class GetAssetMessagesFromConversationUseCase @Inject constructor(
    private val getAssetMessages: GetPaginatedFlowOfAssetMessageByConversationIdUseCase,
    private val observeMemberDetailsByIds: ObserveUserListByIdUseCase,
    private val messageMapper: MessageMapper,
    private val dispatchers: DispatcherProvider
) {

    /**
     * This operation combines asset messages from a conversation and its respective user to UI
     * @param conversationId The conversation ID that it will look for asset messages in.
     *
     * String stands for the Month/Year header, and List<UIMessage> of messages to be displayed.
     * @return A [Map<String, List<UIMessage>>] indicating the success of the operation.
     *
     */
    suspend operator fun invoke(
        conversationId: ConversationId,
        initialOffset: Int
    ): Flow<Map<String, List<UIMessage>>> {
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
            val messagesList = mutableListOf<Message.Standalone>()
            pagingData.flatMap { messageItem ->
                appLogger.d("ASSET_FILES -> msg : ${messageItem.conversationId}")
                messagesList.add(messageItem)
                listOf(messageItem)
            }

            val timeZone = TimeZone.currentSystemDefault()
            appLogger.d("ASSET_FILES -> SIZE : ${messagesList.size}")
            messagesList.toGenericAssetGroupedByMonthAndYear(timeZone = timeZone)
        }.map { groupedAssetMessages ->
            mutableMapOf<String, List<UIMessage>>().apply {
                groupedAssetMessages.entries.forEach { entry ->
                    val values: List<UIMessage> = entry.value.map { message ->
                        observeMemberDetailsByIds(messageMapper.memberIdList(listOf(message)))
                            .mapLatest { usersList ->
                                messageMapper.toUIMessage(usersList, message)?.let { listOf(it) }
                                    ?: emptyList()
                            }.first().first()
                    }
                    this@apply.put(entry.key, values)
                }
            }

//            val x: String = groupedAssetMessages.entries.map {
//                val values: List<UIMessage> = it.value.map { message ->
//                    observeMemberDetailsByIds(messageMapper.memberIdList(listOf(message)))
//                        .mapLatest { usersList ->
//                            messageMapper.toUIMessage(usersList, message)?.let { listOf(it) }
//                                ?: emptyList()
//                        }
//                        .first()
//                }
//                mapOf(it.key to values)
            // }
            // mapOf<String, List<UIMessage>>()
        }.flowOn(dispatchers.io())

//            .map {
////            pagingData.flatMap { messageItem ->
////                observeMemberDetailsByIds(messageMapper.memberIdList(listOf(messageItem)))
////                    .mapLatest { usersList ->
////                        messageMapper.toUIMessage(usersList, messageItem)?.let { listOf(it) }
////                            ?: emptyList()
////                    }.first()
////            }
//
//        }.asFlow().flowOn(dispatchers.io())

//        return getAssetMessages(
//            conversationId = conversationId,
//            limit = 35,
//            offset = 0
//        ).let { assetMessageList ->
//            val timeZone = TimeZone.currentSystemDefault()
//            assetMessageList.toGenericAssetGroupedByMonthAndYear(timeZone = timeZone)
//        }.map {
//
//            val values: List<UIMessage> = it.value.map { message ->
//                observeMemberDetailsByIds(messageMapper.memberIdList(listOf(message)))
//                    .mapLatest { usersList ->
//                        messageMapper.toUIMessage(usersList, message)?.let { listOf(it) }
//                            ?: emptyList()
//                    }
//                    .first()
//                    .first()
//            }
//            mapOf(it.key to values)
//        }.first()
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val INITIAL_LOAD_SIZE = 20
        const val PREFETCH_DISTANCE = 30
    }
}
