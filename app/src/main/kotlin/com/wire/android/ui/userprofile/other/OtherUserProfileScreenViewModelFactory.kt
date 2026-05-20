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
package com.wire.android.ui.userprofile.other

import com.wire.android.mapper.UserTypeMapper
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.client.FetchUsersClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.conversation.IsOneToOneConversationCreatedUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetMLSClientIdentityUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.IsOtherUserE2EIVerifiedUseCase
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class OtherUserProfileScreenViewModelFactory(
    private val dispatchers: DispatcherProvider,
    private val observeUserInfo: ObserveUserInfoUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val observeConversationRoleForUser: ObserveConversationRoleForUserUseCase,
    private val removeMemberFromConversation: RemoveMemberFromConversationUseCase,
    private val updateMemberRole: UpdateConversationMemberRoleUseCase,
    private val observeClientList: ObserveClientsByUserIdUseCase,
    private val fetchUsersClients: FetchUsersClientsFromRemoteUseCase,
    private val getUserE2eiCertificateStatus: IsOtherUserE2EIVerifiedUseCase,
    private val isOneToOneConversationCreated: IsOneToOneConversationCreatedUseCase,
    private val mlsClientIdentity: GetMLSClientIdentityUseCase,
    private val isE2EIEnabled: IsE2EIEnabledUseCase,
) {
    fun create(args: OtherUserProfileNavArgs): OtherUserProfileScreenViewModel = OtherUserProfileScreenViewModel(
        otherUserProfileNavArgs = args,
        dispatchers = dispatchers,
        observeUserInfo = observeUserInfo,
        userTypeMapper = userTypeMapper,
        observeConversationRoleForUser = observeConversationRoleForUser,
        removeMemberFromConversation = removeMemberFromConversation,
        updateMemberRole = updateMemberRole,
        observeClientList = observeClientList,
        fetchUsersClients = fetchUsersClients,
        getUserE2eiCertificateStatus = getUserE2eiCertificateStatus,
        isOneToOneConversationCreated = isOneToOneConversationCreated,
        mlsClientIdentity = mlsClientIdentity,
        isE2EIEnabled = isE2EIEnabled,
    )
}
