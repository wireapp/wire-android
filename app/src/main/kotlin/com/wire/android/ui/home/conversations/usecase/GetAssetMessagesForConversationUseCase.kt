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
 *
 *
 */

package com.wire.android.ui.home.conversations.usecase

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.flatMap
import com.wire.android.appLogger
import com.wire.android.mapper.AssetMapper
import com.wire.android.mapper.MessageMapper
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.messagetypes.asset.UIAsset
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.ObserveUserListByIdUseCase
import com.wire.kalium.logic.feature.message.GetPaginatedFlowOfAssetMessagesByConversationUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import java.lang.Integer.max
import javax.inject.Inject

class GetAssetMessagesForConversationUseCase @Inject constructor(
    private val getMessages: GetPaginatedFlowOfAssetMessagesByConversationUseCase,
    private val observeMemberDetailsByIds: ObserveUserListByIdUseCase,
    private val messageMapper: MessageMapper,
    private val assetMapper: AssetMapper,
    private val dispatchers: DispatcherProvider,
) {

    suspend operator fun invoke(conversationId: ConversationId, lastReadIndex: Int): Flow<PagingData<UIAsset>> {
        val pagingConfig = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            initialLoadSize = max(INITIAL_LOAD_SIZE, lastReadIndex + PREFETCH_DISTANCE)
        )
        return getMessages(
            conversationId,
            pagingConfig = pagingConfig,
            startingOffset = 0
        ).map { pagingData ->
            pagingData.flatMap { messageItem ->
                observeMemberDetailsByIds(messageMapper.memberIdList(listOf(messageItem)))
                    .mapLatest { usersList ->
                        val uiMessage = assetMapper.toUIAsset(usersList, messageItem)
                        if(uiMessage != null) {
                            listOf(uiMessage)
                        } else {
                            listOf()
                        }
                    }
                    .first()
            }
        }.flowOn(dispatchers.io())
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val INITIAL_LOAD_SIZE = 50
        const val PREFETCH_DISTANCE = 30
    }
}
