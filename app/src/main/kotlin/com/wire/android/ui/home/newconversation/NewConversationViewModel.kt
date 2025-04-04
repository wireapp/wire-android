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

package com.wire.android.ui.home.newconversation

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.ui.common.groupname.GroupMetadataState
import com.wire.android.ui.common.groupname.GroupNameValidator
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAddPermissionType
import com.wire.android.ui.home.newconversation.common.CreateGroupState
import com.wire.android.ui.home.newconversation.groupOptions.GroupOptionState
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationOptions
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.channels.ChannelCreationPermission
import com.wire.kalium.logic.feature.channels.ObserveChannelsCreationPermissionUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.ConversationCreationResult
import com.wire.kalium.logic.feature.conversation.createconversation.CreateChannelUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.CreateRegularGroupUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class NewConversationViewModel @Inject constructor(
    private val createRegularGroup: CreateRegularGroupUseCase,
    private val createChannel: CreateChannelUseCase,
    private val isUserAllowedToCreateChannels: ObserveChannelsCreationPermissionUseCase,
    private val getSelfUser: GetSelfUserUseCase,
    private val globalDataStore: GlobalDataStore,
    getDefaultProtocol: GetDefaultProtocolUseCase
) : ViewModel() {

    val newGroupNameTextState: TextFieldState = TextFieldState()
    var newGroupState: GroupMetadataState by mutableStateOf(
        GroupMetadataState().let {
            val defaultProtocol = ConversationOptions
                .Protocol
                .fromSupportedProtocolToConversationOptionsProtocol(getDefaultProtocol())
            it.copy(groupProtocol = defaultProtocol)
        }
    )

    var groupOptionsState: GroupOptionState by mutableStateOf(
        GroupOptionState().let {
            val isMLS = newGroupState.groupProtocol == ConversationOptions.Protocol.MLS
            it.copy(
                isAllowServicesEnabled = !isMLS,
                isAllowServicesPossible = !isMLS,
            )
        }
    )
    var createGroupState: CreateGroupState by mutableStateOf(CreateGroupState())

    init {
        setConversationCreationParam()
        observeGroupNameChanges()
        observeChannelCreationPermission()
        getWireCellFeatureState()
    }

    private fun getWireCellFeatureState() = viewModelScope.launch {
        val isWireCellsFeatureEnabled = globalDataStore.wireCellsEnabled().firstOrNull() ?: false

        if (isWireCellsFeatureEnabled) {
            groupOptionsState = groupOptionsState.copy(
                isWireCellsEnabled = true
            )
        }
    }

    fun setChannelAccess(channelAccessType: ChannelAccessType) {
        newGroupState = newGroupState.copy(channelAccessType = channelAccessType)
    }

    fun setChannelPermission(channelAddPermissionType: ChannelAddPermissionType) {
        newGroupState = newGroupState.copy(channelAddPermissionType = channelAddPermissionType)
    }

    fun setIsChannel(isChannel: Boolean) {
        newGroupState = newGroupState.copy(isChannel = isChannel)
    }

    private fun setConversationCreationParam() {
        viewModelScope.launch {
            val selfUser = getSelfUser()
            val isSelfTeamMember = selfUser?.teamId != null
            val isSelfExternalTeamMember = selfUser?.userType == UserType.EXTERNAL
            newGroupState = newGroupState.copy(
                isSelfTeamMember = isSelfTeamMember,
                isGroupCreatingAllowed = !isSelfExternalTeamMember
            )
        }
    }

    private fun observeGroupNameChanges() {
        viewModelScope.launch {
            newGroupNameTextState.textAsFlow()
                .dropWhile { it.isEmpty() } // ignore first empty value to not show the error before the user typed anything
                .collectLatest {
                    newGroupState = GroupNameValidator.onGroupNameChange(it.toString(), newGroupState)
                }
        }
    }

    private fun observeChannelCreationPermission() {
        viewModelScope.launch {
            isUserAllowedToCreateChannels()
                .collectLatest {
                val isChannelCreationPossible = it is ChannelCreationPermission.Allowed
                newGroupState = newGroupState.copy(isChannelCreationPossible = isChannelCreationPossible)
            }
        }
    }

    fun updateSelectedContacts(selected: Boolean, contact: Contact) {
        if (selected) {
            newGroupState = newGroupState.copy(selectedUsers = (newGroupState.selectedUsers + contact).toImmutableSet())
        } else {
            newGroupState = newGroupState.copy(selectedUsers = newGroupState.selectedUsers.filterNot {
                it.id == contact.id &&
                        it.domain == contact.domain
            }.toImmutableSet())
        }
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
            val newList = newGroupState
                .selectedUsers
                .filter {
                    it.membership != Membership.Guest &&
                            it.membership != Membership.Federated
                }.toImmutableSet()

            newGroupState = newGroupState.copy(selectedUsers = newList)
        }
    }

    private fun checkIfGuestAdded(): Boolean {
        if (groupOptionsState.isAllowGuestEnabled) return false

        val isGuestSelected = !newGroupState.selectedUsers.none {
            it.membership == Membership.Guest ||
                    it.membership == Membership.Federated
        }
        if (isGuestSelected) {
            groupOptionsState = groupOptionsState.copy(showAllowGuestsDialog = true)
        }
        return isGuestSelected
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

    fun createChannel(onCreated: (ConversationId) -> Unit) {
        viewModelScope.launch {
            groupOptionsState = groupOptionsState.copy(isLoading = true)
            val result = createChannel(
                name = newGroupNameTextState.text.toString(),
                userIdList = newGroupState.selectedUsers.map { UserId(it.id, it.domain) },
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

    private fun createGroupForPersonalAccounts(onCreated: (ConversationId) -> Unit) {
        viewModelScope.launch {
            newGroupState = newGroupState.copy(isLoading = true)
            val result = createRegularGroup(
                name = newGroupNameTextState.text.toString(),
                userIdList = newGroupState.selectedUsers.map { UserId(it.id, it.domain) },
                options = ConversationOptions().copy(
                    protocol = ConversationOptions.Protocol.PROTEUS,
                    accessRole = Conversation.defaultGroupAccessRoles,
                    access = Conversation.defaultGroupAccess,
                    wireCellEnabled = groupOptionsState.isWireCellsEnabled ?: false,
                )
            )
            handleNewGroupCreationResult(result)?.let(onCreated)
        }
    }

    private fun createGroupForTeamAccounts(shouldCheckGuests: Boolean = true, onCreated: (ConversationId) -> Unit) {
        if (shouldCheckGuests && checkIfGuestAdded()) return
        viewModelScope.launch {
            groupOptionsState = groupOptionsState.copy(isLoading = true)
            val result = createRegularGroup(
                name = newGroupNameTextState.text.toString(),
                // TODO: change the id in Contact to UserId instead of String
                userIdList = newGroupState.selectedUsers.map { UserId(it.id, it.domain) },
                options = ConversationOptions().copy(
                    protocol = newGroupState.groupProtocol,
                    readReceiptsEnabled = groupOptionsState.isReadReceiptEnabled,
                    wireCellEnabled = groupOptionsState.isWireCellsEnabled ?: false,
                    accessRole = Conversation.accessRolesFor(
                        guestAllowed = groupOptionsState.isAllowGuestEnabled,
                        servicesAllowed = groupOptionsState.isAllowServicesEnabled,
                        nonTeamMembersAllowed = groupOptionsState.isAllowGuestEnabled
                    ),
                    access = Conversation.accessFor(groupOptionsState.isAllowGuestEnabled),
                )
            )
            handleNewGroupCreationResult(result)?.let(onCreated)
        }
    }

    private fun handleNewGroupCreationResult(result: ConversationCreationResult): ConversationId? {
        return when (result) {
            is ConversationCreationResult.Success -> {
                newGroupState = newGroupState.copy(isLoading = false)
                result.conversation.id
            }

            ConversationCreationResult.SyncFailure -> {
                appLogger.d("Can't create group due to SyncFailure")
                groupOptionsState = groupOptionsState.copy(isLoading = false)
                newGroupState = newGroupState.copy(isLoading = false)
                createGroupState = createGroupState.copy(error = CreateGroupState.Error.LackingConnection)
                null
            }

            is ConversationCreationResult.UnknownFailure -> {
                appLogger.w("Error while creating a group ${result.cause}")
                groupOptionsState = groupOptionsState.copy(isLoading = false)
                newGroupState = newGroupState.copy(isLoading = false)
                createGroupState = createGroupState.copy(error = CreateGroupState.Error.Unknown)
                null
            }

            is ConversationCreationResult.BackendConflictFailure -> {
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

    fun onEnableWireCellChanged(enabled: Boolean) {
        groupOptionsState = groupOptionsState.copy(isWireCellsEnabled = enabled)
    }
}
