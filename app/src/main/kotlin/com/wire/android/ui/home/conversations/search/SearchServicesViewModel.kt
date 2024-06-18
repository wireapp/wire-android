/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.conversations.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.mapper.ContactMapper
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.service.SearchServicesByNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchServicesViewModel @Inject constructor(
    private val getAllServices: ObserveAllServicesUseCase,
    private val contactMapper: ContactMapper,
    private val searchServicesByName: SearchServicesByNameUseCase,
) : ViewModel() {
    private val searchQueryTextFlow = MutableStateFlow(String.EMPTY)
    var state: SearchServicesState by mutableStateOf(SearchServicesState())
        private set

    init {
        viewModelScope.launch {
            searchQueryTextFlow
                .debounce(DEFAULT_SEARCH_QUERY_DEBOUNCE)
                .onStart { emit(String.EMPTY) }
                .collectLatest { query ->
                    search(query)
                }
        }
    }

    fun searchQueryChanged(searchQuery: String) {
        viewModelScope.launch {
            searchQueryTextFlow.emit(searchQuery)
        }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                getAllServices().first().also { services ->
                    state = state.copy(result = services.map(contactMapper::fromService).toImmutableList(), searchQuery = query)
                }
            } else {
                searchServicesByName(query).first().also { services ->
                    state = state.copy(result = services.map(contactMapper::fromService).toImmutableList(), searchQuery = query)
                }
            }
        }
    }
}

data class SearchServicesState(
    val result: ImmutableList<Contact> = persistentListOf(),
    val searchQuery: String = String.EMPTY,
    val isLoading: Boolean = false,
    val error: Boolean = false
)
