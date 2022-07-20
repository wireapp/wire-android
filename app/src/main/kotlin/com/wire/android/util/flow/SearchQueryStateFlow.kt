package com.wire.android.util.flow

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class SearchQueryStateFlow(private val coroutineDispatcher: CoroutineDispatcher) {
    private companion object {
        private const val DEBOUNCE_TIME_MILLIS = 500L
    }

    private val searchQuery = MutableStateFlow("")

    private var outGoingSearchJob: Job? = null

    suspend fun onSearchAction(block: suspend CoroutineScope.(String) -> Unit) {
        coroutineScope {
            searchQuery.debounce(DEBOUNCE_TIME_MILLIS).flowOn(coroutineDispatcher).collect { searchTerm ->
                if (outGoingSearchJob != null) {
                    outGoingSearchJob?.let { job ->
                        if (job.isActive) {
                            job.cancel()
                        }
                    }
                }

                outGoingSearchJob = launch {
                    block(searchTerm)
                }
            }
        }
    }

    fun search(searchTerm: String) {
        searchQuery.value = searchTerm
    }
}
