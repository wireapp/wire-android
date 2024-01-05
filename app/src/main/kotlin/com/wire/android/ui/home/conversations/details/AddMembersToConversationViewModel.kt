/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.home.conversations.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.mapper.ContactMapper
import com.wire.android.ui.home.conversations.search.AddMembersSearchNavArgs
import com.wire.android.ui.home.conversations.search.ContactSearchResult
import com.wire.android.ui.home.conversations.search.KnownPeopleSearchViewModel
import com.wire.android.ui.home.conversations.search.SearchPeopleState
import com.wire.android.ui.home.conversations.search.SearchResult
import com.wire.android.ui.home.conversations.search.SearchResultState
import com.wire.android.ui.home.conversations.search.SearchResultTitle
import com.wire.android.ui.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.publicuser.ConversationMemberExcludedOptions
import com.wire.kalium.logic.data.publicuser.SearchUsersOptions
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetAllContactsNotInConversationUseCase
import com.wire.kalium.logic.feature.conversation.Result
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.service.SearchServicesByNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.wire.kalium.logic.feature.publicuser.search.SearchUsersResult as KnownUserSearchResult

@Suppress("LongParameterList")
@HiltViewModel
class AddMembersToConversationViewModel @Inject constructor(
    private val getAllContactsNotInConversation: GetAllContactsNotInConversationUseCase,
    private val getAllServices: ObserveAllServicesUseCase,
    private val contactMapper: ContactMapper,
    private val addMemberToConversation: AddMemberToConversationUseCase,
    private val searchServicesByName: SearchServicesByNameUseCase,
    private val dispatchers: DispatcherProvider,
    private val searchKnownUsers: SearchKnownUsersUseCase,
    savedStateHandle: SavedStateHandle
) : KnownPeopleSearchViewModel() {

    private val addMembersSearchNavArgs: AddMembersSearchNavArgs = savedStateHandle.navArgs()
    private val conversationId: QualifiedID = addMembersSearchNavArgs.conversationId
    private val isServicesAllowed: Boolean = addMembersSearchNavArgs.isServicesAllowed

    var state: SearchPeopleState by mutableStateOf(SearchPeopleState(isGroupCreationContext = false))

    init {
        viewModelScope.launch {
            combine(
                initialContactResultFlow(),
                knownPeopleSearchQueryFlow,
                searchQueryTextFieldFlow,
                selectedContactsFlow,
                combine(initialServicesResultFlow(), servicesSearchQueryFlow, ::Pair)
            ) { initialContacts,
                knownResult,
                searchQuery,
                selectedContacts,
                services ->
                val initialServices = services.first
                val servicesSearchResult = services.second
                SearchPeopleState(
                    initialContacts = initialContacts,
                    searchQuery = searchQuery,
                    searchResult = persistentMapOf(SearchResultTitle(R.string.label_contacts) to knownResult),
                    noneSearchSucceed = knownResult.searchResultState is SearchResultState.Failure,
                    contactsAddedToGroup = selectedContacts.toImmutableList(),
                    isGroupCreationContext = false,
                    servicesInitialContacts = initialServices,
                    servicesSearchResult = servicesSearchResult,
                    isServicesAllowed = isServicesAllowed
                )
            }.collect { updatedState ->
                state = updatedState
            }
        }
    }

    override suspend fun getInitialContacts(): Flow<SearchResult> =
        getAllContactsNotInConversation(conversationId).map { result ->
            when (result) {
                is Result.Failure -> SearchResult.Failure(R.string.label_general_error)
                is Result.Success -> SearchResult.Success(result.contactsNotInConversation.map(contactMapper::fromOtherUser))
            }
        }

    override suspend fun getInitialServices(): Flow<SearchResult> =
        getAllServices().map { result ->
            SearchResult.Success(result.map(contactMapper::fromService))
        }

    override suspend fun searchKnownPeople(searchTerm: String): Flow<ContactSearchResult.InternalContact> {
        return searchKnownUsers(
            searchQuery = searchTerm,
            searchUsersOptions = SearchUsersOptions(
                conversationExcluded = ConversationMemberExcludedOptions.ConversationExcluded(conversationId),
                selfUserIncluded = false
            )
        ).map { result ->
            when (result) {
                is KnownUserSearchResult.Failure.Generic -> ContactSearchResult.InternalContact(
                    SearchResultState.Failure(R.string.label_general_error)
                )
                KnownUserSearchResult.Failure.InvalidQuery -> ContactSearchResult.InternalContact(
                    SearchResultState.Failure(R.string.label_no_results_found)
                )
                KnownUserSearchResult.Failure.InvalidRequest -> ContactSearchResult.InternalContact(
                    SearchResultState.Failure(R.string.label_general_error)
                )
                is KnownUserSearchResult.Success -> ContactSearchResult.InternalContact(
                    SearchResultState.Success(result.userSearchResult.result.map(contactMapper::fromOtherUser).toImmutableList())
                )
            }
        }
    }

    override suspend fun searchServices(searchTerm: String): Flow<SearchResultState> {
        val searchServices = searchServicesByName(search = searchTerm)

        return flowOf(
            if (searchServices.first().isEmpty()) {
                SearchResultState.Failure(R.string.label_no_results_found)
            } else {
                SearchResultState.Success(
                    searchServices.first().map(contactMapper::fromService).toImmutableList()
                )
            }
        )
    }

    fun addMembersToConversation(onCompleted: () -> Unit) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                // TODO: addMembersToConversationUseCase does not handle failure
                addMemberToConversation(
                    conversationId = conversationId,
                    userIdList = state.contactsAddedToGroup.map { UserId(it.id, it.domain) }
                )
            }
            onCompleted()
        }
    }
}
