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
import com.wire.android.ui.home.conversations.search.SearchAllUsersViewModel
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.groupOptions.GroupOptionState
import com.wire.android.ui.home.newconversation.newgroup.NewGroupState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationOptions
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.CreateGroupConversationUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchUsersUseCase
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class NewConversationViewModel @Inject constructor(
    private val createGroupConversation: CreateGroupConversationUseCase,
    getAllKnownUsers: GetAllContactsUseCase,
    searchKnownUsers: SearchKnownUsersUseCase,
    searchPublicUsers: SearchUsersUseCase,
    contactMapper: ContactMapper,
    isMLSEnabled: IsMLSEnabledUseCase,
    dispatchers: DispatcherProvider,
    sendConnectionRequest: SendConnectionRequestUseCase,
    navigationManager: NavigationManager
) : SearchAllUsersViewModel(
    getAllKnownUsers = getAllKnownUsers,
    sendConnectionRequest = sendConnectionRequest,
    searchKnownUsers = searchKnownUsers,
    searchPublicUsers = searchPublicUsers,
    contactMapper = contactMapper,
    dispatcher = dispatchers,
    navigationManager = navigationManager
) {
    private companion object {
        const val GROUP_NAME_MAX_COUNT = 64
    }

    var newGroupState: NewGroupState by mutableStateOf(NewGroupState(mlsEnabled = isMLSEnabled()))

    var groupOptionsState: GroupOptionState by mutableStateOf(GroupOptionState())

//    init {
//        viewModelScope.launch { initialContacts() }
//    }

    fun onGroupNameChange(newText: TextFieldValue) {
        when {
            newText.text.trim().isEmpty() -> {
                newGroupState = newGroupState.copy(
                    animatedGroupNameError = true,
                    groupName = newText,
                    continueEnabled = false,
                    error = NewGroupState.NewGroupError.TextFieldError.GroupNameEmptyError
                )
            }
            newText.text.trim().count() > GROUP_NAME_MAX_COUNT -> {
                newGroupState = newGroupState.copy(
                    animatedGroupNameError = true,
                    groupName = newText,
                    continueEnabled = false,
                    error = NewGroupState.NewGroupError.TextFieldError.GroupNameExceedLimitError
                )
            }
            else -> {
                newGroupState = newGroupState.copy(
                    animatedGroupNameError = false,
                    groupName = newText,
                    continueEnabled = true,
                    error = NewGroupState.NewGroupError.None
                )
            }
        }
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
        createGroup(false)
    }

    fun onNotAllowGuestClicked() {
        onAllowGuestsDialogDismissed()
        onAllowGuestStatusChanged(false)
        removeGuestsIfNotAllowed()
        createGroup(false)
    }

    private fun removeGuestsIfNotAllowed() {
        if (!groupOptionsState.isAllowGuestEnabled) {
            for (item in state.contactsAddedToGroup) {
                if (item.membership == Membership.Guest
                    || item.membership == Membership.Federated
                ) {
                    removeContactFromGroup(item)
                }
            }
        }
    }

    private fun checkIfGuestAdded(): Boolean {
        if (!groupOptionsState.isAllowGuestEnabled) {
            for (item in state.contactsAddedToGroup) {
                if (item.membership == Membership.Guest
                    || item.membership == Membership.Federated
                ) {
                    groupOptionsState = groupOptionsState.copy(showAllowGuestsDialog = true)
                    return true
                }
            }
        }
        return false
    }

    fun createGroup(shouldCheckGuests: Boolean = true) {
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
        newGroupState = newGroupState.copy(animatedGroupNameError = false)
    }

}
