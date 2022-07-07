package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.mapper.ContactMapper
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.BackStackMode
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
import com.wire.kalium.logic.data.conversation.ConversationOptions
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.CreateGroupConversationUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsResult
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
    private val contactMapper: ContactMapper,
    private val createGroupConversation: CreateGroupConversationUseCase,
    private val contactMapper: ContactMapper,
    private val dispatchers: DispatcherProvider,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
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

open class AddConversationMembersViewModel(
    val navigationManager: NavigationManager,
    contactSearchUseCaseDelegation: ContactSearchUseCaseDelegation,
    val dispatchers: DispatcherProvider
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
            launch { allContacts() }

            searchQueryStateFlow.onSearchAction { searchTerm ->
                launch { searchPublic(searchTerm) }
                launch { searchKnown(searchTerm) }
            }
        }
    }

    private suspend fun allContacts() {
        innerSearchPeopleState = innerSearchPeopleState.copy(allKnownContacts = SearchResultState.InProgress)

        val result = withContext(dispatchers.io()) { getAllContacts() }

        innerSearchPeopleState = when (result) {
            is GetAllContactsResult.Failure -> {
                innerSearchPeopleState.copy(
                    allKnownContacts = SearchResultState.Failure(R.string.label_general_error)
                )
            }
            is GetAllContactsResult.Success -> {
                innerSearchPeopleState.copy(
                    allKnownContacts = SearchResultState.Success(result.allContacts.map(contactMapper::fromOtherUser))
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

        val result = searchKnownUsersUseCase(searchTerm)

//        localContactSearchResult = when (val result = searchKnownUsersUseCase(searchTerm)) {
//            is Result.Success -> ContactSearchResult.InternalContact(
//                SearchResultState.Success(result.userSearchResult.result.map { otherUser -> contactMapper.fromOtherUser(otherUser) })
//            )
//            else -> ContactSearchResult.InternalContact(SearchResultState.Failure(R.string.label_general_error))
//        }
    }

    private suspend fun searchPublic(searchTerm: String, showProgress: Boolean = true) {
        if (showProgress) {
            publicContactsSearchResult = ContactSearchResult.ExternalContact(SearchResultState.InProgress)
        }
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

    fun onGroupNameChange(newText: TextFieldValue) {
        when {
            newText.text.trim().isEmpty() -> {
                groupNameState = groupNameState.copy(
                    animatedGroupNameError = true,
                    groupName = newText,
                    continueEnabled = false,
                    error = NewGroupState.GroupNameError.TextFieldError.GroupNameEmptyError
                )
            }
            newText.text.trim().count() > GROUP_NAME_MAX_COUNT -> {
                groupNameState = groupNameState.copy(
                    animatedGroupNameError = true,
                    groupName = newText,
                    continueEnabled = false,
                    error = NewGroupState.GroupNameError.TextFieldError.GroupNameExceedLimitError
                )
            }
            else -> {
                groupNameState = groupNameState.copy(
                    animatedGroupNameError = false,
                    groupName = newText,
                    continueEnabled = true,
                    error = NewGroupState.GroupNameError.None
                )
            }
        }
    }

    fun createGroup() {
        viewModelScope.launch {
            groupNameState = groupNameState.copy(isLoading = true)

            when (val result = createGroupConversation(
                name = groupNameState.groupName.text,
                // TODO: change the id in Contact to UserId instead of String
                userIdList = state.contactsAddedToGroup.map { contact -> UserId(contact.id, contact.domain) },
                options = ConversationOptions().copy(protocol = groupNameState.groupProtocol)
            )
            ) {
                // TODO: handle the error state
                is Either.Left -> {
                    groupNameState = groupNameState.copy(isLoading = false)
                    Log.d("TEST", "error while creating a group ${result.value}")
                }
                is Either.Right -> {
                    groupNameState = groupNameState.copy(isLoading = false)
                    navigationManager.navigate(
                        command = NavigationCommand(
                            destination = NavigationItem.Conversation.getRouteWithArgs(listOf(result.value.id)),
                            backStackMode = BackStackMode.REMOVE_CURRENT
                        )
                    )
                }
            }
        }
    }

    fun onGroupNameErrorAnimated() {
        groupNameState = groupNameState.copy(animatedGroupNameError = false)
    }

    fun close() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

}
