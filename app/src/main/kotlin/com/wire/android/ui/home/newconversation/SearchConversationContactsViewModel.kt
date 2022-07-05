package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.mapper.ContactMapper
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.home.newconversation.search.ContactSearchResult
import com.wire.android.ui.home.newconversation.search.SearchPeopleState
import com.wire.android.ui.home.newconversation.search.SearchResultState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.flow.SearchQueryStateFlow
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.SearchUsersUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


class AllContactSearchUseCaseDelegation @Inject constructor(
    private val searchUsers: SearchUsersUseCase,
    private val searchKnownUsers: SearchKnownUsersUseCase,
    private val getAllContacts: GetAllContactsUseCase,
    private val contactMapper: ContactMapper
) : ContactSearchUseCaseDelegation {

    override suspend fun getAllUsersUseCase(): SearchResultState {
        return SearchResultState.InProgress
    }

    override suspend fun searchKnownUsersUseCase(searchTerm: String): SearchResultState {
        return SearchResultState.InProgress
    }

    override suspend fun searchPublicUsersUseCase(searchTerm: String): SearchResultState {
        return SearchResultState.InProgress
    }
//        when (val result = searchUsers(searchTerm)) {
//            is Result.Failure.Generic, Result.Failure.InvalidRequest -> {
//                SearchResultState.Failure(R.string.label_general_error)
//            }
//            is Result.Failure.InvalidQuery -> {
//                SearchResultState.Failure(R.string.label_no_results_found)
//            }
//            is Result.Success -> {
//                SearchResultState.Success(result.userSearchResult.result.map { otherUser -> contactMapper.fromOtherUser(otherUser) })
//            }
//        }

}

class ContactNotInConversationSearchUseCaseDelegation @Inject constructor(
    private val searchUsers: SearchUsersUseCase,
    private val searchKnownUsers: SearchKnownUsersUseCase,
    private val getAllContacts: GetAllContactsUseCase,
    savedStateHandle: SavedStateHandle,
) : ContactSearchUseCaseDelegation {

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    override suspend fun getAllUsersUseCase(): SearchResultState {
        return SearchResultState.InProgress
    }

    override suspend fun searchKnownUsersUseCase(searchTerm: String): SearchResultState {
        return SearchResultState.InProgress
    }

    override suspend fun searchPublicUsersUseCase(searchTerm: String): SearchResultState {
        return SearchResultState.InProgress
    }

}

interface ContactSearchUseCaseDelegation {
    suspend fun getAllUsersUseCase(): SearchResultState
    suspend fun searchKnownUsersUseCase(searchTerm: String): SearchResultState
    suspend fun searchPublicUsersUseCase(searchTerm: String): SearchResultState
}

abstract class SearchConversationContactsViewModel(
    val navigationManager: NavigationManager,
    contactSearchUseCaseDelegation: ContactSearchUseCaseDelegation,
    private val dispatchers: DispatcherProvider
) : ViewModel(), ContactSearchUseCaseDelegation by contactSearchUseCaseDelegation {

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
            launch {
                tryGetAllContacts()
            }

            searchQueryStateFlow.onSearchAction { searchTerm ->
                launch { searchPublic(searchTerm) }
                launch { searchKnown(searchTerm) }
            }
        }
    }

    private suspend fun tryGetAllContacts() {
        innerSearchPeopleState = innerSearchPeopleState.copy(allKnownContacts = SearchResultState.InProgress)

        withContext(dispatchers.io()) {
            when (val result = getAllUsersUseCase()) {
                is SearchResultState.Failure -> {
                    innerSearchPeopleState = innerSearchPeopleState.copy(
                        allKnownContacts = SearchResultState.Failure(R.string.label_general_error)
                    )
                }
                is SearchResultState.Success -> {
                    innerSearchPeopleState = innerSearchPeopleState.copy(
                        allKnownContacts = SearchResultState.Success(result.result)
                    )
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

    fun search(searchTerm: String) {
        // we set the state with a searchQuery, immediately to update the UI first
        innerSearchPeopleState = state.copy(searchQuery = searchTerm)

        searchQueryStateFlow.search(searchTerm)
    }

    private suspend fun searchKnown(searchTerm: String) {
        localContactSearchResult = ContactSearchResult.InternalContact(SearchResultState.InProgress)

        val result = searchKnownUsersUseCase(searchTerm)

//        localContactSearchResult = when (val result = searchKnownUsersUseCase(searchTerm)) {
//            is Result.Success -> ContactSearchResult.InternalContact(
//                SearchResultState.Success(result.userSearchResult.result.map { otherUser -> contactMapper.fromOtherUser(otherUser) })
//            )
//            else -> ContactSearchResult.InternalContact(SearchResultState.Failure(R.string.label_general_error))
//        }
    }

    private suspend fun searchPublic(searchTerm: String) {
        publicContactsSearchResult = ContactSearchResult.ExternalContact(SearchResultState.InProgress)

//        publicContactsSearchResult = when (val result = searchPublicUsersUseCase(searchTerm)) {
//            is SearchResultState.Failure -> TODO()
//            SearchResultState.InProgress -> TODO()
//            SearchResultState.Initial -> TODO()
//            is SearchResultState.Success -> TODO()
//        }
    }

    fun addContactToGroup(contact: Contact) {
        innerSearchPeopleState = innerSearchPeopleState.copy(
            contactsAddedToGroup = innerSearchPeopleState.contactsAddedToGroup + contact
        )
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

}
