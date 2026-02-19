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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.mapper.ContactMapper
import com.wire.android.ui.common.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.android.ui.home.newconversation.model.Contact
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.feature.auth.ValidateUserHandleResult
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.search.FederatedSearchParser
import com.wire.kalium.logic.feature.search.IsFederationSearchAllowedUseCase
import com.wire.kalium.logic.feature.search.SearchByHandleUseCase
import com.wire.kalium.logic.feature.search.SearchUserResult
import com.wire.kalium.logic.feature.search.SearchUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchUserViewModel @Inject constructor(
    private val searchUserUseCase: SearchUsersUseCase,
    private val searchByHandleUseCase: SearchByHandleUseCase,
    private val contactMapper: ContactMapper,
    private val federatedSearchParser: FederatedSearchParser,
    private val validateUserHandle: ValidateUserHandleUseCase,
    private val isFederationSearchAllowed: IsFederationSearchAllowedUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    @Suppress("TooGenericExceptionCaught")
    private val addMembersSearchNavArgs: AddMembersSearchNavArgs? = try {
        savedStateHandle.navArgs<AddMembersSearchNavArgs>()
    } catch (e: RuntimeException) {
        null
    }

    private val searchQueryTextFlow = MutableStateFlow(String.EMPTY)
    private val selectedContactsFlow = MutableStateFlow<ImmutableSet<Contact>>(persistentSetOf())
    var state: SearchUserState by mutableStateOf(SearchUserState(isLoading = true))
        private set

    init {
        viewModelScope.launch {
            val isOtherDomainAllowed = isFederationSearchAllowed(addMembersSearchNavArgs?.conversationId)
            state = state.copy(isOtherDomainAllowed = isOtherDomainAllowed)
        }

        viewModelScope.launch {
            searchQueryTextFlow
                .debounce(DEFAULT_SEARCH_QUERY_DEBOUNCE)
                .onStart { emit(String.EMPTY) }
                .flatMapLatest { searchQuery ->
                    val (searchTerm, domain) = federatedSearchParser(searchQuery, state.isOtherDomainAllowed)
                    val isHandleSearch = validateUserHandle(searchTerm.removeQueryPrefix()) is ValidateUserHandleResult.Valid
                    val searchResult = if (isHandleSearch) {
                        searchByHandle(searchTerm, domain)
                    } else {
                        searchByName(searchTerm, domain)
                    }
                    selectedContactsFlow
                        .mapLatest { selectedContacts ->
                            filterResults(searchResult, selectedContacts, isHandleSearch, searchTerm)
                        }
                }
                .collectLatest { newState ->
                    state = state.copy(
                        contactsResult = newState.contactsResult,
                        publicResult = newState.publicResult,
                        selectedResult = newState.selectedResult,
                        searchQuery = newState.searchQuery,
                        isLoading = false,
                    )
                }
        }
    }

    fun searchQueryChanged(searchQuery: String) {
        viewModelScope.launch {
            searchQueryTextFlow.emit(searchQuery)
        }
    }

    fun selectedContactsChanged(selectedContacts: ImmutableSet<Contact>) {
        viewModelScope.launch {
            selectedContactsFlow.emit(selectedContacts)
        }
    }

    private fun filterResults(
        searchResult: SearchUserResult,
        selectedContacts: ImmutableSet<Contact>,
        isHandleSearch: Boolean,
        searchTerm: String
    ): SearchUserState {
        val selectedContactsIds = selectedContacts.map { it.id }
        val notSelectedContactsResult = searchResult.connected
            .map(contactMapper::fromSearchUserResult)
            .filterNot { selectedContactsIds.contains(it.id) }
        val notSelectedPublicResult = searchResult.notConnected
            .map(contactMapper::fromSearchUserResult)
            .filterNot { selectedContactsIds.contains(it.id) }
        val selectedContactsResult = selectedContacts
            .filter { selectedContact ->
                when {
                    isHandleSearch -> selectedContact.handle.contains(searchTerm, ignoreCase = true)
                    else -> selectedContact.name.contains(searchTerm, ignoreCase = true)
                }
            }
        return SearchUserState(
            contactsResult = notSelectedContactsResult.toImmutableList(),
            publicResult = notSelectedPublicResult.toImmutableList(),
            selectedResult = selectedContactsResult.toImmutableList(),
            searchQuery = searchTerm
        )
    }

    private suspend fun searchByHandle(searchTerm: String, domain: String?): SearchUserResult =
        searchByHandleUseCase(
            searchTerm,
            excludingConversation = addMembersSearchNavArgs?.conversationId,
            customDomain = domain
        )

    private suspend fun searchByName(searchTerm: String, domain: String?): SearchUserResult =
        searchUserUseCase(
            searchTerm,
            excludingMembersOfConversation = addMembersSearchNavArgs?.conversationId,
            customDomain = domain
        )
}

data class SearchUserState(
    val contactsResult: ImmutableList<Contact> = persistentListOf(),
    val publicResult: ImmutableList<Contact> = persistentListOf(),
    val selectedResult: ImmutableList<Contact> = persistentListOf(),
    val searchQuery: String = String.EMPTY,
    val isOtherDomainAllowed: Boolean = false,
    val isLoading: Boolean = false,
)
