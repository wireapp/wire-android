package com.wire.android.ui.home.newconversation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.mapper.ContactMapper
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.search.SearchPeopleViewModel
import com.wire.android.ui.home.conversations.search.SearchResult
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.groupOptions.GroupOptionState
import com.wire.android.ui.home.newconversation.newgroup.NewGroupState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationOptions
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.CreateGroupConversationUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsResult
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.search.Result
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchUsersUseCase
import com.wire.kalium.logic.functional.Either
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewConversationViewModel @Inject constructor(
    private val getAllKnownUsers: GetAllContactsUseCase,
    private val searchKnownUsers: SearchKnownUsersUseCase,
    private val searchPublicUsers: SearchUsersUseCase,
    private val createGroupConversation: CreateGroupConversationUseCase,
    private val contactMapper: ContactMapper,
    dispatchers: DispatcherProvider,
    sendConnectionRequest: SendConnectionRequestUseCase,
    navigationManager: NavigationManager
) : SearchPeopleViewModel(navigationManager, sendConnectionRequest, dispatchers) {
    private companion object {
        const val GROUP_NAME_MAX_COUNT = 64
    }

    var groupNameState: NewGroupState by mutableStateOf(NewGroupState())

    var groupOptionsState: GroupOptionState by mutableStateOf(GroupOptionState())

    init {
        viewModelScope.launch { allContacts() }
    }

    override suspend fun getAllUsersUseCase() =
        when (val result = getAllKnownUsers()) {
            is GetAllContactsResult.Failure -> SearchResult.Failure(R.string.label_general_error)
            is GetAllContactsResult.Success -> SearchResult.Success(
                result.allContacts.map { otherUser ->
                    contactMapper.fromOtherUser(
                        otherUser
                    )
                }
            )
        }

    override suspend fun searchKnownUsersUseCase(searchTerm: String) =
        when (val result = searchKnownUsers(searchTerm)) {
            is Result.Failure.Generic, Result.Failure.InvalidRequest -> {
                SearchResult.Failure(R.string.label_general_error)
            }
            is Result.Failure.InvalidQuery -> {
                SearchResult.Failure(R.string.label_no_results_found)
            }
            is Result.Success -> {
                SearchResult.Success(result.userSearchResult.result.map { otherUser -> contactMapper.fromOtherUser(otherUser) })
            }
        }

    override suspend fun searchPublicUsersUseCase(searchTerm: String) =
        when (val result = searchPublicUsers(searchTerm)) {
            is Result.Failure.Generic, Result.Failure.InvalidRequest -> {
                SearchResult.Failure(R.string.label_general_error)
            }
            is Result.Failure.InvalidQuery -> {
                SearchResult.Failure(R.string.label_no_results_found)
            }
            is Result.Success -> {
                SearchResult.Success(result.userSearchResult.result.map { otherUser -> contactMapper.fromOtherUser(otherUser) })
            }
        }

    fun onGroupNameChange(newText: TextFieldValue) {
        when {
            newText.text.trim().isEmpty() -> {
                groupNameState = groupNameState.copy(
                    animatedGroupNameError = true,
                    groupName = newText,
                    continueEnabled = false,
                    error = NewGroupState.GroupNameError.TextFieldError.GroupNameEmptyError
                )
            }
            newText.text.trim().count() > GROUP_NAME_MAX_COUNT -> {
                groupNameState = groupNameState.copy(
                    animatedGroupNameError = true,
                    groupName = newText,
                    continueEnabled = false,
                    error = NewGroupState.GroupNameError.TextFieldError.GroupNameExceedLimitError
                )
            }
            else -> {
                groupNameState = groupNameState.copy(
                    animatedGroupNameError = false,
                    groupName = newText,
                    continueEnabled = true,
                    error = NewGroupState.GroupNameError.None
                )
            }
        }
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
            groupNameState = groupNameState.copy(isLoading = true)

            when (val result = createGroupConversation(
                name = groupNameState.groupName.text,
                // TODO: change the id in Contact to UserId instead of String
                userIdList = state.contactsAddedToGroup.map { contact -> UserId(contact.id, contact.domain) },
                options = ConversationOptions().copy(
                    protocol = groupNameState.groupProtocol,
                    readReceiptsEnabled = groupOptionsState.isReadReceiptEnabled,
                    accessRole = groupOptionsState.accessRoleState
                )
            )
            ) {
                // TODO: handle the error state
                is Either.Left -> {
                    groupNameState = groupNameState.copy(isLoading = false)
                    Log.d("TEST", "error while creating a group ${result.value}")
                }
                is Either.Right -> {
                    groupNameState = groupNameState.copy(isLoading = false)
                    navigationManager.navigate(
                        command = NavigationCommand(
                            destination = NavigationItem.Conversation.getRouteWithArgs(listOf(result.value.id)),
                            backStackMode = BackStackMode.REMOVE_CURRENT
                        )
                    )
                }
            }
        }
    }

    fun onGroupNameErrorAnimated() {
        groupNameState = groupNameState.copy(animatedGroupNameError = false)
    }

}
