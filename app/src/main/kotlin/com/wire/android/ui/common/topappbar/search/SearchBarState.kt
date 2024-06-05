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

package com.wire.android.ui.common.topappbar.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun rememberSearchbarState(
    searchQueryTextState: TextFieldState = rememberTextFieldState()
): SearchBarState = rememberSaveable(
    saver = SearchBarState.saver(searchQueryTextState)
) {
    SearchBarState(searchQueryTextState = searchQueryTextState)
}

class SearchBarState(
    isSearchActive: Boolean = false,
    val searchQueryTextState: TextFieldState
) {

    var isSearchActive by mutableStateOf(isSearchActive)
        private set

    fun closeSearch() {
        isSearchActive = false
    }

    fun openSearch() {
        isSearchActive = true
    }

    fun searchActiveChanged(isSearchActive: Boolean) {
        this.isSearchActive = isSearchActive
    }

    companion object {
        fun saver(searchQueryTextState: TextFieldState): Saver<SearchBarState, *> = Saver(
            save = {
                listOf(it.isSearchActive)
            },
            restore = {
                SearchBarState(
                    isSearchActive = it[0],
                    searchQueryTextState = searchQueryTextState
                )
            }
        )
    }
}
