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
package com.wire.android.search

import com.wire.android.mapper.ContactMapper
import com.wire.android.search.apps.SearchAppsViewModel
import com.wire.android.search.users.SearchUserViewModel
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.app.ObserveAllAppsUseCase
import com.wire.kalium.logic.feature.app.SearchAppsByNameUseCase
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.search.FederatedSearchParser
import com.wire.kalium.logic.feature.search.IsFederationSearchAllowedUseCase
import com.wire.kalium.logic.feature.search.SearchUsersByHandleUseCase
import com.wire.kalium.logic.feature.search.SearchUsersByNameUseCase
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.service.SearchServicesByNameUseCase
import com.wire.kalium.logic.feature.service.SyncServicesUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dev.zacsweers.metro.Inject

@Suppress("LongParameterList")
class SearchViewModelFactory @Inject constructor(
    private val searchUsersByName: SearchUsersByNameUseCase,
    private val searchUsersByHandle: SearchUsersByHandleUseCase,
    private val contactMapper: ContactMapper,
    private val federatedSearchParser: FederatedSearchParser,
    private val validateUserHandle: ValidateUserHandleUseCase,
    private val isFederationSearchAllowed: IsFederationSearchAllowedUseCase,
    private val observeAllServices: ObserveAllServicesUseCase,
    private val syncServices: SyncServicesUseCase,
    private val observeAllApps: ObserveAllAppsUseCase,
    private val searchServicesByName: SearchServicesByNameUseCase,
    private val searchAppsByName: SearchAppsByNameUseCase,
    private val observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase,
    private val observeSelfUser: ObserveSelfUserUseCase,
) {
    fun searchUserViewModel(
        conversationId: ConversationId? = null,
        onlyConnectedContacts: Boolean = false,
    ) = SearchUserViewModel(
        conversationId = conversationId,
        onlyConnectedContacts = onlyConnectedContacts,
        searchUsersByName = searchUsersByName,
        searchUsersByHandle = searchUsersByHandle,
        contactMapper = contactMapper,
        federatedSearchParser = federatedSearchParser,
        validateUserHandle = validateUserHandle,
        isFederationSearchAllowed = isFederationSearchAllowed,
    )

    fun searchAppsViewModel(protocolInfo: Conversation.ProtocolInfo?) = SearchAppsViewModel(
        protocolInfo = protocolInfo,
        getAllServices = observeAllServices,
        syncServices = syncServices,
        getAllApps = observeAllApps,
        contactMapper = contactMapper,
        searchServicesByName = searchServicesByName,
        searchAppsByName = searchAppsByName,
        isAppsAllowedForUsage = observeIsAppsAllowedForUsage,
        observeSelfUser = observeSelfUser,
    )
}
