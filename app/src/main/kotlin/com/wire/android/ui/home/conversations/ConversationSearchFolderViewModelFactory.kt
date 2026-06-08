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
package com.wire.android.ui.home.conversations

import androidx.lifecycle.SavedStateHandle
import com.wire.android.mapper.ContactMapper
import com.wire.android.ui.home.conversations.folder.ConversationFoldersStateArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersVMImpl
import com.wire.android.ui.home.conversations.folder.MoveConversationToFolderArgs
import com.wire.android.ui.home.conversations.folder.MoveConversationToFolderVMImpl
import com.wire.android.ui.home.conversations.folder.NewFolderViewModel
import com.wire.android.ui.home.conversations.promoteadmin.PromoteAdminViewModel
import com.wire.android.ui.home.conversations.search.SearchUserViewModel
import com.wire.android.ui.home.conversations.search.adddembertoconversation.AddMembersToConversationViewModel
import com.wire.android.ui.home.conversations.search.apps.SearchAppsViewModel
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesViewModel
import com.wire.android.ui.home.conversations.usecase.GetConversationMessagesFromSearchUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.feature.app.ObserveAllAppsUseCase
import com.wire.kalium.logic.feature.app.SearchAppsByNameUseCase
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveEligibleMembersForConversationAdminRoleUseCase
import com.wire.kalium.logic.feature.conversation.PromoteAdminAndLeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.folder.CreateConversationFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.MoveConversationToFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.ObserveUserFoldersUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.search.FederatedSearchParser
import com.wire.kalium.logic.feature.search.IsFederationSearchAllowedUseCase
import com.wire.kalium.logic.feature.search.SearchByHandleUseCase
import com.wire.kalium.logic.feature.search.SearchUsersUseCase
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.service.SearchServicesByNameUseCase
import com.wire.kalium.logic.feature.service.SyncServicesUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dev.zacsweers.metro.Inject

@Suppress("LongParameterList")
class ConversationSearchFolderViewModelFactory @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val observeUserFolders: ObserveUserFoldersUseCase,
    private val createConversationFolder: CreateConversationFolderUseCase,
    private val moveConversationToFolder: MoveConversationToFolderUseCase,
    private val searchUsers: SearchUsersUseCase,
    private val searchByHandle: SearchByHandleUseCase,
    private val contactMapper: ContactMapper,
    private val federatedSearchParser: FederatedSearchParser,
    private val validateUserHandle: ValidateUserHandleUseCase,
    private val isFederationSearchAllowed: IsFederationSearchAllowedUseCase,
    private val addMemberToConversation: AddMemberToConversationUseCase,
    private val getConversationMessagesFromSearch: GetConversationMessagesFromSearchUseCase,
    private val observeAllServices: ObserveAllServicesUseCase,
    private val syncServices: SyncServicesUseCase,
    private val observeAllApps: ObserveAllAppsUseCase,
    private val searchServicesByName: SearchServicesByNameUseCase,
    private val searchAppsByName: SearchAppsByNameUseCase,
    private val observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase,
    private val observeSelfUser: ObserveSelfUserUseCase,
    private val promoteAdminAndLeave: PromoteAdminAndLeaveConversationUseCase,
    private val observeEligibleMembers: ObserveEligibleMembersForConversationAdminRoleUseCase,
) {
    fun conversationFoldersViewModel(args: ConversationFoldersStateArgs) = ConversationFoldersVMImpl(
        args = args,
        observeUserFoldersUseCase = observeUserFolders,
    )

    fun moveConversationToFolderViewModel(args: MoveConversationToFolderArgs) = MoveConversationToFolderVMImpl(
        dispatchers = dispatchers,
        args = args,
        moveConversationToFolder = moveConversationToFolder,
    )

    fun newFolderViewModel() = NewFolderViewModel(
        observeUserFolders = observeUserFolders,
        createConversationFolder = createConversationFolder,
    )

    fun searchUserViewModel(savedStateHandle: SavedStateHandle) = SearchUserViewModel(
        searchUserUseCase = searchUsers,
        searchByHandleUseCase = searchByHandle,
        contactMapper = contactMapper,
        federatedSearchParser = federatedSearchParser,
        validateUserHandle = validateUserHandle,
        isFederationSearchAllowed = isFederationSearchAllowed,
        savedStateHandle = savedStateHandle,
    )

    fun addMembersToConversationViewModel(savedStateHandle: SavedStateHandle) = AddMembersToConversationViewModel(
        addMemberToConversation = addMemberToConversation,
        dispatchers = dispatchers,
        savedStateHandle = savedStateHandle,
    )

    fun searchConversationMessagesViewModel(savedStateHandle: SavedStateHandle) = SearchConversationMessagesViewModel(
        getSearchMessagesForConversation = getConversationMessagesFromSearch,
        dispatchers = dispatchers,
        savedStateHandle = savedStateHandle,
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

    fun promoteAdminViewModel(savedStateHandle: SavedStateHandle) = PromoteAdminViewModel(
        promoteAdminAndLeave = promoteAdminAndLeave,
        observeEligibleMembers = observeEligibleMembers,
        dispatchers = dispatchers,
        savedStateHandle = savedStateHandle,
    )
}
