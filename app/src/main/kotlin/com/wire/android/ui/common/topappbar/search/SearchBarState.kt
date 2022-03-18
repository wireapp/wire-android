package com.wire.android.ui.common.topappbar.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.ui.common.effects.ScrollingDownEffect

@Composable
fun rememberSearchbarState(scrollPosition: Int): SearchBarState {
    val searchBarState = remember {
        SearchBarState()
    }

    ScrollingDownEffect(scrollPosition) { shouldCollapse -> searchBarState.isSearchBarCollapsed = shouldCollapse }

    return searchBarState
}

class SearchBarState {

    var isSearchBarCollapsed by mutableStateOf(false)

    var isTopBarVisible by mutableStateOf(true)
        private set

    fun hideTopBar() {
        isTopBarVisible = false
    }

    fun showTopBar() {
        isTopBarVisible = true
    }
}
