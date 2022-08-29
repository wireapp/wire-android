package com.wire.android.ui.home.conversations.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.mapper.ContactMapper
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.search.ContactSearchResult
import com.wire.android.ui.home.conversations.search.SearchPeopleState
import com.wire.android.ui.home.conversations.search.SearchPeopleViewModel
import com.wire.android.ui.home.conversations.search.SearchResult
import com.wire.android.ui.home.conversations.search.SearchResultState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.publicuser.ConversationMemberExcludedOptions
import com.wire.kalium.logic.data.publicuser.SearchUsersOptions
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetAllContactsNotInConversationUseCase
import com.wire.kalium.logic.feature.conversation.Result
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.wire.kalium.logic.feature.publicuser.search.Result as KnownUserSearchResult

@Suppress("LongParameterList")
@HiltViewModel
class AddMembersToConversationViewModel @Inject constructor(
    private val getAllContactsNotInConversation: GetAllContactsNotInConversationUseCase,
    private val contactMapper: ContactMapper,
    private val addMemberToConversation: AddMemberToConversationUseCase,
    private val dispatchers: DispatcherProvider,
    private val searchKnownUsers: SearchKnownUsersUseCase,
    savedStateHandle: SavedStateHandle,
    navigationManager: NavigationManager,
    qualifiedIdMapper: QualifiedIdMapper
) : SearchPeopleViewModel(
    dispatcher = dispatchers,
    navigationManager = navigationManager
) {

    private var localContactSearchResult =
        MutableStateFlow(ContactSearchResult.InternalContact(searchResultState = SearchResultState.Initial))

    var state: SearchPeopleState by mutableStateOf(SearchPeopleState())

    private val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    init {
        viewModelScope.launch {
            launch {
                combine(
                    searchPeopleResult,
                    localContactSearchResult.onStart { ContactSearchResult.InternalContact(SearchResultState.InProgress) }) { a, b ->
                    SearchPeopleState(
                        self = null,
                        initialContacts = a.initialContactResult,
                        searchQuery = a.searchQuery,
                        searchResult = mapOf("test" to b),
                        noneSearchSucceed = false,
                        contactsAddedToGroup = a.selectedContacts
                    )
                }.collect { searchPeopleState ->
                    state = searchPeopleState
                }
            }

            launch {
                searchQueryStateFlow.onSearchAction { searchTerm ->
                    launch {
                        val result = withContext(dispatchers.io()) {
                            searchKnownUsers(
                                searchQuery = searchTerm,
                                searchUsersOptions = SearchUsersOptions(
                                    conversationExcluded = ConversationMemberExcludedOptions.ConversationExcluded(conversationId),
                                    selfUserIncluded = false
                                )
                            )
                        }

                        localContactSearchResult.emit(
                            when (result) {
                                is KnownUserSearchResult.Failure.Generic -> ContactSearchResult.InternalContact(
                                    SearchResultState.Failure(R.string.label_general_error)
                                )
                                KnownUserSearchResult.Failure.InvalidQuery -> ContactSearchResult.InternalContact(
                                    SearchResultState.Failure(R.string.label_general_error)
                                )
                                KnownUserSearchResult.Failure.InvalidRequest -> ContactSearchResult.InternalContact(
                                    SearchResultState.Failure(R.string.label_general_error)
                                )
                                is KnownUserSearchResult.Success -> ContactSearchResult.InternalContact(
                                    SearchResultState.Success(
                                        result.userSearchResult.result.map(
                                            contactMapper::fromOtherUser
                                        )
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    override suspend fun getInitialContacts(): SearchResult =
        withContext(dispatchers.io()) {
            when (val result = getAllContactsNotInConversation(conversationId)) {
                is Result.Failure -> SearchResult.Failure(R.string.label_general_error)
                is Result.Success -> SearchResult.Success(result.contactsNotInConversation.map(contactMapper::fromOtherUser))
            }
        }

    fun addMembersToConversation() {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                //TODO: addMembersToConversationUseCase does not handle failure
                addMemberToConversation(
                    conversationId = conversationId,
                    userIdList = state.contactsAddedToGroup.map { UserId(it.id, it.domain) }
                )
            }
            navigationManager.navigateBack()
        }
    }

}
