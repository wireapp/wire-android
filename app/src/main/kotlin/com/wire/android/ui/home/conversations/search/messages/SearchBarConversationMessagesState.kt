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
package com.wire.android.ui.home.conversations.search.messages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun rememberSearchBarConversationMessagesState(): SearchBarConversationMessagesState {
    val searchBarState = rememberSaveable(
        saver = SearchBarConversationMessagesState.saver()
    ) {
        SearchBarConversationMessagesState()
    }

    return searchBarState
}

class SearchBarConversationMessagesState(
    isSearchActive: Boolean = false,
    searchQuery: TextFieldValue = TextFieldValue("")
) {

    var isSearchActive by mutableStateOf(isSearchActive)
        private set

    var searchQuery by mutableStateOf(searchQuery)

    fun closeSearch() {
        isSearchActive = false
    }

    fun openSearch() {
        isSearchActive = true
    }

    fun searchQueryChanged(searchQuery: TextFieldValue) {
        this.searchQuery = searchQuery
    }

    companion object {

        fun saver(): Saver<SearchBarConversationMessagesState, *> = Saver(
            save = {
                listOf(it.isSearchActive, it.searchQuery.text)
            },
            restore = {
                SearchBarConversationMessagesState(
                    isSearchActive = it[0] as Boolean,
                    searchQuery = TextFieldValue(it[1] as String)
                )
            }
        )
    }
}