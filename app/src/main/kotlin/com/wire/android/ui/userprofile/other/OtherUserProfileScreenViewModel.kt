package com.wire.android.ui.userprofile.other

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.EXTRA_CONNECTED_STATUS
import com.wire.android.navigation.EXTRA_USER_DOMAIN
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.CreateConversationResult
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.publicuser.GetKnownUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OtherUserProfileScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
    private val getKnownUser: GetKnownUserUseCase,
    private val sendConnectionRequest: SendConnectionRequestUseCase
) : ViewModel() {

    var state: OtherUserProfileState by mutableStateOf(OtherUserProfileState())

    private val userId = UserId(
        value = savedStateHandle.get<String>(EXTRA_USER_ID)!!,
        domain = savedStateHandle.get<String>(EXTRA_USER_DOMAIN)!!
    )

    init {
        val internalStatus = savedStateHandle.get<String>(EXTRA_CONNECTED_STATUS)
        when (val connectedStatus = getConnectedStatus(internalStatus)) {
            is ConnectionStatus.Connected -> {
            }
            else -> {
                state = state.copy(
                    isAvatarLoading = false,
                    fullName = "Kim",
                    userName = "Dawson",
                    teamName = "AWESOME TEAM NAME",
                    email = "kim.dawson@gmail.com",
                    phone = "+49 123 456 000",
                    connectionStatus = connectedStatus
                )
            }
        }
        state = state.copy(isDataLoading = true)
        viewModelScope.launch {
            getKnownUser(userId).collect { otherUser ->
                otherUser?.let {
                    state = state.copy(
                        userAvatarAsset = it.completePicture?.let { pic -> ImageAsset.UserAvatarAsset(pic) },
                        isDataLoading = false,
                        fullName = it.name ?: String.EMPTY,
                        userName = it.handle ?: String.EMPTY,
                        teamName = it.team ?: String.EMPTY,
                        email = it.email ?: String.EMPTY,
                        phone = it.phone ?: String.EMPTY,
                        connectionStatus = it.connectionStatus.toOtherUserProfileConnectionStatus()
                    )
                } ?: run {
                    appLogger.d("Couldn't not find the user with provided id:$userId.id and domain:$userId.domain")
                }
            }
        }
    }

    private fun getConnectedStatus(internalStatus: String?): ConnectionStatus {
        val status = internalStatus?.let {
            ConnectionState.values()[it.toInt()]
        }
        return status?.toOtherUserProfileConnectionStatus() ?: ConnectionStatus.Unknown
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
                is SendConnectionRequestResult.Success -> state = state.copy(connectionStatus = ConnectionStatus.NotConnected(true))
            }
        }
    }

    fun cancelConnectionRequest() {
        // TODO: fire a use case
        state = state.copy(connectionStatus = ConnectionStatus.NotConnected(false))
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }
}
