package com.wire.android.ui.userprofile.other

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.navigation.EXTRA_CONNECTED_STATUS
import com.wire.android.navigation.EXTRA_USER_DOMAIN
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.CreateConversationResult
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.publicuser.GetKnownUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtherUserProfileScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
    private val getKnownUserUseCase: GetKnownUserUseCase
) : ViewModel() {

    var state: OtherUserProfileState by mutableStateOf(OtherUserProfileState())

    private val userId = UserId(
        value = savedStateHandle.get<String>(EXTRA_USER_ID)!!,
        domain = savedStateHandle.get<String>(EXTRA_USER_DOMAIN)!!
    )

    init {
        //TODO: internal is here untill we can get the ConnectionStatus from the user
        // for now it is just to be able to proceed forward
        val internalStatus = savedStateHandle.get<String>(EXTRA_CONNECTED_STATUS)

        val isAKnownUser = internalStatus == "true"

        if (isAKnownUser) {
            state = state.copy(isDataLoading = true)

            viewModelScope.launch {
                getKnownUserUseCase(userId).collect { otherUser ->
                    otherUser?.let {
                        state = state.copy(
                            isDataLoading = false,
                            fullName = it.name ?: String.EMPTY,
                            userName = it.handle ?: String.EMPTY,
                            teamName = it.team ?: String.EMPTY,
                            email = it.email ?: String.EMPTY,
                            phone = it.phone ?: String.EMPTY,
                            connectionStatus = ConnectionStatus.Connected
                        )
                    } ?: run {
                        appLogger.d("Couldn't not find the user with provided id:$userId.id and domain:$userId.domain")
                    }
                }
            }
        } else {
            //TODO: for now this is a mock data when we open a screen for a not connected user
            // we need to retrieve it from the back-end ? and not the local source ?
            state = state.copy(
                isAvatarLoading = false,
                fullName = "Kim",
                userName = "Dawson",
                teamName = "AWESOME TEAM NAME",
                email = "kim.dawson@gmail.com",
                phone = "+49 123 456 000",
                connectionStatus = if (isAKnownUser) ConnectionStatus.Connected else ConnectionStatus.NotConnected(false)
            )
        }
    }

    fun openConversation() {
        viewModelScope.launch {
            when (val result = getOrCreateOneToOneConversation(userId)) {
                is CreateConversationResult.Failure -> appLogger.d(("Couldn't retrieve or create the conversation"))

                is CreateConversationResult.Success -> viewModelScope.launch {
                    navigationManager.navigate(
                        command = NavigationCommand(
                            destination = NavigationItem.Conversation.getRouteWithArgs(listOf(result.conversationId.id))
                        )
                    )
                }
            }
        }
    }

    fun sendConnectionRequest() {
        //TODO: fire a use case
        state = state.copy(connectionStatus = ConnectionStatus.NotConnected(true))
    }

    fun cancelConnectionRequest() {
        //TODO: fire a use case
        state = state.copy(connectionStatus = ConnectionStatus.NotConnected(false))
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

}
