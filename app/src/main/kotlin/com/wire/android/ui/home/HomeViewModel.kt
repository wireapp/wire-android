package com.wire.android.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.*
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.featureConfig.GetFeatureConfigStatusResult
import com.wire.kalium.logic.feature.featureConfig.GetRemoteFeatureConfigStatusAndPersistUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
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
    private val getRemoteFeatureConfigStatusAndPersist: GetRemoteFeatureConfigStatusAndPersistUseCase,
    private val isFileSharingEnabled: IsFileSharingEnabledUseCase
) : SavedStateViewModel(savedStateHandle) {

    var snackbarMessageState by mutableStateOf<HomeSnackbarState>(HomeSnackbarState.None)

    var homeState by mutableStateOf(HomeState())
        private set

    var userAvatar by mutableStateOf(SelfUserData())
        private set

    init {
        viewModelScope.launch {
            launch { loadUserAvatar() }
            getAndSaveFileSharingConfig()
        }
    }

    private suspend fun getAndSaveFileSharingConfig() {
        getRemoteFeatureConfigStatusAndPersist().let {
            when (it) {
                is GetFeatureConfigStatusResult.Failure.NoTeam -> {
                    appLogger.i("this user doesn't belong to a team")
                }
                is GetFeatureConfigStatusResult.Failure.Generic -> {
                    appLogger.d("${it.failure}")
                }
                is GetFeatureConfigStatusResult.Failure.OperationDenied -> {
                    appLogger.d("operation denied due to insufficient permissions")
                }
                is GetFeatureConfigStatusResult.Success -> {
                    setFileSharingStatus()
                    if (it.isStatusChanged) {
                        homeState = homeState.copy(showFileSharingDialog = true)
                    }
                }
            }
        }
    }

    private fun setFileSharingStatus() {
        viewModelScope.launch {
            homeState = homeState.copy(isFileSharingEnabledState = isFileSharingEnabled())
        }
    }

    fun hideDialogStatus() {
        homeState = homeState.copy(showFileSharingDialog = false)
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
                userAvatar = SelfUserData(selfUser.previewPicture?.let { UserAvatarAsset(it) }, selfUser.availabilityStatus)
            }
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

data class SelfUserData(
    val avatarAsset: UserAvatarAsset? = null,
    val status: UserAvailabilityStatus = UserAvailabilityStatus.NONE
)

sealed class HomeSnackbarState {
    class SuccessConnectionIgnoreRequest(val userName: String) : HomeSnackbarState()
    object None : HomeSnackbarState()
}
