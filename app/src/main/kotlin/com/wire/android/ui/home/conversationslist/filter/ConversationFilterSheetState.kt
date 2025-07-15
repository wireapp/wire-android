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
package com.wire.android.ui.home.conversationslist.filter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wire.kalium.logic.data.conversation.ConversationFilter
import com.wire.kalium.logic.data.conversation.ConversationFolder
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

class ConversationFilterSheetState(
    conversationFilterSheetData: ConversationFilterSheetData = ConversationFilterSheetData(
        currentFilter = ConversationFilter.All,
        folders = listOf()
    )
) {
    var currentData: ConversationFilterSheetData by mutableStateOf(conversationFilterSheetData)

    fun toFolders() {
        currentData = currentData.copy(tab = FilterTab.FOLDERS)
    }

    fun toFilters() {
        currentData = currentData.copy(tab = FilterTab.FILTERS)
    }
}

@Serializable
data class ConversationFilterSheetData(
    val tab: FilterTab = FilterTab.FILTERS,
    val currentFilter: ConversationFilter,
    val folders: List<ConversationFolder>
)

@Serializable
enum class FilterTab {
    FILTERS,
    FOLDERS
}
