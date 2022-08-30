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
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsResult
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.search.Result
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchUsersUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Suppress("TooManyFunctions")
open class SearchAllPeopleViewModel(
    private val getAllKnownUsers: GetAllContactsUseCase,
    private val searchKnownUsers: SearchKnownUsersUseCase,
    private val searchPublicUsers: SearchUsersUseCase,
    private val contactMapper: ContactMapper,
    private val dispatcher: DispatcherProvider,
    sendConnectionRequest: SendConnectionRequestUseCase,
    navigationManager: NavigationManager,
) : PublicWithKnownPeopleSearchViewModel(sendConnectionRequest, dispatcher, navigationManager) {

    var state: SearchPeopleState by mutableStateOf(SearchPeopleState())

    init {
        viewModelScope.launch {
            combine(
                initialContactResultFlow,
                publicPeopleSearchQueryFlow,
                knownPeopleSearchQueryFlow,
                searchQueryTextFieldFlow,
                selectedContactsFlow
            ) { initialContacts, publicResult, knownResult, searchQuery, selectedContacts ->
                SearchPeopleState(
                    initialContacts = initialContacts,
                    searchQuery = searchQuery,
                    searchResult = mapOf(
                        "test" to publicResult.filterContacts(knownResult),
                        "test1" to knownResult
                    ),
                    noneSearchSucceed = publicResult.searchResultState is SearchResultState.Failure
                            && knownResult.searchResultState is SearchResultState.Failure,
                    contactsAddedToGroup = selectedContacts
                )
            }.collect { updatedState ->
                state = updatedState
            }
        }
    }

    override suspend fun getInitialContacts(): SearchResult =
        when (val result = getAllKnownUsers()) {
            is GetAllContactsResult.Failure -> SearchResult.Failure(R.string.label_general_error)
            is GetAllContactsResult.Success -> SearchResult.Success(result.allContacts.map(contactMapper::fromOtherUser))
        }

    override suspend fun searchKnownPeople(searchTerm: String): ContactSearchResult.InternalContact =
        when (val result: Result = withContext(dispatcher.io()) { searchKnownUsers(searchTerm) }) {
            is Result.Failure.Generic, Result.Failure.InvalidQuery ->
                ContactSearchResult.InternalContact(SearchResultState.Failure(R.string.label_general_error))
            Result.Failure.InvalidRequest ->
                ContactSearchResult.InternalContact(SearchResultState.Failure(R.string.label_general_error))
            is Result.Success -> ContactSearchResult.InternalContact(
                SearchResultState.Success(result.userSearchResult.result.map(contactMapper::fromOtherUser))
            )
        }

    override suspend fun searchPublicPeople(searchTerm: String): ContactSearchResult.ExternalContact =
        when (val result = withContext(dispatcher.io()) { searchPublicUsers(searchTerm) }) {
            is Result.Failure.Generic -> ContactSearchResult.ExternalContact(SearchResultState.Failure(R.string.label_general_error))
            Result.Failure.InvalidQuery -> ContactSearchResult.ExternalContact(SearchResultState.Failure(R.string.label_general_error))
            Result.Failure.InvalidRequest -> ContactSearchResult.ExternalContact(SearchResultState.Failure(R.string.label_general_error))
            is Result.Success -> ContactSearchResult.ExternalContact(
                SearchResultState.Success(result.userSearchResult.result.map(contactMapper::fromOtherUser))
            )
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

}

data class PublicRefresh(val withProgress: Boolean = true)

abstract class PublicWithKnownPeopleSearchViewModel(
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    dispatcher: DispatcherProvider,
    navigationManager: NavigationManager
) : KnownPeopleSearchViewModel(dispatcher, navigationManager) {

    protected val refreshPublicResult: MutableSharedFlow<PublicRefresh> = MutableSharedFlow(0)

    protected val publicPeopleSearchQueryFlow = _searchQueryFlow
        .combine(refreshPublicResult.onSubscription { emit(PublicRefresh()) })
        { searchTerm, publicRefresh ->
            searchTerm to publicRefresh
        }.flatMapLatest { (searchTerm, publicRefresh) ->
            flow {
                if (publicRefresh.withProgress) {
                    emit(ContactSearchResult.ExternalContact(SearchResultState.InProgress))
                }

                emit(searchPublicPeople(searchTerm))
            }.cancellable()
        }


    fun addContact(contact: Contact) {
        viewModelScope.launch {
            val userId = UserId(contact.id, contact.domain)

            when (sendConnectionRequest(userId)) {
                is SendConnectionRequestResult.Failure -> {
                    appLogger.d(("Couldn't send a connect request to user $userId"))
                }
                is SendConnectionRequestResult.Success -> {
                    refreshPublicResult.emit(PublicRefresh(withProgress = false))
                    snackbarMessageState = NewConversationSnackbarState.SuccessSendConnectionRequest
                }
            }
        }
    }

    abstract suspend fun searchPublicPeople(searchTerm: String): ContactSearchResult.ExternalContact
}

abstract class KnownPeopleSearchViewModel(
    dispatcher: DispatcherProvider,
    navigationManager: NavigationManager
) : SearchPeopleViewModel(dispatcher, navigationManager) {

    protected val knownPeopleSearchQueryFlow = _searchQueryFlow
        .flatMapLatest { searchTerm ->
            flow {
                emit(ContactSearchResult.InternalContact(SearchResultState.InProgress))
                emit(searchKnownPeople(searchTerm))
            }.cancellable()
        }

    abstract suspend fun searchKnownPeople(searchTerm: String): ContactSearchResult.InternalContact
}

abstract class SearchPeopleViewModel(
    dispatcher: DispatcherProvider,
    val navigationManager: NavigationManager
) : ViewModel() {

    protected val _searchQueryFlow = MutableStateFlow("")

    protected val searchQueryFlow = _searchQueryFlow
        .asStateFlow()
        .debounce(500)

    protected val initialContactResultFlow = flow {
        when (val result = getInitialContacts()) {
            is SearchResult.Failure -> {
                emit(
                    SearchResultState.Failure(result.failureString)
                )
            }
            is SearchResult.Success -> {
                emit(
                    SearchResultState.Success(result.contacts)
                )
            }
        }
    }.flowOn(dispatcher.io())

    protected val searchQueryTextFieldFlow = MutableStateFlow(TextFieldValue(""))

    protected val selectedContactsFlow = MutableStateFlow(emptyList<Contact>())

    var snackbarMessageState by mutableStateOf<NewConversationSnackbarState>(NewConversationSnackbarState.None)

    fun searchQueryChanged(searchQuery: TextFieldValue) {
        val textQueryChanged = searchQueryTextFieldFlow.value.text != searchQuery.text
        // we set the state with a searchQuery, immediately to update the UI first
        viewModelScope.launch {
            searchQueryTextFieldFlow.emit(searchQuery)

            if (textQueryChanged) _searchQueryFlow.emit(searchQuery.text)
        }
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
