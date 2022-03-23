package com.wire.android.ui.home.newconversation.search

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope


@Composable
fun rememberSearchPeopleScreenState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    lazyListState: LazyListState = rememberLazyListState()
): SearchPeopleScreenState {
    return remember {
        SearchPeopleScreenState(
            coroutineScope = coroutineScope,
            lazyListState = lazyListState
        )
    }
}

class SearchPeopleScreenState(
    val coroutineScope: CoroutineScope,
    val lazyListState: LazyListState,
) {

    var contactsAllResultsCollapsed: Boolean by mutableStateOf(false)

    var publicResultsCollapsed: Boolean by mutableStateOf(false)

    var federatedBackendResultsCollapsed: Boolean by mutableStateOf(false)

    fun toggleShowAllContactsResult() {
        contactsAllResultsCollapsed = !contactsAllResultsCollapsed
    }

    fun toggleShowAllPublicResult() {
        publicResultsCollapsed = !publicResultsCollapsed
    }

    fun toggleShowFederatedBackendResult() {
        federatedBackendResultsCollapsed = !federatedBackendResultsCollapsed
    }
}
