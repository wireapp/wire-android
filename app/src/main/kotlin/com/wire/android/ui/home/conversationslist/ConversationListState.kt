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

package com.wire.android.ui.home.conversationslist

import androidx.compose.runtime.Stable
import androidx.paging.PagingData
import com.wire.android.ui.home.conversationslist.model.ConversationSection
import com.wire.android.ui.home.conversationslist.model.ConversationItemType
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow

@Stable
sealed interface ConversationListState {
    data class Paginated(
        val conversations: Flow<PagingData<ConversationItemType>>,
        val domain: String = "",
    ) : ConversationListState
    data class NotPaginated(
        val isLoading: Boolean = true,
        val conversations: ImmutableMap<ConversationSection, List<ConversationItem>> = persistentMapOf(),
        val domain: String = "",
    ) : ConversationListState
}
