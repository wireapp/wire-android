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

package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.ContactMapper
import com.wire.android.ui.common.groupname.GroupMetadataState
import com.wire.android.ui.common.groupname.GroupNameValidator
import com.wire.android.ui.home.conversations.search.SearchAllPeopleViewModel
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.common.CreateGroupState
import com.wire.android.ui.home.newconversation.groupOptions.GroupOptionState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationOptions
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.CreateGroupConversationUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchPublicUsersUseCase
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
import com.wire.kalium.logic.feature.user.IsSelfATeamMemberUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class NewConversationViewModel @Inject constructor(
    private val createGroupConversation: CreateGroupConversationUseCase,
    isSelfATeamMember: IsSelfATeamMemberUseCase,
    getAllKnownUsers: GetAllContactsUseCase,
    searchKnownUsers: SearchKnownUsersUseCase,
    searchPublicUsers: SearchPublicUsersUseCase,
    getAllServices: ObserveAllServicesUseCase,
    contactMapper: ContactMapper,
    isMLSEnabled: IsMLSEnabledUseCase,
    dispatchers: DispatcherProvider,
    sendConnectionRequest: SendConnectionRequestUseCase
) : SearchAllPeopleViewModel(
    getAllKnownUsers = getAllKnownUsers,
    sendConnectionRequest = sendConnectionRequest,
    searchKnownUsers = searchKnownUsers,
    searchPublicUsers = searchPublicUsers,
    contactMapper = contactMapper,
    dispatcher = dispatchers,
    getAllServices = getAllServices
) {

    var newGroupState: GroupMetadataState by mutableStateOf(
        GroupMetadataState(
            // TODO: remove the runBlocking
            mlsEnabled = runBlocking { isMLSEnabled() },
        )
    )

    var groupOptionsState: GroupOptionState by mutableStateOf(GroupOptionState())
    var createGroupState: CreateGroupState by mutableStateOf(CreateGroupState())

    init {
        viewModelScope.launch {
            val isSelfTeamMember = isSelfATeamMember()
            newGroupState = newGroupState.copy(isSelfTeamMember = isSelfTeamMember)
        }
    }

    fun onGroupNameChange(newText: TextFieldValue) {
        newGroupState = GroupNameValidator.onGroupNameChange(newText, newGroupState)
    }

    fun onCreateGroupErrorDismiss() {
        createGroupState = createGroupState.copy(error = null)
    }

    fun onAllowGuestStatusChanged(status: Boolean) {
        groupOptionsState = groupOptionsState.copy(isAllowGuestEnabled = status)
    }

    fun onAllowServicesStatusChanged(status: Boolean) {
        groupOptionsState = groupOptionsState.copy(isAllowServicesEnabled = status)
    }

    fun onReadReceiptStatusChanged(status: Boolean) {
        groupOptionsState = groupOptionsState.copy(isReadReceiptEnabled = status)
    }

    fun onAllowGuestsDialogDismissed() {
        groupOptionsState = groupOptionsState.copy(showAllowGuestsDialog = false)
    }

    fun onAllowGuestsClicked(onCreated: (ConversationId) -> Unit) {
        onAllowGuestsDialogDismissed()
        onAllowGuestStatusChanged(true)
        createGroupForTeamAccounts(false, onCreated)
    }

    fun onNotAllowGuestClicked(onCreated: (ConversationId) -> Unit) {
        onAllowGuestsDialogDismissed()
        onAllowGuestStatusChanged(false)
        removeGuestsIfNotAllowed()
        createGroupForTeamAccounts(false, onCreated)
    }

    private fun removeGuestsIfNotAllowed() {
        if (!groupOptionsState.isAllowGuestEnabled) {
            val contactsToRemove = state
                .contactsAddedToGroup
                .filter {
                    it.membership in setOf(Membership.Guest, Membership.Federated)
                }.toSet()
            removeContactsFromGroup(contactsToRemove)
        }
    }

    private fun checkIfGuestAdded(): Boolean {
        if (!groupOptionsState.isAllowGuestEnabled) {
            for (item in state.contactsAddedToGroup) {
                if (item.membership == Membership.Guest ||
                    item.membership == Membership.Federated
                ) {
                    groupOptionsState = groupOptionsState.copy(showAllowGuestsDialog = true)
                    return true
                }
            }
        }
        return false
    }

    fun createGroup(onCreated: (ConversationId) -> Unit) {
        newGroupState.isSelfTeamMember?.let {
            if (it) {
                createGroupForTeamAccounts(true, onCreated)
            } else {
                // Personal Account
                createGroupForPersonalAccounts(onCreated)
            }
        }
    }

    private fun createGroupForPersonalAccounts(onCreated: (ConversationId) -> Unit) {
        viewModelScope.launch {
            newGroupState = newGroupState.copy(isLoading = true)
            val result = createGroupConversation(
                name = newGroupState.groupName.text,
                // TODO: change the id in Contact to UserId instead of String
                userIdList = state.contactsAddedToGroup.map { contact -> UserId(contact.id, contact.domain) },
                options = ConversationOptions().copy(
                    protocol = ConversationOptions.Protocol.PROTEUS,
                    accessRole = Conversation.defaultGroupAccessRoles,
                    access = Conversation.defaultGroupAccess
                )
            )
            handleNewGroupCreationResult(result)?.let(onCreated)
        }
    }

    private fun createGroupForTeamAccounts(shouldCheckGuests: Boolean = true, onCreated: (ConversationId) -> Unit) {
        if (shouldCheckGuests && checkIfGuestAdded()) return
        viewModelScope.launch {
            groupOptionsState = groupOptionsState.copy(isLoading = true)
            val result = createGroupConversation(
                name = newGroupState.groupName.text,
                // TODO: change the id in Contact to UserId instead of String
                userIdList = state.contactsAddedToGroup.map { contact -> UserId(contact.id, contact.domain) },
                options = ConversationOptions().copy(
                    protocol = newGroupState.groupProtocol,
                    readReceiptsEnabled = groupOptionsState.isReadReceiptEnabled,
                    accessRole = Conversation.accessRolesFor(
                        guestAllowed = groupOptionsState.isAllowGuestEnabled,
                        servicesAllowed = groupOptionsState.isAllowServicesEnabled,
                        nonTeamMembersAllowed = groupOptionsState.isAllowGuestEnabled
                    ),
                    access = Conversation.accessFor(groupOptionsState.isAllowGuestEnabled)
                )
            )
            handleNewGroupCreationResult(result)?.let(onCreated)
        }
    }

    private fun handleNewGroupCreationResult(result: CreateGroupConversationUseCase.Result): ConversationId? {
        return when (result) {
            is CreateGroupConversationUseCase.Result.Success -> {
                newGroupState = newGroupState.copy(isLoading = false)
                result.conversation.id
            }

            CreateGroupConversationUseCase.Result.SyncFailure -> {
                appLogger.d("Can't create group due to SyncFailure")
                groupOptionsState = groupOptionsState.copy(isLoading = false)
                newGroupState = newGroupState.copy(isLoading = false)
                createGroupState = createGroupState.copy(error = CreateGroupState.Error.LackingConnection)
                null
            }

            is CreateGroupConversationUseCase.Result.UnknownFailure -> {
                appLogger.w("Error while creating a group ${result.cause}")
                groupOptionsState = groupOptionsState.copy(isLoading = false)
                newGroupState = newGroupState.copy(isLoading = false)
                createGroupState = createGroupState.copy(error = CreateGroupState.Error.Unknown)
                null
            }

            is CreateGroupConversationUseCase.Result.BackendConflictFailure -> {
                groupOptionsState = groupOptionsState.copy(isLoading = false)
                newGroupState = newGroupState.copy(isLoading = false)
                createGroupState = createGroupState.copy(error = CreateGroupState.Error.ConflictedBackends(result.domains))
                null
            }
        }
    }

    fun onGroupNameErrorAnimated() {
        newGroupState = GroupNameValidator.onGroupNameErrorAnimated(newGroupState)
    }
}
