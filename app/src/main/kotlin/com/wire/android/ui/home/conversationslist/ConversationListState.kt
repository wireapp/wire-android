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
import com.wire.android.ui.home.conversationslist.model.ConversationFolderItem
import com.wire.kalium.logic.data.conversation.ConversationFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Stable
data class ConversationListState(
    val foldersWithConversations: Flow<PagingData<ConversationFolderItem>> = emptyFlow(),
    val filter: ConversationFilter = ConversationFilter.NONE,
    val domain: String = ""
)
