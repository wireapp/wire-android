package com.wire.android.ui.userprofile.other

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.EXTRA_USER_DOMAIN
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.CreateConversationResult
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.GetUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OtherUserProfileScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
    private val getUserInfo: GetUserInfoUseCase,
    private val sendConnectionRequest: SendConnectionRequestUseCase
) : ViewModel() {

    var state: OtherUserProfileState by mutableStateOf(OtherUserProfileState())

    private val userId = UserId(
        value = savedStateHandle.get<String>(EXTRA_USER_ID)!!,
        domain = savedStateHandle.get<String>(EXTRA_USER_DOMAIN)!!
    )

    init {
        state = state.copy(isDataLoading = true)
        viewModelScope.launch {
            when (val result = getUserInfo(userId)) {
                is GetUserInfoResult.Failure ->
                    appLogger.d("Couldn't not find the user with provided id:$userId.id and domain:$userId.domain")
                is GetUserInfoResult.Success -> loadViewState(result.otherUser)
            }
        }
    }

    private fun loadViewState(otherUser: OtherUser) {
        state = state.copy(
            isDataLoading = false,
            userAvatarAsset = otherUser.completePicture?.let { pic -> ImageAsset.UserAvatarAsset(pic) },
            fullName = otherUser.name ?: String.EMPTY,
            userName = otherUser.handle ?: String.EMPTY,
            teamName = otherUser.team ?: String.EMPTY,
            email = otherUser.email ?: String.EMPTY,
            phone = otherUser.phone ?: String.EMPTY,
            connectionStatus = otherUser.connectionStatus.toOtherUserProfileConnectionStatus()
        )
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
        viewModelScope.launch {
            when (sendConnectionRequest(userId)) {
                is SendConnectionRequestResult.Failure -> appLogger.d(("Couldn't send a connect request to user $userId"))
                is SendConnectionRequestResult.Success -> {
                    state = state.copy(connectionStatus = ConnectionStatus.NotConnected(true))
                    navigateBack()
                }
            }
        }
    }

    fun cancelConnectionRequest() {
        // TODO: fire a use case
        state = state.copy(connectionStatus = ConnectionStatus.NotConnected(false))
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }
}
