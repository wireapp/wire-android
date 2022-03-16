package com.wire.android.ui.common.topappbar.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

@Composable
fun rememberSearchbarState(scrollPosition: Int): SearchBarState {
    val searchBarState = remember {
        SearchBarState()
    }

    LaunchedEffect(scrollPosition) {
        snapshotFlow { scrollPosition }
            .scan(0 to 0) { prevPair, newScrollIndex ->
                if (prevPair.second == newScrollIndex || newScrollIndex == prevPair.second + 1) prevPair
                else prevPair.second to newScrollIndex
            }
            .map { (prevScrollIndex, newScrollIndex) ->
                newScrollIndex > prevScrollIndex + 1
            }
            .distinctUntilChanged().collect { shouldCollapse ->
                searchBarState.isSearchBarCollapsed = shouldCollapse
            }
    }

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
