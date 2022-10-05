package com.wire.android.ui.common.topappbar.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun rememberSearchbarState(): SearchBarState {
    val searchBarState = rememberSaveable(
        saver = SearchBarState.saver()
    ) {
        SearchBarState()
    }

    return searchBarState
}

class SearchBarState(
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
        fun saver(): Saver<SearchBarState, *> = Saver(
            save = {
                listOf(it.isSearchActive, it.searchQuery)
            },
            restore = {
                SearchBarState(
                    isSearchActive = it[0] as Boolean,
                    searchQuery = it[1] as TextFieldValue
                )
            }
        )
    }
}
