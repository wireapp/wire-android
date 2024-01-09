/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.ContactMapper
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.kalium.logic.feature.search.SearchUsersUseCase
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.service.SearchServicesByNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {
    var state: SearchState by mutableStateOf(SearchState())
        private set

    private val _userSearchSignal: MutableSharedFlow<String> = MutableSharedFlow()
    val userSearchSignal: Flow<String> = _userSearchSignal
        .debounce(DEFAULT_SEARCH_QUERY_DEBOUNCE)

    private val _serviceSearchSignal: MutableSharedFlow<String> = MutableSharedFlow()
    val serviceSearchSignal: Flow<String> = _serviceSearchSignal
        .debounce(DEFAULT_SEARCH_QUERY_DEBOUNCE)

    fun onServiceSearchQueryChanged(query: TextFieldValue) {
        appLogger.d("onServiceSearchQueryChanged: ${query.text}")
        state = state.copy(serviceSearchQuery = query)
        viewModelScope.launch {
            _serviceSearchSignal.emit(query.text)
        }
    }

    fun onUserSearchQueryChanged(query: TextFieldValue) {
        appLogger.d("onUserSearchQueryChanged: ${query.text}")
        state = state.copy(userSearchQuery = query)
        viewModelScope.launch {
            _userSearchSignal.emit(query.text)
        }
    }
}

data class SearchState(
    val serviceSearchQuery: TextFieldValue = TextFieldValue(),
    val userSearchQuery: TextFieldValue = TextFieldValue(),
    val isServicesAllowed: Boolean = false,
    val isGroupCreationContext: Boolean = false
)

@HiltViewModel
class SearchUserViewModel @Inject constructor(
    private val searchUserUseCase: SearchUsersUseCase,
    private val contactMapper: ContactMapper
) : ViewModel() {
    var state: SearchUserState by mutableStateOf(SearchUserState())
        private set

    init {
        viewModelScope.launch {
            searchUserUseCase("", customDomain = null, excludingConversation = null).also { userSearchEntities ->
                state = state.copy(
                    contactsResult = userSearchEntities.connected.map(contactMapper::fromSearchUserResult).toImmutableList(),
                    publicResult = userSearchEntities.notConnected.map(contactMapper::fromSearchUserResult).toImmutableList()
                )
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            searchUserUseCase(query, excludingConversation = null, customDomain = null).also { userSearchEntities ->
                state = state.copy(
                    contactsResult = userSearchEntities.connected.map(contactMapper::fromSearchUserResult).toImmutableList(),
                    publicResult = userSearchEntities.notConnected.map(contactMapper::fromSearchUserResult).toImmutableList()
                )
            }
        }
    }
}

@HiltViewModel
class SearchServicesViewModel @Inject constructor(
    private val getAllServices: ObserveAllServicesUseCase,
    private val contactMapper: ContactMapper,
    private val searchServicesByName: SearchServicesByNameUseCase
) : ViewModel() {

    var state: SearchServicesState by mutableStateOf(SearchServicesState())
        private set

    fun search(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                getAllServices().first().also { services ->
                    state = state.copy(result = services.map(contactMapper::fromService).toImmutableList())
                }
            } else {
                searchServicesByName(query).first().also { services ->
                    state = state.copy(result = services.map(contactMapper::fromService).toImmutableList())
                }
            }
        }
    }
}

data class SearchServicesState(
    val result: ImmutableList<Contact> = persistentListOf(),
    val searchQuery: TextFieldValue = TextFieldValue(),
    val noneSearchSucceeded: Boolean = false,
    val isLoading: Boolean = false,
    val error: Boolean = false
)

data class SearchUserState(
    val contactsResult: ImmutableList<Contact> = persistentListOf(),
    val publicResult: ImmutableList<Contact> = persistentListOf(),
    val includeServices: Boolean = false,
    val noneSearchSucceeded: Boolean = false
)
