package com.wire.android.ui.home.newconversation

import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.home.newconversation.model.toContact
import com.wire.android.ui.home.newconversation.newgroup.NewGroupState
import com.wire.android.ui.home.newconversation.search.ContactSearchResult
import com.wire.android.ui.home.newconversation.search.SearchPeopleState
import com.wire.android.ui.home.newconversation.search.SearchResultState
import com.wire.android.util.flow.SearchQueryStateFlow
import com.wire.kalium.logic.data.conversation.ConversationOptions
import com.wire.kalium.logic.data.id.VALUE_DOMAIN_SEPARATOR
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.feature.conversation.CreateGroupConversationUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.Result
import com.wire.kalium.logic.feature.publicuser.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.SearchUserDirectoryUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.functional.Either
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class NewConversationViewModel
@Inject constructor(
    private val navigationManager: NavigationManager,
    private val searchKnownUsers: SearchKnownUsersUseCase,
    private val searchPublicUsers: SearchUserDirectoryUseCase,
    private val getAllContacts: GetAllContactsUseCase,
    private val createGroupConversation: CreateGroupConversationUseCase,
    private val getSelf: GetSelfUserUseCase,
) : ViewModel() {

    private companion object {
        val FEDERATION_REGEX = Regex("[^@.]+@[^@.]+\\.[^@]+")
        const val GROUP_NAME_MAX_COUNT = 64
    }

    val state: SearchPeopleState by derivedStateOf {
        val noneSearchSucceed: Boolean =
            localContactSearchResult.searchResultState is SearchResultState.Failure &&
                    publicContactsSearchResult.searchResultState is SearchResultState.Failure &&
                    federatedContactSearchResult.searchResultState is SearchResultState.Failure

        innerSearchPeopleState.copy(
            noneSearchSucceed = noneSearchSucceed,
            localContactSearchResult = localContactSearchResult,
            publicContactsSearchResult = publicContactsSearchResult,
            federatedContactSearchResult = federatedContactSearchResult
        )
    }

    var groupNameState: NewGroupState by mutableStateOf(NewGroupState())

    private var innerSearchPeopleState: SearchPeopleState by mutableStateOf(SearchPeopleState())

    var self by mutableStateOf<SelfUser?>(null)

    private var localContactSearchResult by mutableStateOf(
        ContactSearchResult.InternalContact(searchResultState = SearchResultState.Initial)
    )

    private var publicContactsSearchResult by mutableStateOf(
        ContactSearchResult.ExternalContact(searchResultState = SearchResultState.Initial)
    )

    private var federatedContactSearchResult by mutableStateOf(
        ContactSearchResult.ExternalContact(searchResultState = SearchResultState.Initial)
    )

    private val searchQueryStateFlow = SearchQueryStateFlow()

    fun updateScrollPosition(newScrollPosition: Int) {
        innerSearchPeopleState = state.copy(scrollPosition = newScrollPosition)
    }

    init {
        viewModelScope.launch {
            launch { loadUser() }

            launch {
                val allContacts = getAllContacts()

                innerSearchPeopleState = innerSearchPeopleState.copy(
                    allKnownContacts = allContacts.map { otherUser -> otherUser.toContact() }
                )
            }

            searchQueryStateFlow.onSearchAction { searchTerm ->
                launch { searchPublic(searchTerm) }
                launch { searchKnown(searchTerm) }
            }
        }
    }

    private suspend fun loadUser() {
        viewModelScope.launch {
            getSelf().collect { selfUser ->
                self = selfUser
            }
        }
    }

    fun search(searchTerm: String) {
        // we set the state with a searchQuery, immediately to update the UI first
        innerSearchPeopleState = state.copy(searchQuery = searchTerm)

        searchQueryStateFlow.search(searchTerm)
    }

    //TODO: suppress for now,
    // we should  map the result to a custom Result class containing Error on Kalium side for this use case
    @Suppress("TooGenericExceptionCaught")
    private suspend fun searchKnown(searchTerm: String) {
        localContactSearchResult = ContactSearchResult.InternalContact(SearchResultState.InProgress)

        localContactSearchResult = try {
            val searchResult = searchKnownUsers(searchTerm)

            ContactSearchResult.InternalContact(
                SearchResultState.Success(searchResult.result.map { otherUser -> otherUser.toContact() })
            )
        } catch (exception: Exception) {
            ContactSearchResult.InternalContact(SearchResultState.Failure(R.string.label_general_error))
        }
    }

    private suspend fun searchPublic(searchTerm: String) {
        publicContactsSearchResult = ContactSearchResult.ExternalContact(SearchResultState.InProgress)

        val result: Result
        val isFederationSearch = searchTerm.matches(FEDERATION_REGEX)
        val query: String
        val domain: String

        if (isFederationSearch) {
            query = searchTerm.split(VALUE_DOMAIN_SEPARATOR)[0]
            domain = searchTerm.split(VALUE_DOMAIN_SEPARATOR)[1]
        } else {
            query = searchTerm
            domain = self?.id?.domain ?: ""
        }

        result = withContext(Dispatchers.IO) {
            searchPublicUsers(
                searchQuery = query,
                domain = domain
            )
        }

        publicContactsSearchResult = when (result) {
            is Result.Failure.Generic, Result.Failure.InvalidRequest -> {
                ContactSearchResult.ExternalContact(SearchResultState.Failure(R.string.label_general_error))
            }
            is Result.Failure.InvalidQuery -> {
                ContactSearchResult.ExternalContact(SearchResultState.Failure(R.string.label_no_results_found))
            }
            is Result.Success -> {
                ContactSearchResult.ExternalContact(
                    SearchResultState.Success(result.userSearchResult.result.map { it.toContact() })
                )
            }
        }
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

            when (
                val result = createGroupConversation(
                    name = groupNameState.groupName.text,
                    members = state.contactsAddedToGroup.map { contact -> contact.toMember() },
                    options = ConversationOptions()
                )
            ) {
                // TODO: handle the error state
                is Either.Left -> {
                    groupNameState = groupNameState.copy(isLoading = false)
                    Log.d("TEST", "error while creating a group ${result.value}")
                }
                is Either.Right -> {
                    close()
                    groupNameState = groupNameState.copy(isLoading = false)
                    navigationManager.navigate(
                        command = NavigationCommand(
                            destination = NavigationItem.Conversation.getRouteWithArgs(listOf(result.value.id))
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
