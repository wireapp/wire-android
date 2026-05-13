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
package com.wire.android.ui.userprofile.service

import com.wire.android.di.CurrentAccount
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.app.GetAppByIdUseCase
import com.wire.kalium.logic.feature.app.ObserveIsAppMemberUseCase
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.AddServiceToConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.service.GetServiceByIdUseCase
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class ServiceDetailsViewModelFactory(
    private val dispatchers: DispatcherProvider,
    @CurrentAccount private val selfUserId: UserId,
    private val getServiceById: GetServiceByIdUseCase,
    private val getAppById: GetAppByIdUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeIsServiceMember: ObserveIsServiceMemberUseCase,
    private val observeIsAppMember: ObserveIsAppMemberUseCase,
    private val observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase,
    private val observeConversationRoleForUser: ObserveConversationRoleForUserUseCase,
    private val removeMemberFromConversation: RemoveMemberFromConversationUseCase,
    private val addServiceToConversation: AddServiceToConversationUseCase,
    private val addMemberToConversation: AddMemberToConversationUseCase,
) {
    fun create(args: ServiceDetailsNavArgs): ServiceDetailsViewModel = ServiceDetailsViewModel(
        dispatchers = dispatchers,
        selfUserId = selfUserId,
        getServiceById = getServiceById,
        getAppById = getAppById,
        observeConversationDetails = observeConversationDetails,
        observeIsServiceMember = observeIsServiceMember,
        observeIsAppMember = observeIsAppMember,
        observeIsAppsAllowedForUsage = observeIsAppsAllowedForUsage,
        observeConversationRoleForUser = observeConversationRoleForUser,
        removeMemberFromConversation = removeMemberFromConversation,
        addServiceToConversation = addServiceToConversation,
        addMemberToConversation = addMemberToConversation,
        serviceDetailsNavArgs = args,
    )
}
