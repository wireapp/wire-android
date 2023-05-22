/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.conversations.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.mapper.ContactMapper
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_IS_SERVICES_ALLOWED
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.search.ContactSearchResult
import com.wire.android.ui.home.conversations.search.KnownPeopleSearchViewModel
import com.wire.android.ui.home.conversations.search.SearchPeopleState
import com.wire.android.ui.home.conversations.search.SearchResult
import com.wire.android.ui.home.conversations.search.SearchResultState
import com.wire.android.ui.home.conversations.search.SearchResultTitle
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.publicuser.ConversationMemberExcludedOptions
import com.wire.kalium.logic.data.publicuser.SearchUsersOptions
import com.wire.kalium.logic.data.user.BotService
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
    savedStateHandle: SavedStateHandle,
    navigationManager: NavigationManager,
    qualifiedIdMapper: QualifiedIdMapper
) : KnownPeopleSearchViewModel(
    navigationManager = navigationManager
) {

    private val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )
    private val isServicesAllowed: Boolean = savedStateHandle
        .get<String>(EXTRA_IS_SERVICES_ALLOWED)!!.toBoolean()

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

    override fun getInitialContacts(): Flow<SearchResult> =
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

    fun addMembersToConversation() {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                // TODO: addMembersToConversationUseCase does not handle failure
                addMemberToConversation(
                    conversationId = conversationId,
                    userIdList = state.contactsAddedToGroup.map { UserId(it.id, it.domain) }
                )
            }
            navigationManager.navigateBack()
        }
    }

    fun onServiceClicked(contact: Contact) {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.ServiceDetails.getRouteWithArgs(
                        listOf(
                            BotService(
                                id = contact.id,
                                provider = contact.domain
                            ),
                            conversationId
                        )
                    )
                )
            )
        }
    }
}
