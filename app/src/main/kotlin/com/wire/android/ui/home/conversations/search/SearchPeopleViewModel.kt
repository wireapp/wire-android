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
 *
 *
 */

package com.wire.android.ui.home.conversations.search

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.mapper.ContactMapper
import com.wire.android.model.SnackBarMessage
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsResult
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchPublicUsersUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchUsersResult
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions", "LongParameterList")
open class SearchAllPeopleViewModel(
    private val getAllKnownUsers: GetAllContactsUseCase,
    private val searchKnownUsers: SearchKnownUsersUseCase,
    private val searchPublicUsers: SearchPublicUsersUseCase,
    private val getAllServices: ObserveAllServicesUseCase,
    private val contactMapper: ContactMapper,
    private val dispatcher: DispatcherProvider,
    sendConnectionRequest: SendConnectionRequestUseCase
) : PublicWithKnownPeopleSearchViewModel(
    sendConnectionRequest = sendConnectionRequest,
) {

    var state: SearchPeopleState by mutableStateOf(SearchPeopleState(isGroupCreationContext = true))

    init {
        viewModelScope.launch {
            combine(
                initialContactResultFlow(),
                publicPeopleSearchQueryFlow,
                knownPeopleSearchQueryFlow,
                searchQueryTextFieldFlow,
                selectedContactsFlow
            ) { initialContacts, publicResult, knownResult, searchQuery, selectedContacts ->

                SearchPeopleState(
                    initialContacts = initialContacts,
                    searchQuery = searchQuery,
                    searchResult = persistentMapOf(
                        SearchResultTitle(R.string.label_contacts) to knownResult,
                        SearchResultTitle(R.string.label_public_wire) to publicResult.filterContacts(
                            knownResult
                        )
                    ),
                    noneSearchSucceed =
                    (publicResult.searchResultState is SearchResultState.Failure
                            || publicResult.searchResultState is SearchResultState.EmptyResult)
                            && (knownResult.searchResultState is SearchResultState.Failure
                            || knownResult.searchResultState is SearchResultState.EmptyResult),
                    contactsAddedToGroup = selectedContacts.toImmutableList(),
                    isGroupCreationContext = true
                )
            }.collect { updatedState ->
                state = updatedState
            }
        }
    }

    override suspend fun getInitialContacts(): Flow<SearchResult> = getAllKnownUsers()
        .map { result ->
            when (result) {
                is GetAllContactsResult.Failure -> SearchResult.Failure(R.string.label_general_error)
                is GetAllContactsResult.Success -> SearchResult.Success(
                    result.allContacts.map(
                        contactMapper::fromOtherUser
                    )
                )
            }
        }

    override suspend fun getInitialServices(): Flow<SearchResult> =
        getAllServices().map { result ->
            SearchResult.Success(result.map(contactMapper::fromService))
        }

    override suspend fun searchKnownPeople(searchTerm: String): Flow<ContactSearchResult.InternalContact> =
        searchKnownUsers(searchTerm).flowOn(dispatcher.io()).map { result ->
            when (result) {
                is SearchUsersResult.Failure.Generic, SearchUsersResult.Failure.InvalidRequest ->
                    ContactSearchResult.InternalContact(SearchResultState.Failure(R.string.label_general_error))

                SearchUsersResult.Failure.InvalidQuery ->
                    ContactSearchResult.InternalContact(SearchResultState.Failure(R.string.label_no_results_found))

                is SearchUsersResult.Success -> ContactSearchResult.InternalContact(
                    if (result.userSearchResult.result.isEmpty()) SearchResultState.EmptyResult
                    else SearchResultState.Success(
                        result.userSearchResult.result.map(contactMapper::fromOtherUser)
                            .toImmutableList()
                    )
                )
            }
        }

    override suspend fun searchPublicPeople(searchTerm: String): Flow<ContactSearchResult.ExternalContact> {
        return searchPublicUsers(searchTerm).map { result ->
            when (result) {
                is SearchUsersResult.Failure.Generic, SearchUsersResult.Failure.InvalidRequest ->
                    ContactSearchResult.ExternalContact(
                        SearchResultState.Failure(R.string.label_general_error)
                    )

                SearchUsersResult.Failure.InvalidQuery -> ContactSearchResult.ExternalContact(
                    SearchResultState.Failure(R.string.label_no_results_found)
                )

                is SearchUsersResult.Success -> ContactSearchResult.ExternalContact(
                    if (result.userSearchResult.result.isEmpty()) SearchResultState.EmptyResult
                    else SearchResultState.Success(
                        result.userSearchResult.result.map(contactMapper::fromOtherUser)
                            .toImmutableList()
                    )
                )
            }
        }
            .flowOn(dispatcher.io())
    }

    override suspend fun searchServices(searchTerm: String): Flow<SearchResultState> {
        // TODO(Service): This only needs to be implemented when adding services to group when
        // group is being created.
        // Currently not supported.
        return flowOf(SearchResultState.EmptyResult)
    }

    private fun ContactSearchResult.filterContacts(contactSearchResult: ContactSearchResult): ContactSearchResult {
        return if (searchResultState is SearchResultState.Success &&
            contactSearchResult.searchResultState is SearchResultState.Success
        ) {
            ContactSearchResult.ExternalContact(SearchResultState.Success(
                searchResultState.result
                    .filterNot { contact ->
                        contactSearchResult.searchResultState.result
                            .map { it.id }
                            .contains(contact.id)
                    }
                    .toImmutableList()
            ))
        } else {
            this
        }
    }
}

abstract class PublicWithKnownPeopleSearchViewModel(
    private val sendConnectionRequest: SendConnectionRequestUseCase,
) : KnownPeopleSearchViewModel() {

    protected val publicPeopleSearchQueryFlow = mutableSearchQueryFlow
        .flatMapLatest { searchTerm ->
            searchPublicPeople(searchTerm)
                .onStart {
                    emit(ContactSearchResult.ExternalContact(SearchResultState.InProgress))
                }
        }

    fun addContact(contact: Contact) {
        viewModelScope.launch {
            val userId = UserId(contact.id, contact.domain)

            when (sendConnectionRequest(userId)) {
                is SendConnectionRequestResult.Success -> {
                    mutableInfoMessage.emit(SearchPeopleMessageType.SuccessConnectionSentRequest.uiText)
                }

                is SendConnectionRequestResult.Failure.FederationDenied, is SendConnectionRequestResult.Failure.GenericFailure -> {
                    appLogger.d(("Couldn't send a connect request to user $userId"))
                }
            }
        }
    }

    abstract suspend fun searchPublicPeople(searchTerm: String): Flow<ContactSearchResult.ExternalContact>
}

@OptIn(ExperimentalCoroutinesApi::class)
abstract class KnownPeopleSearchViewModel : SearchServicesViewModel() {

    protected val knownPeopleSearchQueryFlow = mutableSearchQueryFlow
        .flatMapLatest { searchTerm ->
            searchKnownPeople(searchTerm)
                .onStart {
                    emit(ContactSearchResult.InternalContact(SearchResultState.InProgress))
                }
        }

    abstract suspend fun searchKnownPeople(searchTerm: String): Flow<ContactSearchResult.InternalContact>
}

abstract class SearchPeopleViewModel : ViewModel() {
    companion object {
        const val DEFAULT_SEARCH_QUERY_DEBOUNCE = 500L
    }

    protected val mutableSearchQueryFlow = MutableStateFlow("")

    protected val searchQueryFlow = mutableSearchQueryFlow
        .asStateFlow()
        .debounce(DEFAULT_SEARCH_QUERY_DEBOUNCE)

    suspend fun initialContactResultFlow() = getInitialContacts().map { result ->
        when (result) {
            is SearchResult.Failure -> {
                SearchResultState.Failure(result.failureString)
            }

            is SearchResult.Success -> {
                result
                    .contacts
                    .sortedBy { it.name.lowercase() }
                    .toImmutableList()
                    .let {
                        if (it.isEmpty()) SearchResultState.EmptyResult
                        else SearchResultState.Success(it)
                    }
            }
        }
    }

    suspend fun initialServicesResultFlow() = getInitialServices().map { result ->
        when (result) {
            is SearchResult.Failure -> {
                SearchResultState.Failure(result.failureString)
            }

            is SearchResult.Success -> {
                SearchResultState.Success(result.contacts.toImmutableList())
            }
        }
    }

    protected val searchQueryTextFieldFlow = MutableStateFlow(TextFieldValue(""))

    protected val selectedContactsFlow = MutableStateFlow(emptyList<Contact>())

    protected val mutableInfoMessage = MutableSharedFlow<UIText>()
    val infoMessage = mutableInfoMessage.asSharedFlow()

    fun searchQueryChanged(searchQuery: TextFieldValue) {
        val textQueryChanged = searchQueryTextFieldFlow.value.text != searchQuery.text
        // we set the state with a searchQuery, immediately to update the UI first
        viewModelScope.launch {
            searchQueryTextFieldFlow.emit(searchQuery)

            if (textQueryChanged) mutableSearchQueryFlow.emit(searchQuery.text)
        }
    }

    /**
     * Do not under any circumstances call this function in a loop.
     * It will cause the to emit a value each time. This in turn
     * will result in synchronous execution of functions on the main
     * thread like to use outdated values.
     */
    fun addContactToGroup(contact: Contact) {
        viewModelScope.launch {
            selectedContactsFlow.emit(selectedContactsFlow.value + contact)
        }
    }

    /**
     * Do not under any circumstances call this function in a loop.
     * It will cause the state to emit a value each time. This in turn
     * will result in synchronous execution of functions on the main
     * thread like to use outdated values.
     * Use [removeContactsFromGroup] instead.
     */
    fun removeContactFromGroup(contact: Contact) {
        removeContactsFromGroup(setOf(contact))
    }

    fun removeContactsFromGroup(contacts: Set<Contact>) {
        viewModelScope.launch {
            selectedContactsFlow.emit(selectedContactsFlow.value - contacts)
        }
    }

    abstract suspend fun getInitialContacts(): Flow<SearchResult>

    abstract suspend fun getInitialServices(): Flow<SearchResult>
}

// Different use cases could return different type for the search, we are making sure here
// that the type is mapped to a SearchResult that can further used in this class
sealed class SearchResult {
    data class Success(val contacts: List<Contact>) : SearchResult()
    data class Failure(@StringRes val failureString: Int) : SearchResult()
}

sealed class SearchPeopleMessageType(override val uiText: UIText) : SnackBarMessage {
    object SuccessConnectionSentRequest : SearchPeopleMessageType(UIText.StringResource(R.string.connection_request_sent))
}
