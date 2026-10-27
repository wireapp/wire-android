/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.search.users

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.metro.WireAssistedViewModelBinding
import com.wire.android.mapper.ContactMapper
import com.wire.android.model.Contact
import com.wire.android.search.SearchManualViewModelFactoryGroup
import com.wire.android.search.removeQueryPrefix
import com.wire.android.ui.common.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.ContactStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.ValidateUserHandleResult
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.search.FederatedSearchParser
import com.wire.kalium.logic.feature.search.IsFederationSearchAllowedUseCase
import com.wire.kalium.logic.feature.search.RefreshContactStatusesUseCase
import com.wire.kalium.logic.feature.search.SearchUserResult
import com.wire.kalium.logic.feature.search.SearchUsersByHandleUseCase
import com.wire.kalium.logic.feature.search.SearchUsersByNameUseCase
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
@WireAssistedViewModelBinding(SearchManualViewModelFactoryGroup::class)
class SearchUserViewModel @AssistedInject constructor(
    @Assisted private val conversationId: ConversationId?,
    @Assisted private val onlySelfTeamAndDomain: Boolean,
    private val searchUsersByName: SearchUsersByNameUseCase,
    private val searchUsersByHandle: SearchUsersByHandleUseCase,
    private val contactMapper: ContactMapper,
    private val federatedSearchParser: FederatedSearchParser,
    private val validateUserHandle: ValidateUserHandleUseCase,
    private val refreshContactStatuses: RefreshContactStatusesUseCase,
    private val isFederationSearchAllowed: IsFederationSearchAllowedUseCase,
    private val kaliumConfigs: KaliumConfigs,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(conversationId: ConversationId?, onlySelfTeamAndDomain: Boolean): SearchUserViewModel
    }

    private val searchQueryTextFlow = MutableStateFlow(String.EMPTY)
    private val refreshEpoch = MutableStateFlow(0)
    private var generation = 0
    private var enteredForeground = false
    private val selectedContactsFlow = MutableStateFlow<ImmutableSet<Contact>>(persistentSetOf())
    var state: SearchUserState by mutableStateOf(SearchUserState(isLoading = true))
        private set

    init {
        viewModelScope.launch {
            val isOtherDomainAllowed = isFederationSearchAllowed(conversationId)
            state = state.copy(isOtherDomainAllowed = isOtherDomainAllowed)
        }

        viewModelScope.launch {
            combine(
                searchQueryTextFlow.debounce(DEFAULT_SEARCH_QUERY_DEBOUNCE).onStart { emit(String.EMPTY) }.distinctUntilChanged(),
                refreshEpoch,
            ) { query, _ -> query }
                .flatMapLatest { searchQuery ->
                    // Each started search owns a generation; skipped duplicate queries keep the running one valid.
                    val requestGeneration = ++generation
                    val (searchTerm, domain) = federatedSearchParser(searchQuery, state.isOtherDomainAllowed)
                    val isHandleSearch = validateUserHandle(searchTerm.removeQueryPrefix()) is ValidateUserHandleResult.Valid
                    val searchResult = if (isHandleSearch) searchByHandle(searchTerm, domain) else searchByName(searchTerm, domain)
                    channelFlow {
                        val results = searchResult.connected + searchResult.notConnected
                        val freshStatuses = results
                            .filter { kaliumConfigs.pendingConnections && it.contactStatus != null }
                            .associate { it.id to it.contactStatus }
                        // Keep visible statuses while refreshing, but only fresh results count as fetched this time.
                        val statuses = if (kaliumConfigs.pendingConnections) state.contactStatuses.toMutableMap() else mutableMapOf()
                        statuses.putAll(freshStatuses)
                        val fetched = freshStatuses.keys.toMutableSet()
                        selectedContactsFlow.collectLatest { selectedContacts ->
                            val ids = results.map { it.id }.toSet() + selectedContacts.map { UserId(it.id, it.domain) }
                            fun resultState() = filterResults(searchResult, selectedContacts, isHandleSearch, searchTerm)
                                .withContactStatuses(ids.associateWith { statuses[it] })
                            send(requestGeneration to resultState())
                            val missing = ids - fetched
                            if (kaliumConfigs.pendingConnections && missing.isNotEmpty()) {
                                val refreshed = refreshContactStatuses(missing)
                                statuses.putAll(refreshed)
                                fetched.addAll(missing)
                                send(requestGeneration to resultState())
                            }
                        }
                    }
                }
                .collectLatest { (requestGeneration, newState) ->
                    if (requestGeneration == generation) {
                        state = newState.copy(isOtherDomainAllowed = state.isOtherDomainAllowed, isLoading = false)
                    }
                }
        }
    }

    fun onForeground() {
        if (!kaliumConfigs.pendingConnections) return
        if (enteredForeground) {
            generation++
            refreshEpoch.value++
        }
        enteredForeground = true
    }

    fun onBackground() {
        if (!kaliumConfigs.pendingConnections) return
        // In-flight work must not publish after this screen leaves the foreground.
        generation++
    }

    fun searchQueryChanged(searchQuery: String) {
        searchQueryTextFlow.value = searchQuery
    }

    fun selectedContactsChanged(selectedContacts: ImmutableSet<Contact>) {
        // Status belongs to this screen's responses, not to incoming selection snapshots.
        selectedContactsFlow.value = selectedContacts.map { it.copy(contactStatus = null) }.toImmutableSet()
    }

    private fun filterResults(
        searchResult: SearchUserResult,
        selectedContacts: ImmutableSet<Contact>,
        isHandleSearch: Boolean,
        searchTerm: String
    ): SearchUserState {
        val selectedContactsIds = selectedContacts.map { UserId(it.id, it.domain) }
        val notSelectedContactsResult = searchResult.connected
            .map(contactMapper::fromSearchUserResult)
            .filterNot { selectedContactsIds.contains(UserId(it.id, it.domain)) }
        val notSelectedPublicResult = searchResult.notConnected
            .map(contactMapper::fromSearchUserResult)
            .filterNot { selectedContactsIds.contains(UserId(it.id, it.domain)) }
        val selectedContactsResult = selectedContacts
            .filter { selectedContact ->
                when {
                    isHandleSearch -> selectedContact.handle.contains(searchTerm, ignoreCase = true)
                    else -> selectedContact.name.contains(searchTerm, ignoreCase = true)
                }
            }
        return SearchUserState(
            contactsResult = notSelectedContactsResult.toImmutableList(),
            publicResult = notSelectedPublicResult.toImmutableList(),
            selectedResult = selectedContactsResult.toImmutableList(),
            searchQuery = searchTerm
        )
    }

    private suspend fun searchByHandle(searchTerm: String, domain: String?): SearchUserResult =
        searchUsersByHandle(
            searchTerm,
            excludingMembersOfConversation = conversationId,
            onlySelfTeamAndDomain = onlySelfTeamAndDomain,
            customDomain = domain
        )

    private suspend fun searchByName(searchTerm: String, domain: String?): SearchUserResult =
        searchUsersByName(
            searchTerm,
            excludingMembersOfConversation = conversationId,
            onlySelfTeamAndDomain = onlySelfTeamAndDomain,
            customDomain = domain
        )
}

data class SearchUserState(
    val contactsResult: ImmutableList<Contact> = persistentListOf(),
    val publicResult: ImmutableList<Contact> = persistentListOf(),
    val selectedResult: ImmutableList<Contact> = persistentListOf(),
    val searchQuery: String = String.EMPTY,
    val isOtherDomainAllowed: Boolean = false,
    val isLoading: Boolean = false,
    val contactStatuses: Map<UserId, ContactStatus?> = emptyMap(),
) {
    fun withContactStatuses(statuses: Map<UserId, ContactStatus?>): SearchUserState {
        fun Contact.refreshed() = copy(contactStatus = statuses[UserId(id, domain)])
        return copy(
            contactsResult = contactsResult.map { it.refreshed() }.toImmutableList(),
            publicResult = publicResult.map { it.refreshed() }.toImmutableList(),
            selectedResult = selectedResult.map { it.refreshed() }.toImmutableList(),
            contactStatuses = statuses,
        )
    }
}
