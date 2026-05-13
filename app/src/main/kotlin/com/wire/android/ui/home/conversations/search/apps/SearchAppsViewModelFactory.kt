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
package com.wire.android.ui.home.conversations.search.apps

import com.wire.android.mapper.ContactMapper
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.feature.app.ObserveAllAppsUseCase
import com.wire.kalium.logic.feature.app.SearchAppsByNameUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.service.SearchServicesByNameUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dev.zacsweers.metro.Inject

@Inject
class SearchAppsViewModelFactory(
    private val getAllServices: ObserveAllServicesUseCase,
    private val getAllApps: ObserveAllAppsUseCase,
    private val contactMapper: ContactMapper,
    private val searchServicesByName: SearchServicesByNameUseCase,
    private val searchAppsByName: SearchAppsByNameUseCase,
    private val isAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase,
    private val observeSelfUser: ObserveSelfUserUseCase,
) {
    fun create(protocolInfo: Conversation.ProtocolInfo?): SearchAppsViewModel = SearchAppsViewModel(
        protocolInfo = protocolInfo,
        getAllServices = getAllServices,
        getAllApps = getAllApps,
        contactMapper = contactMapper,
        searchServicesByName = searchServicesByName,
        searchAppsByName = searchAppsByName,
        isAppsAllowedForUsage = isAppsAllowedForUsage,
        observeSelfUser = observeSelfUser,
    )
}
