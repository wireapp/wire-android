package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.home.newconversation.search.ContactSearchResult
import com.wire.android.ui.home.newconversation.search.SearchPeopleState
import com.wire.android.ui.home.newconversation.search.SearchResultState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.flow.SearchQueryStateFlow
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContextg

abstract class SearchConversationViewModel(
    val navigationManager: NavigationManager,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    val dispatchers: DispatcherProvider
) : ViewModel() {

    private var innerSearchPeopleState: SearchPeopleState by mutableStateOf(SearchPeopleState())

    private var localContactSearchResult by mutableStateOf(
        ContactSearchResult.InternalContact(searchResultState = SearchResultState.Initial)
    )

    private var publicContactsSearchResult by mutableStateOf(
        ContactSearchResult.ExternalContact(searchResultState = SearchResultState.Initial)
    )

    val state: SearchPeopleState by derivedStateOf {
        val noneSearchSucceed: Boolean =
            localContactSearchResult.searchResultState is SearchResultState.Failure &&
                    publicContactsSearchResult.searchResultState is SearchResultState.Failure

        val filteredPublicContactsSearchResult: ContactSearchResult =
            publicContactsSearchResult.filterContacts(localContactSearchResult)

        innerSearchPeopleState.copy(
            noneSearchSucceed = noneSearchSucceed,
            localContactSearchResult = localContactSearchResult,
            publicContactsSearchResult = filteredPublicContactsSearchResult,
        )
    }

    private val searchQueryStateFlow = SearchQueryStateFlow()

    init {
        viewModelScope.launch {
            launch { allContacts() }

            searchQueryStateFlow.onSearchAction { searchTerm ->
                launch { searchPublic(searchTerm) }
                launch { searchKnown(searchTerm) }
            }
        }
    }

    private suspend fun allContacts() {
        innerSearchPeopleState = innerSearchPeopleState.copy(allKnownContacts = SearchResultState.InProgress)

        val result = withContext(dispatchers.io()) { getAllUsersUseCase() }

        innerSearchPeopleState = when (result) {
            is SearchResultState.Failure -> {
                innerSearchPeopleState.copy(
                    allKnownContacts = SearchResultState.Failure(R.string.label_general_error)
                )
            }
            is SearchResultState.Success -> {
                innerSearchPeopleState.copy(
                    allKnownContacts = SearchResultState.Success(result.result)
                )
            }
        }
    }

    private fun ContactSearchResult.filterContacts(contactSearchResult: ContactSearchResult): ContactSearchResult {
        return if (searchResultState is SearchResultState.Success &&
            contactSearchResult.searchResultState is SearchResultState.Success
        ) {
            ContactSearchResult.ExternalContact(SearchResultState.Success(searchResultState.result.filterNot { contact ->
                contactSearchResult.searchResultState.result.map { it.id }.contains(
                    contact.id
                )
            }))
        } else {
            this
        }
    }

    fun search(searchTerm: String) {
        // we set the state with a searchQuery, immediately to update the UI first
        innerSearchPeopleState = state.copy(searchQuery = searchTerm)

        searchQueryStateFlow.search(searchTerm)
    }

    private suspend fun searchKnown(searchTerm: String) {
        localContactSearchResult = ContactSearchResult.InternalContact(SearchResultState.InProgress)

        localContactSearchResult = when (val result = searchKnownUsersUseCase(searchTerm)) {
            is SearchResultState.Success -> ContactSearchResult.InternalContact(
                SearchResultState.Success(result.result)
            )
            else -> ContactSearchResult.InternalContact(SearchResultState.Failure(R.string.label_general_error))
        }
    }

    private suspend fun searchPublic(searchTerm: String, showProgress: Boolean = true) {
        if (showProgress) {
            publicContactsSearchResult = ContactSearchResult.ExternalContact(SearchResultState.InProgress)
        }
        val result = withContext(dispatchers.io()) {
            searchPublicUsersUseCase(
                searchTerm
            )
        }

        publicContactsSearchResult = when (result) {
            is SearchResultState.Failure -> {
                ContactSearchResult.ExternalContact(result)
            }
            is SearchResultState.Success -> {
                ContactSearchResult.ExternalContact(result)
            }
        }
    }

    fun addContactToGroup(contact: Contact) {
        innerSearchPeopleState = innerSearchPeopleState.copy(
            contactsAddedToGroup = innerSearchPeopleState.contactsAddedToGroup + contact
        )
    }

    fun addContact(contact: Contact) {
        viewModelScope.launch {
            val userId = UserId(contact.id, contact.domain)

            when (sendConnectionRequest(userId)) {
                is SendConnectionRequestResult.Failure -> {
                    appLogger.d(("Couldn't send a connect request to user $userId"))
                }
                is SendConnectionRequestResult.Success -> {
                    searchPublic(state.searchQuery, showProgress = false)
                }
            }
        }
    }

    fun removeContactFromGroup(contact: Contact) {
        innerSearchPeopleState = innerSearchPeopleState.copy(
            contactsAddedToGroup = innerSearchPeopleState.contactsAddedToGroup - contact
        )
    }

    fun openUserProfile(contact: Contact) {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.OtherUserProfile.getRouteWithArgs(
                        listOf(
                            contact.domain, contact.id, contact.connectionState
                        )
                    )
                )
            )
        }
    }

    fun close() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

    abstract suspend fun getAllUsersUseCase(): SearchResultState

    abstract suspend fun searchKnownUsersUseCase(searchTerm: String): SearchResultState

    abstract suspend fun searchPublicUsersUseCase(searchTerm: String): SearchResultState

}
