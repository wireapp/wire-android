package com.wire.android.ui.common.topappbar.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.io.Serializable

@Composable
fun rememberSearchbarState(): SearchBarState {
    val searchBarState = rememberSaveable(saver = SearchBarState.saver()) {
        SearchBarState(
            isSearchActive = false,
            isSearchBarCollapsed = false,
            scrollPositionProvider = ScrollPositionProvider()
        )
    }

    return searchBarState
}

data class ScrollPositionProvider(val scrollPositionProvider: (() -> Int) = { 0 }) : Serializable

class SearchBarState(
    isSearchActive: Boolean,
    isSearchBarCollapsed: Boolean,
    scrollPositionProvider: ScrollPositionProvider
) {

    var isSearchBarCollapsed by mutableStateOf(isSearchBarCollapsed)

    var isSearchActive by mutableStateOf(isSearchActive)
        private set

    var scrollPositionProvider: ScrollPositionProvider by mutableStateOf(scrollPositionProvider)

    fun closeSearch() {
        isSearchActive = false
    }

    fun openSearch() {
        isSearchActive = true
    }

    companion object {
        fun saver(): Saver<SearchBarState, *> = Saver(
            save = { listOf(it.isSearchActive, it.isSearchBarCollapsed, it.scrollPositionProvider.scrollPositionProvider()) },
            restore = {
                SearchBarState(
                    isSearchActive = it[0] as Boolean,
                    isSearchBarCollapsed = it[1] as Boolean,
                    scrollPositionProvider = ScrollPositionProvider { it[2] as Int }
                )
            }
        )
    }
}
