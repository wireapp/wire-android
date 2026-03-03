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
package com.wire.android.feature.cells.ui.search.filter.bottomsheet.conversation

import androidx.compose.runtime.*
import com.wire.android.feature.cells.ui.search.filter.data.FilterConversationUi

class ConversationFilterSheetState(
    initialItems: List<FilterConversationUi>
) {
    private val initialById = initialItems.associateBy { it.id }

    var conversations by mutableStateOf(initialItems)
        private set

    var query by mutableStateOf("")
        private set

    val hasChanges: Boolean
        get() = conversations.any { o -> initialById[o.id]?.selected != o.selected }


    val filteredConversations: List<FilterConversationUi>
        get() {
            val q = query.trim()
            return if (q.isBlank()) conversations
            else conversations.filter { it.name.contains(q, ignoreCase = true) }
        }

    fun onQueryChange(text: String) {
        query = text
    }

    fun selectConversation(id: String) {
        conversations = conversations.map { conversation ->
            when (conversation.id.toString()) {
                id -> conversation.copy(selected = !conversation.selected)
                else -> conversation.copy(selected = false)
            }
        }
    }

    fun removeAll() {
        conversations = conversations.map { it.copy(selected = false) }
    }

    fun selectedConversation(): List<FilterConversationUi> =
        conversations.filter { it.selected }
}

@Composable
fun rememberConversationFilterSheetState(
    items: List<FilterConversationUi>
): ConversationFilterSheetState {
    return remember(items) {
        ConversationFilterSheetState(
            initialItems = items
        )
    }
}
