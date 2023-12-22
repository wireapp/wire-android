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
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import com.wire.android.mapper.UIAssetMapper
import com.wire.android.ui.common.monthYearHeader
import com.wire.android.ui.home.conversations.model.messagetypes.asset.UIAssetMessage
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetPaginatedFlowOfAssetImageMessageByConversationIdUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.math.max

class GetImageAssetMessagesFromConversationUseCase @Inject constructor(
    private val getAssetMessages: GetPaginatedFlowOfAssetImageMessageByConversationIdUseCase,
    private val assetMapper: UIAssetMapper,
    private val dispatchers: DispatcherProvider
) {

    /**
     * This operation observers image asset messages from a conversation
     * @param conversationId The conversation ID that it will look for image asset messages in.
     *
     * @return A [PagingData<UIImageAssetPagingItem>>] containing [UIImageAssetPagingItem.Asset] and time [UIImageAssetPagingItem.Label].
     */
    suspend operator fun invoke(
        conversationId: ConversationId,
        initialOffset: Int
    ): Flow<PagingData<UIImageAssetPagingItem>> {
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
            val uiMessagePagingData: PagingData<UIImageAssetPagingItem> = pagingData.map { assetMessage ->
                UIImageAssetPagingItem.Asset(assetMapper.toUIAsset(assetMessage))
            }.insertSeparators { before: UIImageAssetPagingItem.Asset?, after: UIImageAssetPagingItem.Asset? ->
                if (before == null && after != null) {
                    val localDateTime = after.uiAssetMessage.time.toLocalDateTime(currentTime)
                    UIImageAssetPagingItem.Label(monthYearHeader(year = localDateTime.year, month = localDateTime.monthNumber))
                } else if (before != null && after != null) {
                    val beforeDateTime = before.uiAssetMessage.time.toLocalDateTime(currentTime)
                    val afterDateTime = after.uiAssetMessage.time.toLocalDateTime(currentTime)

                    if (beforeDateTime.year != afterDateTime.year
                        || beforeDateTime.month != afterDateTime.month
                    ) {
                        UIImageAssetPagingItem.Label(monthYearHeader(year = afterDateTime.year, month = afterDateTime.monthNumber))
                    } else {
                        null
                    }
                } else {
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

sealed class UIImageAssetPagingItem {

    data class Asset(val uiAssetMessage: UIAssetMessage) : UIImageAssetPagingItem()

    data class Label(val date: String) : UIImageAssetPagingItem()
}
