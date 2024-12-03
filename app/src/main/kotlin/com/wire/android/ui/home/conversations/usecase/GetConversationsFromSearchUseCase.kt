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
import androidx.paging.map
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.mapper.toConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.ConversationQueryConfig
import com.wire.kalium.logic.feature.conversation.GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetConversationsFromSearchUseCase @Inject constructor(
    private val useCase: GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val userTypeMapper: UserTypeMapper,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(
        searchQuery: String = "",
        fromArchive: Boolean = false,
        newActivitiesOnTop: Boolean = false,
        onlyInteractionEnabled: Boolean = false,
    ): Flow<PagingData<ConversationItem>> {
        val pagingConfig = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            initialLoadSize = INITIAL_LOAD_SIZE,
            enablePlaceholders = true,
        )
        return useCase(
            queryConfig = ConversationQueryConfig(
                searchQuery = searchQuery,
                fromArchive = fromArchive,
                newActivitiesOnTop = newActivitiesOnTop,
                onlyInteractionEnabled = onlyInteractionEnabled
            ),
            pagingConfig = pagingConfig,
            startingOffset = 0L,
        ).map { pagingData ->
            pagingData.map {
                it.toConversationItem(wireSessionImageLoader, userTypeMapper, searchQuery)
            }
        }.flowOn(dispatchers.io())
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val INITIAL_LOAD_SIZE = 60
        const val PREFETCH_DISTANCE = 5
    }
}
