package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.ContactMapper
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.common.groupname.GroupMetadataState
import com.wire.android.ui.common.groupname.GroupNameValidator
import com.wire.android.ui.home.conversations.search.SearchAllPeopleViewModel
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.groupOptions.GroupOptionState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationOptions
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.CreateGroupConversationUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchPublicUsersUseCase
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
    private val isSelfATeamMember: IsSelfATeamMemberUseCase,
    getAllKnownUsers: GetAllContactsUseCase,
    searchKnownUsers: SearchKnownUsersUseCase,
    searchPublicUsers: SearchPublicUsersUseCase,
    contactMapper: ContactMapper,
    isMLSEnabled: IsMLSEnabledUseCase,
    dispatchers: DispatcherProvider,
    sendConnectionRequest: SendConnectionRequestUseCase,
    navigationManager: NavigationManager
) : SearchAllPeopleViewModel(
    getAllKnownUsers = getAllKnownUsers,
    sendConnectionRequest = sendConnectionRequest,
    searchKnownUsers = searchKnownUsers,
    searchPublicUsers = searchPublicUsers,
    contactMapper = contactMapper,
    dispatcher = dispatchers,
    navigationManager = navigationManager
) {

    var newGroupState: GroupMetadataState by mutableStateOf(
        GroupMetadataState(
            mlsEnabled = isMLSEnabled(),
            isSelfTeamMember = runBlocking { isSelfATeamMember() })
    )

    var groupOptionsState: GroupOptionState by mutableStateOf(GroupOptionState())

    fun onGroupNameChange(newText: TextFieldValue) {
        newGroupState = GroupNameValidator.onGroupNameChange(newText, newGroupState)
    }

    fun onGroupOptionsErrorDismiss() {
        groupOptionsState = groupOptionsState.copy(error = null)
    }

    fun onAllowGuestStatusChanged(status: Boolean) {
        groupOptionsState = groupOptionsState.copy(isAllowGuestEnabled = status)
        if (!status) {
            groupOptionsState.accessRoleState.remove(Conversation.AccessRole.NON_TEAM_MEMBER)
            groupOptionsState.accessRoleState.remove(Conversation.AccessRole.GUEST)
        } else {
            groupOptionsState.accessRoleState.add(Conversation.AccessRole.NON_TEAM_MEMBER)
            groupOptionsState.accessRoleState.add(Conversation.AccessRole.GUEST)
        }
    }

    fun onAllowServicesStatusChanged(status: Boolean) {
        groupOptionsState = groupOptionsState.copy(isAllowServicesEnabled = status)
        if (!status) {
            groupOptionsState.accessRoleState.remove(Conversation.AccessRole.SERVICE)
        } else {
            groupOptionsState.accessRoleState.add(Conversation.AccessRole.SERVICE)
        }
    }

    fun onReadReceiptStatusChanged(status: Boolean) {
        groupOptionsState = groupOptionsState.copy(isReadReceiptEnabled = status)
    }

    fun onAllowGuestsDialogDismissed() {
        groupOptionsState = groupOptionsState.copy(showAllowGuestsDialog = false)
    }

    fun onAllowGuestsClicked() {
        onAllowGuestsDialogDismissed()
        onAllowGuestStatusChanged(true)
        createGroupWithCustomOptions(false)
    }

    fun onNotAllowGuestClicked() {
        onAllowGuestsDialogDismissed()
        onAllowGuestStatusChanged(false)
        removeGuestsIfNotAllowed()
        createGroupWithCustomOptions(false)
    }

    private fun removeGuestsIfNotAllowed() {
        if (!groupOptionsState.isAllowGuestEnabled) {
            for (item in state.contactsAddedToGroup) {
                if (item.membership == Membership.Guest ||
                    item.membership == Membership.Federated
                ) {
                    removeContactFromGroup(item)
                }
            }
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

    fun createGroup() {
        if (newGroupState.isSelfTeamMember) {
            createGroupWithCustomOptions(true)
        } else {
            createGroupWithoutOption()
        }
    }

    private fun createGroupWithoutOption() {
        viewModelScope.launch {
            newGroupState = newGroupState.copy(isLoading = true)
            val result = createGroupConversation(
                name = newGroupState.groupName.text,
                // TODO: change the id in Contact to UserId instead of String
                userIdList = state.contactsAddedToGroup.map { contact -> UserId(contact.id, contact.domain) },
                options = ConversationOptions().copy(
                    protocol = ConversationOptions.Protocol.PROTEUS,
                    readReceiptsEnabled = null,
                    accessRole = null
                )
            )
            handleNewGroupCreationResult(result)
        }
    }

    private fun createGroupWithCustomOptions(shouldCheckGuests: Boolean = true) {
        if (shouldCheckGuests && checkIfGuestAdded())
            return
        viewModelScope.launch {
            newGroupState = newGroupState.copy(isLoading = true)
            val result = createGroupConversation(
                name = newGroupState.groupName.text,
                // TODO: change the id in Contact to UserId instead of String
                userIdList = state.contactsAddedToGroup.map { contact -> UserId(contact.id, contact.domain) },
                options = ConversationOptions().copy(
                    protocol = newGroupState.groupProtocol,
                    readReceiptsEnabled = groupOptionsState.isReadReceiptEnabled,
                    accessRole = groupOptionsState.accessRoleState
                )
            )
            handleNewGroupCreationResult(result)
        }
    }

    private suspend fun handleNewGroupCreationResult(result: CreateGroupConversationUseCase.Result) {
        when (result) {
            is CreateGroupConversationUseCase.Result.Success -> {
                newGroupState = newGroupState.copy(isLoading = false)
                navigationManager.navigate(
                    command = NavigationCommand(
                        destination = NavigationItem.Conversation.getRouteWithArgs(listOf(result.conversation.id)),
                        backStackMode = BackStackMode.REMOVE_CURRENT
                    )
                )
            }

            CreateGroupConversationUseCase.Result.SyncFailure -> {
                appLogger.d("Can't create group due to SyncFailure")
                groupOptionsState = groupOptionsState.copy(isLoading = false, error = GroupOptionState.Error.LackingConnection)
            }

            is CreateGroupConversationUseCase.Result.UnknownFailure -> {
                appLogger.w("Error while creating a group ${result.cause}")
                groupOptionsState = groupOptionsState.copy(isLoading = false, error = GroupOptionState.Error.Unknown)
            }
        }
    }

    fun onGroupNameErrorAnimated() {
        newGroupState = GroupNameValidator.onGroupNameErrorAnimated(newGroupState)
    }
}
