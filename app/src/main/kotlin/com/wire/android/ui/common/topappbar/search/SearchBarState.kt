package com.wire.android.ui.common.topappbar.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.ui.common.effects.ScrollingDownEffect

@Composable
fun rememberSearchbarState(): SearchBarState {
    val searchBarState = remember {
        SearchBarState()
    }

    return searchBarState
}

class SearchBarState {

    var isSearchBarCollapsed by mutableStateOf(false)

    var isSearchActive by mutableStateOf(false)
        private set

    fun cancelSearch() {
        isSearchActive = false
    }

    fun startSearch() {
        isSearchActive = true
    }
}
