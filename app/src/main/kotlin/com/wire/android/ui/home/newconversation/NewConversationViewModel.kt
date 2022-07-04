package com.wire.android.ui.home.newconversation

import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.mapper.ContactMapper
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.home.newconversation.newgroup.NewGroupState
import com.wire.android.ui.home.newconversation.search.ContactSearchResult
import com.wire.android.ui.home.newconversation.search.SearchPeopleState
import com.wire.android.ui.home.newconversation.search.SearchResultState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.flow.SearchQueryStateFlow
import com.wire.kalium.logic.data.conversation.ConversationOptions
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.CreateGroupConversationUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsResult
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.Result
import com.wire.kalium.logic.feature.publicuser.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.SearchUsersUseCase
import com.wire.kalium.logic.functional.Either
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class NewConversationViewModel
@Inject constructor(
    private val createGroupConversation: CreateGroupConversationUseCase,
    navigationManager: NavigationManager,
    searchUsers: SearchUsersUseCase,
    searchKnownUsers: SearchKnownUsersUseCase,
    getAllContacts: GetAllContactsUseCase,
    contactMapper: ContactMapper,
    dispatchers: DispatcherProvider
) : CreateConversationViewModel() {
    private companion object {
        const val GROUP_NAME_MAX_COUNT = 64
    }

    var groupNameState: NewGroupState by mutableStateOf(NewGroupState())

    fun onGroupNameErrorAnimated() {
        groupNameState = groupNameState.copy(animatedGroupNameError = false)
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

    fun createGroup() {
        viewModelScope.launch {
            groupNameState = groupNameState.copy(isLoading = true)

            when (val result = createGroupConversation(
                name = groupNameState.groupName.text,
                members = state.contactsAddedToGroup.map { contact -> contact.toMember() },
                options = ConversationOptions().copy(protocol = groupNameState.groupProtocol)
            )
            ) {
                // TODO: handle the error state
                is Either.Left -> {
                    groupNameState = groupNameState.copy(isLoading = false)
                    Log.d("TEST", "error while creating a group ${result.value}")
                }
                is Either.Right -> {
                    close()
                    groupNameState = groupNameState.copy(isLoading = false)
                    navigationManager.navigate(
                        command = NavigationCommand(
                            destination = NavigationItem.Conversation.getRouteWithArgs(listOf(result.value.id))
                        )
                    )
                }
            }
        }
    }

}


class AddConversationMembersViewModel
@Inject constructor(dispatchers: DispatcherProvider) : CreateConversationViewModel(dispatchers) {

}









