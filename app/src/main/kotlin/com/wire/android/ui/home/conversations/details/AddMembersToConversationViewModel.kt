package com.wire.android.ui.home.conversations.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.mapper.ContactMapper
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.search.SearchPeopleViewModel
import com.wire.android.ui.home.conversations.search.SearchResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.data.publicuser.ConversationMemberExcludedOptions
import com.wire.kalium.logic.data.publicuser.SearchUsersOptions
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetAllContactsNotInConversationUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.wire.kalium.logic.feature.conversation.Result as GetContactsResult
import com.wire.kalium.logic.feature.publicuser.search.Result
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AddMembersToConversationViewModel @Inject constructor(
    private val getAllContactsNotInConversation: GetAllContactsNotInConversationUseCase,
    private val searchKnownUsers: SearchKnownUsersUseCase,
    private val searchPublicUsers: SearchUsersUseCase,
    private val contactMapper: ContactMapper,
    private val addMemberToConversation: AddMemberToConversationUseCase,
    private val dispatchers: DispatcherProvider,
    sendConnectionRequest: SendConnectionRequestUseCase,
    savedStateHandle: SavedStateHandle,
    navigationManager: NavigationManager
) : SearchPeopleViewModel(navigationManager, sendConnectionRequest, dispatchers) {

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    init{
        viewModelScope.launch { allContacts() }
    }

    override suspend fun getAllUsersUseCase() =
        when (val result = getAllContactsNotInConversation(conversationId)) {
            is GetContactsResult.Failure -> SearchResult.Failure(R.string.label_general_error)
            is GetContactsResult.Success -> SearchResult.Success(
                result.contactsNotInConversation.map { otherUser ->
                    contactMapper.fromOtherUser(
                        otherUser
                    )
                }
            )
        }

    override suspend fun searchKnownUsersUseCase(searchTerm: String) =
        when (val result = searchKnownUsers(
            searchQuery = searchTerm,
            searchUsersOptions = SearchUsersOptions(
                conversationExcluded = ConversationMemberExcludedOptions.ConversationExcluded(conversationId)
            )
        )) {
            is Result.Failure.Generic, Result.Failure.InvalidRequest -> {
                SearchResult.Failure(R.string.label_general_error)
            }
            is Result.Failure.InvalidQuery -> {
                SearchResult.Failure(R.string.label_no_results_found)
            }
            is Result.Success -> {
                SearchResult.Success(result.userSearchResult.result.map { otherUser -> contactMapper.fromOtherUser(otherUser) })
            }
        }

    override suspend fun searchPublicUsersUseCase(searchTerm: String) =
        when (val result = searchPublicUsers(
            searchTerm,
        )) {
            is Result.Failure.Generic, Result.Failure.InvalidRequest -> {
                SearchResult.Failure(R.string.label_general_error)
            }
            is Result.Failure.InvalidQuery -> {
                SearchResult.Failure(R.string.label_no_results_found)
            }
            is Result.Success -> {
                SearchResult.Success(result.userSearchResult.result.map { otherUser -> contactMapper.fromOtherUser(otherUser) })
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
