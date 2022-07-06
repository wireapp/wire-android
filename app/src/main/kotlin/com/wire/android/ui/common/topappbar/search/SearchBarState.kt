package com.wire.android.ui.common.topappbar.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun rememberSearchbarState(scrollPositionProvider: (() -> Int) = { 0 }): SearchBarState {
    val searchBarState = rememberSaveable(saver = SearchBarState.saver(scrollPositionProvider)) {
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

    var scrollPositionProvider: (() -> Int) by mutableStateOf(scrollPositionProvider)

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
                    isSearchActive = it[0] as Boolean,
                    isSearchBarCollapsed = it[1] as Boolean,
                    // here we are restoring the lambda, providing last scroll position
                    scrollPositionProvider = scrollPositionProvider
                )
            }
        )
    }
}
