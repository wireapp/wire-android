/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

package com.wire.android.ui.common.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun rememberSearchbarState(
    initialIsSearchActive: Boolean = false,
    searchQueryTextState: TextFieldState = rememberTextFieldState()
): SearchBarState = rememberSaveable(
    saver = SearchBarState.saver()
) {
    SearchBarState(isSearchActive = initialIsSearchActive, searchQueryTextState = searchQueryTextState)
}

class SearchBarState(
    isSearchActive: Boolean = false,
    isSearchVisible: Boolean = true,
    isFilterActive: Boolean = false,
    val searchQueryTextState: TextFieldState
) {

    var isSearchActive by mutableStateOf(isSearchActive)
        private set

    var isSearchVisible by mutableStateOf(isSearchVisible)
        private set

    var isFilterActive by mutableStateOf(isFilterActive)
        private set

    fun closeSearch() {
        isSearchActive = false
    }

    fun openSearch() {
        isSearchActive = true
    }

    fun onFilterActiveChanged(isFilterActive: Boolean) {
        this.isFilterActive = isFilterActive
    }

    fun searchActiveChanged(isSearchActive: Boolean) {
        this.isSearchActive = isSearchActive
    }

    fun searchVisibleChanged(isSearchVisible: Boolean) {
        this.isSearchVisible = isSearchVisible
    }

    companion object {
        fun saver(): Saver<SearchBarState, *> = Saver(
            save = {
                listOf(
                    it.isSearchActive,
                    with(TextFieldState.Saver) {
                        save(it.searchQueryTextState)
                    }
                )
            },
            restore = {
                SearchBarState(
                    isSearchActive = (it.getOrNull(0) as? Boolean) ?: false,
                    searchQueryTextState = it.getOrNull(1)?.let {
                        with(TextFieldState.Saver) {
                            restore(it)
                        }
                    } ?: TextFieldState()
                )
            }
        )
    }
}
