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
import androidx.compose.foundation.text.input.clearText
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
    val searchQueryTextState: TextFieldState,
    shouldClearSearchOnResume: Boolean = false,
) {

    var isSearchActive by mutableStateOf(isSearchActive)
        private set

    var isSearchVisible by mutableStateOf(isSearchVisible)
        private set

    private var shouldClearSearchOnResume = shouldClearSearchOnResume

    fun closeSearch() {
        isSearchActive = false
    }

    fun clearSearch() {
        shouldClearSearchOnResume = false
        searchQueryTextState.clearText()
        closeSearch()
    }

    fun openSearch() {
        isSearchActive = true
    }

    fun searchActiveChanged(isSearchActive: Boolean) {
        this.isSearchActive = isSearchActive
    }

    fun searchVisibleChanged(isSearchVisible: Boolean) {
        this.isSearchVisible = isSearchVisible
    }

    fun requestClearSearchOnNextResume() {
        shouldClearSearchOnResume = isSearchActive || searchQueryTextState.text.isNotEmpty()
    }

    fun clearSearchOnResumeIfRequested() {
        if (shouldClearSearchOnResume) {
            shouldClearSearchOnResume = false
            clearSearch()
        }
    }

    companion object {
        fun saver(): Saver<SearchBarState, List<Any?>> = Saver(
            save = {
                listOf(
                    it.isSearchActive,
                    with(TextFieldState.Saver) {
                        save(it.searchQueryTextState)
                    },
                    it.isSearchVisible,
                    it.shouldClearSearchOnResume,
                )
            },
            restore = {
                SearchBarState(
                    isSearchActive = (it.getOrNull(IS_SEARCH_ACTIVE_INDEX) as? Boolean) ?: false,
                    searchQueryTextState = it.getOrNull(SEARCH_QUERY_TEXT_STATE_INDEX)?.let {
                        with(TextFieldState.Saver) {
                            restore(it)
                        }
                    } ?: TextFieldState(),
                    isSearchVisible = (it.getOrNull(IS_SEARCH_VISIBLE_INDEX) as? Boolean) ?: true,
                    shouldClearSearchOnResume = (it.getOrNull(SHOULD_CLEAR_SEARCH_ON_RESUME_INDEX) as? Boolean) ?: false
                )
            }
        )

        private const val IS_SEARCH_ACTIVE_INDEX = 0
        private const val SEARCH_QUERY_TEXT_STATE_INDEX = 1
        private const val IS_SEARCH_VISIBLE_INDEX = 2
        private const val SHOULD_CLEAR_SEARCH_ON_RESUME_INDEX = 3
    }
}
