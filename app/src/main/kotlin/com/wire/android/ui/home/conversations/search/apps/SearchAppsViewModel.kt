/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.search.apps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.mapper.ContactMapper
import com.wire.android.ui.common.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.EMPTY
import com.wire.android.util.AppsUtil
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.user.type.isTeamAdmin
import com.wire.kalium.logic.feature.app.ObserveAllAppsUseCase
import com.wire.kalium.logic.feature.app.SearchAppsByNameUseCase
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedResult
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.service.SearchServicesByNameUseCase
import com.wire.kalium.logic.feature.service.SyncServicesUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class SearchAppsViewModel(
    val protocolInfo: Conversation.ProtocolInfo?,
    private val getAllServices: ObserveAllServicesUseCase,
    private val syncServices: SyncServicesUseCase,
    private val getAllApps: ObserveAllAppsUseCase,
    private val contactMapper: ContactMapper,
    private val searchServicesByName: SearchServicesByNameUseCase,
    private val searchAppsByName: SearchAppsByNameUseCase,
    private val isAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase,
    private val observeSelfUser: ObserveSelfUserUseCase
) : ViewModel() {
    private val searchQueryTextFlow = MutableStateFlow(String.EMPTY)
    private var servicesSynced = false
    var state: SearchServicesState by mutableStateOf(SearchServicesState(isLoading = true))
        private set

    init {
        viewModelScope.launch {
            combine(
                observeSelfUser(),
                isAppsAllowedForUsage(),
                searchQueryTextFlow.onStart { emit(String.EMPTY) }
            ) { selfUser, isEnabledResult, query ->
                Triple(selfUser, isEnabledResult, query)
            }.debounce(DEFAULT_SEARCH_QUERY_DEBOUNCE).collectLatest { (selfUser, isEnabledResult, query) ->
                val protocolAwareResult = resolveProtocolAwareAppsAllowedResult(isEnabledResult)
                state = state.copy(isTeamAllowedToUseApps = protocolAwareResult, isSelfATeamAdmin = selfUser.userType.isTeamAdmin())
                if (protocolAwareResult is AppsAllowedResult.Enabled) {
                    search(query, protocolAwareResult)
                } else {
                    state = state.copy(isLoading = false, result = persistentListOf())
                }
            }
        }
    }

    private fun resolveProtocolAwareAppsAllowedResult(appsAllowedResult: AppsAllowedResult): AppsAllowedResult =
        if (
            appsAllowedResult is AppsAllowedResult.Enabled &&
            protocolInfo is Conversation.ProtocolInfo.MLS &&
            !AppsUtil.isAppsAllowed(appsAllowedResult, protocolInfo)
        ) {
            AppsAllowedResult.Disabled
        } else {
            appsAllowedResult
        }

    fun searchQueryChanged(searchQuery: String) {
        viewModelScope.launch {
            searchQueryTextFlow.emit(searchQuery)
        }
    }

    private fun search(query: String, appsAllowedResult: AppsAllowedResult.Enabled) {
        viewModelScope.launch {
            val showNewApps = AppsUtil.isAppsAllowed(
                appsAllowedResult = appsAllowedResult,
                conversationProtocol = protocolInfo
            )

            val result = if (showNewApps) {
                if (query.isEmpty()) getAllApps() else searchAppsByName(query)
            } else {
                if (!servicesSynced) {
                    servicesSynced = true
                    launch { syncServices() }
                }
                if (query.isEmpty()) getAllServices() else searchServicesByName(query)
            }

            state = state.copy(
                isLoading = false,
                searchQuery = query,
                result = result.first().map(contactMapper::fromService).toImmutableList()
            )
        }
    }
}

data class SearchServicesState(
    val result: ImmutableList<Contact> = persistentListOf(),
    val searchQuery: String = String.EMPTY,
    val isTeamAllowedToUseApps: AppsAllowedResult = AppsAllowedResult.Disabled,
    val isSelfATeamAdmin: Boolean = false,
    val isLoading: Boolean = false,
)
