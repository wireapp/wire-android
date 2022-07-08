package com.wire.android.ui.home.newconversation

import androidx.lifecycle.SavedStateHandle
import com.wire.android.R
import com.wire.android.mapper.ContactMapper
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.newconversation.search.SearchResultState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetAllContactsNotInConversationUseCase
import com.wire.kalium.logic.feature.publicuser.Result
import com.wire.kalium.logic.feature.publicuser.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.SearchUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.wire.kalium.logic.feature.conversation.Result as GetContactsResult

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
) : SearchConversationViewModel(navigationManager, sendConnectionRequest,dispatchers) {

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    override suspend fun getAllUsersUseCase() =
        withContext(dispatchers.io()) {
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
        }

    override suspend fun searchKnownUsersUseCase(searchTerm: String) =
        withContext(dispatchers.io()) {
            when (val result = searchKnownUsers(searchTerm)) {
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
        }

    override suspend fun searchPublicUsersUseCase(searchTerm: String) =
        withContext(dispatchers.io()) {
            when (val result = searchPublicUsers(searchTerm)) {
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
        }

    suspend fun addSelectedMembers() {
        withContext(dispatchers.io()) {
            addMemberToConversation(conversationId, state.contactsAddedToGroup.map { UserId(it.id, it.domain) })
        }
    }

}
