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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.feature.cells.ui.search.filter.data.FilterConversationUi

/**
 * Manages the selection state for the conversation filter bottom sheet.
 * Tracks a single selected [FilterConversationUi] and whether it has changed from the
 * value that was provided when the sheet was opened.
 */
class ConversationFilterSheetState(initialSelected: FilterConversationUi?) {

    private val initialSelectedId = initialSelected?.id

    var selectedConversation by mutableStateOf(initialSelected)
        private set

    val hasChanges: Boolean
        get() = selectedConversation?.id != initialSelectedId

    fun selectConversation(conversation: FilterConversationUi) {
        selectedConversation = if (selectedConversation?.id == conversation.id) {
            null
        } else {
            conversation
        }
    }

    fun removeAll() {
        selectedConversation = null
    }
}

@Composable
fun rememberConversationFilterSheetState(
    selectedConversation: FilterConversationUi?,
): ConversationFilterSheetState {
    return remember(selectedConversation) {
        ConversationFilterSheetState(initialSelected = selectedConversation)
    }
}
