package com.wire.android.ui.common.topappbar.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun rememberSearchbarState(scrollPositionProvider: (() -> Int) = { 0 }): SearchBarState {
    val searchBarState = rememberSaveable(
        saver = SearchBarState.saver(scrollPositionProvider)
    ) {
        SearchBarState()
    }

    return searchBarState
}

class SearchBarState(
    isSearchActive: Boolean = false,
    isSearchBarCollapsed: Boolean = false,
    scrollPositionProvider: (() -> Int) = { 0 }
) {

    var isSearchBarCollapsed by mutableStateOf(isSearchBarCollapsed)

    var isSearchActive by mutableStateOf(isSearchActive)
        private set

    fun closeSearch() {
        isSearchActive = false
    }

    fun openSearch() {
        isSearchActive = true
    }

    companion object {
        fun saver(scrollPositionProvider: (() -> Int) = { 0 }): Saver<SearchBarState, *> = Saver(
            save = {
                listOf(it.isSearchActive, it.isSearchBarCollapsed)
            },
            restore = {
                SearchBarState(
                    isSearchActive = it[0],
                    isSearchBarCollapsed = it[1],
                    scrollPositionProvider = scrollPositionProvider
                )
            }
        )
    }
}
