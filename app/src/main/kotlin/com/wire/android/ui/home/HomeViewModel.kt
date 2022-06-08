package com.wire.android.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.featureConfig.GetAndSaveFileSharingStatusUseCase
import com.wire.kalium.logic.feature.featureConfig.GetFileSharingStatusResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.sync.ListenToEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val listenToEvents: ListenToEventsUseCase,
    private val incomingCalls: GetIncomingCallsUseCase,
    private val getSelf: GetSelfUserUseCase,
    private val needsToRegisterClient: NeedsToRegisterClientUseCase,
    private val getAndSaveFileSharingStatusUseCase: GetAndSaveFileSharingStatusUseCase
) : ViewModel() {

    var userAvatar by mutableStateOf<UserAvatarAsset?>(null)
        private set

    init {
        viewModelScope.launch {
            launch { listenToEvents() } // listen for the WebSockets updates and update DB accordingly
            launch { loadUserAvatar() }
            launch {
                incomingCalls().collect { observeIncomingCalls(calls = it) }
            }
        }
        getAndSaveFileSharingConfig()
    }

    private fun getAndSaveFileSharingConfig() {
        viewModelScope.launch {
            getAndSaveFileSharingStatusUseCase().let {
                when (it) {
                    is GetFileSharingStatusResult.Failure.NoTeam -> {}
                    is GetFileSharingStatusResult.Failure.Generic -> {}
                    is GetFileSharingStatusResult.Failure.OperationDenied -> {}
                    is GetFileSharingStatusResult.Success -> { // todo update the UI with dialog }
                    }
                }
            }
        }
    }

    fun checkRequirements() {
        viewModelScope.launch {
            when {
                needsToRegisterClient() -> { // check if the client has been registered and open the proper screen if not
                    navigationManager.navigate(
                        NavigationCommand(
                            NavigationItem.RegisterDevice.getRouteWithArgs(),
                            BackStackMode.CLEAR_WHOLE
                        )
                    )
                    return@launch
                }
                getSelf().first().handle.isNullOrEmpty() -> { // check if the user handle has been set and open the proper screen if not
                    navigationManager.navigate(
                        NavigationCommand(
                            NavigationItem.CreateUsername.getRouteWithArgs(),
                            BackStackMode.CLEAR_WHOLE
                        )
                    )
                    return@launch
                }
            }
        }
    }

    private suspend fun loadUserAvatar() {
        viewModelScope.launch {
            getSelf().collect { selfUser ->
                userAvatar = selfUser.previewPicture?.let { UserAvatarAsset(it) }
            }
        }
    }

    private suspend fun observeIncomingCalls(calls: List<Call>) {
        if (calls.isNotEmpty()) {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.IncomingCall.getRouteWithArgs(listOf(calls.first().conversationId))
                )
            )
        }
    }

    suspend fun navigateTo(item: NavigationItem, extraRouteId: String = "") {
        navigationManager.navigate(NavigationCommand(destination = item.getRouteWithArgs(listOf(extraRouteId))))
    }

    fun navigateToUserProfile() = viewModelScope.launch { navigateTo(NavigationItem.SelfUserProfile, MY_USER_PROFILE_SUBROUTE) }

    companion object {
        const val MY_USER_PROFILE_SUBROUTE = "myUserProfile"
    }
}
