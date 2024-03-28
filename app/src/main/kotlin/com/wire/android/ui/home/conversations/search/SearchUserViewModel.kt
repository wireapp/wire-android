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
package com.wire.android.ui.home.conversations.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.mapper.ContactMapper
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.feature.auth.ValidateUserHandleResult
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.search.FederatedSearchParser
import com.wire.kalium.logic.feature.search.SearchByHandleUseCase
import com.wire.kalium.logic.feature.search.SearchUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import javax.inject.Inject

@HiltViewModel
class SearchUserViewModel @Inject constructor(
    private val searchUserUseCase: SearchUsersUseCase,
    private val searchByHandleUseCase: SearchByHandleUseCase,
    private val contactMapper: ContactMapper,
    private val federatedSearchParser: FederatedSearchParser,
    private val validateUserHandle: ValidateUserHandleUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    @Suppress("TooGenericExceptionCaught")
    private val addMembersSearchNavArgs: AddMembersSearchNavArgs? = try {
        savedStateHandle.navArgs<AddMembersSearchNavArgs>()
    } catch (e: RuntimeException) {
        null
    }

    var state: SearchUserState by mutableStateOf(SearchUserState())
        private set

    fun search(query: String) = viewModelScope.launch {
        safeSearch(query)
    }

    @VisibleForTesting
    suspend fun safeSearch(query: String) {
        val (searchTerm, domain) = federatedSearchParser(query)
        val isHandleSearch = validateUserHandle(searchTerm.removeQueryPrefix()) is ValidateUserHandleResult.Valid

        if (isHandleSearch) {
            searchByHandle(searchTerm, domain)
        } else {
            searchByName(searchTerm, domain)
        }
    }

    private suspend fun searchByHandle(searchTerm: String, domain: String?) {
        searchByHandleUseCase(
            searchTerm,
            excludingConversation = addMembersSearchNavArgs?.conversationId,
            customDomain = domain
        ).also { userSearchEntities ->
            state = state.copy(
                contactsResult = userSearchEntities.connected.map(contactMapper::fromSearchUserResult).toImmutableList(),
                publicResult = userSearchEntities.notConnected.map(contactMapper::fromSearchUserResult).toImmutableList()
            )
        }
    }

    private suspend fun searchByName(searchTerm: String, domain: String?) {
        searchUserUseCase(
            searchTerm,
            excludingMembersOfConversation = addMembersSearchNavArgs?.conversationId,
            customDomain = domain
        ).also { userSearchEntities ->
            state = state.copy(
                contactsResult = userSearchEntities.connected.map(contactMapper::fromSearchUserResult).toImmutableList(),
                publicResult = userSearchEntities.notConnected.map(contactMapper::fromSearchUserResult).toImmutableList()
            )
        }
    }
}

data class SearchUserState(
    val contactsResult: ImmutableList<Contact> = persistentListOf(),
    val publicResult: ImmutableList<Contact> = persistentListOf(),
    val includeServices: Boolean = false,
    val noneSearchSucceeded: Boolean = false
)
