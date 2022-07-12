package com.wire.android.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_CONNECTION_IGNORED_USER_NAME
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.navigation.getBackNavArg
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ExperimentalMaterial3Api
@HiltViewModel
class HomeViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val getSelf: GetSelfUserUseCase,
    private val needsToRegisterClient: NeedsToRegisterClientUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader
) : SavedStateViewModel(savedStateHandle) {

    var snackbarMessageState by mutableStateOf<HomeSnackbarState>(HomeSnackbarState.None)

    var userAvatar by mutableStateOf(SelfUserData())
        private set

    init {
        viewModelScope.launch {
            launch { loadUserAvatar() }
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

    fun checkPendingActions() {
        val connectionIgnoredUsername = savedStateHandle
            .getBackNavArg<String>(EXTRA_CONNECTION_IGNORED_USER_NAME)
        snackbarMessageState =
            connectionIgnoredUsername?.let { HomeSnackbarState.SuccessConnectionIgnoreRequest(it) } ?: HomeSnackbarState.None
    }

    fun clearSnackbarMessage() {
        snackbarMessageState = HomeSnackbarState.None
    }

    private suspend fun loadUserAvatar() {
        viewModelScope.launch {
            getSelf().collect { selfUser ->
                userAvatar = SelfUserData(
                    selfUser.previewPicture?.let { UserAvatarAsset(wireSessionImageLoader, it) },
                    selfUser.availabilityStatus
                )
            }
        }
    }

    suspend fun navigateTo(item: NavigationItem) { navigationManager.navigate(NavigationCommand(destination = item.getRouteWithArgs())) }

    fun navigateToUserProfile() = viewModelScope.launch { navigateTo(NavigationItem.SelfUserProfile) }
}

data class SelfUserData(
    val avatarAsset: UserAvatarAsset? = null,
    val status: UserAvailabilityStatus = UserAvailabilityStatus.NONE
)

sealed class HomeSnackbarState {
    class SuccessConnectionIgnoreRequest(val userName: String) : HomeSnackbarState()
    object None : HomeSnackbarState()
}
