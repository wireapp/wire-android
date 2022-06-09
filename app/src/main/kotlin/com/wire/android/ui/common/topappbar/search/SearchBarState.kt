package com.wire.android.ui.common.topappbar.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun rememberSearchbarState(): SearchBarState {
    val searchBarState = rememberSaveable(saver = SearchBarState.saver()) {
        SearchBarState(
            isSearchActive = false,
            isSearchBarCollapsed = false,
            scrollPositionProvider = null
        )
    }

    return searchBarState
}

class SearchBarState(
    isSearchActive: Boolean,
    isSearchBarCollapsed: Boolean,
    scrollPositionProvider: (() -> Int)?
) {

    var isSearchBarCollapsed by mutableStateOf(isSearchBarCollapsed)

    var isSearchActive by mutableStateOf(isSearchActive)
        private set

    var scrollPositionProvider: (() -> Int)? by mutableStateOf(null)

    fun closeSearch() {
        isSearchActive = false
    }

    fun openSearch() {
        isSearchActive = true
    }

    companion object {
        fun saver(): Saver<SearchBarState, *> = Saver(
            save = { Triple(it.isSearchActive, it.isSearchBarCollapsed, it.scrollPositionProvider) },
            restore = { (isSearchActive, isSearchBarCollapsed, scrollPositionProvider) ->
                SearchBarState(isSearchActive, isSearchBarCollapsed, scrollPositionProvider)
            }
        )
    }
}
