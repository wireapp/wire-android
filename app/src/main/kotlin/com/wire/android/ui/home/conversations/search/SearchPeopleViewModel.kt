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
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.flow.SearchQueryStateFlow
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsResult
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.search.Result
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchUsersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Suppress("TooManyFunctions")
open class SearchAllUsersViewModel(
    private val getAllKnownUsers: GetAllContactsUseCase,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    private val searchKnownUsers: SearchKnownUsersUseCase,
    private val searchPublicUsers: SearchUsersUseCase,
    private val contactMapper: ContactMapper,
    private val dispatcher: DispatcherProvider,
    navigationManager: NavigationManager,
) : SearchPeopleViewModel(dispatcher, navigationManager) {

    private val localContactSearchResult =
        MutableStateFlow(ContactSearchResult.InternalContact(searchResultState = SearchResultState.Initial))

    private val publicContactsSearchResult =
        MutableStateFlow(ContactSearchResult.ExternalContact(searchResultState = SearchResultState.Initial))

    var state: SearchPeopleState by mutableStateOf(SearchPeopleState())

    init {
        viewModelScope.launch {
            launch {
                searchQueryStateFlow.onSearchAction { searchTerm ->
                    launch { searchKnown(searchTerm) }
                    launch { searchPublic(searchTerm) }
                }
            }

            launch {
                combine(
                    searchPeopleResult,
                    localContactSearchResult.onStart { ContactSearchResult.InternalContact(SearchResultState.InProgress) },
                    publicContactsSearchResult
                ) { searchPeopleResult, localContactSearchResult, publicContactSearchResult ->
                    SearchPeopleState(
                        self = null,
                        initialContacts = searchPeopleResult.initialContactResult,
                        searchQuery = searchPeopleResult.searchQuery,
                        searchResult = mapOf(
                            "test" to localContactSearchResult,
                            "test1" to publicContactSearchResult.filterContacts(localContactSearchResult)
                        ),
                        noneSearchSucceed = false,
                        contactsAddedToGroup = searchPeopleResult.selectedContacts
                    )
                }.collect { searchPeopleState ->
                    state = searchPeopleState
                }
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

    override suspend fun getInitialContacts(): SearchResult = withContext(dispatcher.io()) {
        when (val result = getAllKnownUsers()) {
            is GetAllContactsResult.Failure -> SearchResult.Failure(R.string.label_general_error)
            is GetAllContactsResult.Success -> SearchResult.Success(result.allContacts.map(contactMapper::fromOtherUser))
        }
    }

    private suspend fun searchKnown(searchTerm: String) {
        viewModelScope.launch {
            localContactSearchResult.emit(
                when (val result: Result = withContext(dispatcher.io()) { searchKnownUsers(searchTerm) }) {
                    is Result.Failure.Generic, Result.Failure.InvalidQuery ->
                        ContactSearchResult.InternalContact(SearchResultState.Failure(R.string.label_general_error))
                    Result.Failure.InvalidRequest ->
                        ContactSearchResult.InternalContact(SearchResultState.Failure(R.string.label_general_error))
                    is Result.Success -> ContactSearchResult.InternalContact(
                        SearchResultState.Success(result.userSearchResult.result.map(contactMapper::fromOtherUser))
                    )
                }
            )
        }
    }

    private suspend fun searchPublic(searchTerm: String, showProgress: Boolean = true) {
        if (showProgress) {
            publicContactsSearchResult.emit(ContactSearchResult.ExternalContact(SearchResultState.InProgress))
        }

        val result = withContext(dispatcher.io()) { searchPublicUsers(searchTerm) }
        publicContactsSearchResult.emit(
            when (result) {
                is Result.Failure.Generic -> ContactSearchResult.ExternalContact(SearchResultState.Failure(R.string.label_general_error))
                Result.Failure.InvalidQuery -> ContactSearchResult.ExternalContact(SearchResultState.Failure(R.string.label_general_error))
                Result.Failure.InvalidRequest -> ContactSearchResult.ExternalContact(SearchResultState.Failure(R.string.label_general_error))
                is Result.Success -> ContactSearchResult.ExternalContact(
                    SearchResultState.Success(
                        result.userSearchResult.result.map(
                            contactMapper::fromOtherUser
                        )
                    )
                )
            }
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
                    searchPublic(state.searchQuery.text, showProgress = false)
                    snackbarMessageState = NewConversationSnackbarState.SuccessSendConnectionRequest
                }
            }
        }
    }

}

data class SearchPeopleResult(
    val initialContactResult: SearchResultState = SearchResultState.Initial,
    val searchQuery: TextFieldValue = TextFieldValue(""),
    val selectedContacts: List<Contact> = emptyList()
)

abstract class SearchPeopleViewModel(
    private val dispatcher: DispatcherProvider,
    val navigationManager: NavigationManager
) : ViewModel() {

    private val initialContactResultFlow: MutableStateFlow<SearchResultState> = MutableStateFlow(SearchResultState.Initial)

    private val searchQueryFlow = MutableStateFlow(TextFieldValue(""))

    private val selectedContactsFlow = MutableStateFlow(emptyList<Contact>())

    protected val searchQueryStateFlow = SearchQueryStateFlow(dispatcher.io())

    protected val searchPeopleResult =
        combine(initialContactResultFlow, searchQueryFlow, selectedContactsFlow) { initialContactResult, searchQuery, selectedContacts ->
            SearchPeopleResult(
                initialContactResult = initialContactResult,
                searchQuery = searchQuery,
                selectedContacts = selectedContacts
            )
        }

    var snackbarMessageState by mutableStateOf<NewConversationSnackbarState>(NewConversationSnackbarState.None)

    init {
        viewModelScope.launch {
            launch {
                when (val result = withContext(dispatcher.io()) { getInitialContacts() }) {
                    is SearchResult.Failure -> {
                        initialContactResultFlow.emit(
                            SearchResultState.Failure(result.failureString)
                        )
                    }
                    is SearchResult.Success -> {
                        initialContactResultFlow.emit(
                            SearchResultState.Success(result.contacts)
                        )
                    }
                }
            }
        }
    }

    fun searchQueryChanged(searchQuery: TextFieldValue) {
        val textQueryChanged = searchQueryFlow.value.text != searchQuery.text
        // we set the state with a searchQuery, immediately to update the UI first
        viewModelScope.launch {
            searchQueryFlow.emit(searchQuery)
        }
        if (textQueryChanged) searchQueryStateFlow.search(searchQuery.text)
    }

    fun addContactToGroup(contact: Contact) {
        viewModelScope.launch {
            selectedContactsFlow.emit(selectedContactsFlow.value + contact)
        }
    }

    fun removeContactFromGroup(contact: Contact) {
        viewModelScope.launch {
            selectedContactsFlow.emit(selectedContactsFlow.value - contact)
        }
    }

    fun openUserProfile(contact: Contact) {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.OtherUserProfile.getRouteWithArgs(
                        listOf(
                            QualifiedID(contact.id, contact.domain), contact.connectionState
                        )
                    )
                )
            )
        }
    }

    fun clearSnackbarMessage() {
        snackbarMessageState = NewConversationSnackbarState.None
    }

    fun close() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

    abstract suspend fun getInitialContacts(): SearchResult
}

// Different use cases could return different type for the search, we are making sure here
// that the type is mapped to a SearchResult that can further used in this class
sealed class SearchResult {
    data class Success(val contacts: List<Contact>) : SearchResult()
    data class Failure(@StringRes val failureString: Int) : SearchResult()
}

sealed class NewConversationSnackbarState {
    object SuccessSendConnectionRequest : NewConversationSnackbarState()
    object None : NewConversationSnackbarState()
}
