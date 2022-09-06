package com.wire.android.util.flow

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow

class SearchQueryStateFlow(private val coroutineDispatcher: CoroutineDispatcher) {

    private val searchQuery = MutableStateFlow("")

    fun search(searchTerm: String) {
        searchQuery.value = searchTerm
    }
}
