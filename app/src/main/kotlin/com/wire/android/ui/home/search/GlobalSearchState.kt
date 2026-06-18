/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.home.search

import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.id.ConversationId

data class GlobalSearchState(
    val searchQuery: String = String.EMPTY,
    val selectedFilter: GlobalSearchFilter = GlobalSearchFilter.All,
    val results: List<GlobalSearchResultItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
) {
    val visibleResults: List<GlobalSearchResultItem>
        get() = when (selectedFilter) {
            GlobalSearchFilter.All,
            GlobalSearchFilter.Messages -> results

            GlobalSearchFilter.People,
            GlobalSearchFilter.Files,
            GlobalSearchFilter.Links,
            GlobalSearchFilter.Media -> emptyList()
        }
}

enum class GlobalSearchFilter(
    val isEnabled: Boolean
) {
    All(isEnabled = true),
    Messages(isEnabled = true),
    People(isEnabled = false),
    Files(isEnabled = false),
    Links(isEnabled = false),
    Media(isEnabled = false),
}

data class GlobalSearchResultItem(
    val messageId: String,
    val conversationId: ConversationId,
    val senderName: String,
    val conversationName: String,
    val preview: String,
)
