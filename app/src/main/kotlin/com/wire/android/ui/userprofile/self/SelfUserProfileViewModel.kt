package com.wire.android.ui.userprofile.self

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStore
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.userprofile.self.dialog.StatusDialogData
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UpdateSelfAvailabilityStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// Suppress for now after removing mockMethodForAvatar it should not complain
@Suppress("TooManyFunctions", "LongParameterList")
@ExperimentalMaterial3Api
@HiltViewModel
class SelfUserProfileViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dataStore: UserDataStore,
    private val getSelf: GetSelfUserUseCase,
    private val getSelfTeam: GetSelfTeamUseCase,
    private val updateStatus: UpdateSelfAvailabilityStatusUseCase,
    private val logout: LogoutUseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    var userProfileState by mutableStateOf(SelfUserProfileState())
        private set

    init {
        viewModelScope.launch {
            fetchSelfUser()
        }
    }

    private suspend fun fetchSelfUser() {
        viewModelScope.launch {
            getSelf().combine(getSelfTeam(), ::Pair)
                .collect { (selfUser, selfTeam) ->
                    with(selfUser) {
                        // Load user avatar raw image data
                        completePicture?.let { updateUserAvatar(it) }

                        // Update user data state
                        userProfileState = SelfUserProfileState(
                            status = availabilityStatus,
                            fullName = name.orEmpty(),
                            userName = handle.orEmpty(),
                            teamName = selfTeam?.name,
                            otherAccounts = listOf() //TODO: implement other accounts functionality
                        )
                    }
                }

        }
    }

    private fun showErrorMessage() {
        userProfileState = userProfileState.copy(errorMessageCode = ErrorCodes.DownloadUserInfoError)
    }

    private fun showLoadingAvatar(show: Boolean) {
        userProfileState = userProfileState.copy(isAvatarLoading = show)
    }

    private fun updateUserAvatar(avatarAssetId: UserAssetId) {
        // We try to download the user avatar on a separate thread so that we don't block the display of the user's info
        viewModelScope.launch {
            showLoadingAvatar(true)
            withContext(dispatchers.io()) {
                try {
                    userProfileState = userProfileState.copy(
                        avatarAsset = UserAvatarAsset(avatarAssetId)
                    )

                    // Update avatar asset id on user data store
                    // TODO: obtain the asset id through a useCase once we also store assets ids
                    dataStore.updateUserAvatarAssetId(avatarAssetId.toString())
                } catch (e: ClassCastException) {
                    appLogger.e("There was an error while downloading the user avatar", e)
                    // Show error snackbar if avatar download fails
                    showErrorMessage()
                }
            }
            showLoadingAvatar(false)
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

    fun onLogoutClick() {
        viewModelScope.launch {
            logout()
            dataStore.clear() // TODO this should be moved to some service that will clear all the data in the app
            navigationManager.navigate(
                NavigationCommand(
                    NavigationItem.Welcome.getRouteWithArgs(),
                    BackStackMode.CLEAR_WHOLE
                )
            )
        }
    }

    fun addAccount() {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.CreatePersonalAccount.getRouteWithArgs()))
        }
    }

    fun editProfile() {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.Settings.getRouteWithArgs()))
        }
    }

    fun dismissStatusDialog() {
        userProfileState = userProfileState.copy(statusDialogData = null)
    }

    fun changeStatus(status: UserAvailabilityStatus) {
        setNotShowStatusRationaleAgainIfNeeded(status)
        //TODO add the broadcast message to inform everyone about the self user new status
        viewModelScope.launch { updateStatus(status) }
        dismissStatusDialog()
    }

    fun dialogCheckBoxStateChanged(isChecked: Boolean) {
        userProfileState.run {
            userProfileState = copy(statusDialogData = statusDialogData?.changeCheckBoxState(isChecked))
        }
    }

    fun changeStatusClick(status: UserAvailabilityStatus) {
        if (userProfileState.status == status) return

        viewModelScope.launch {
            if (shouldShowStatusRationaleDialog(status)) {
                val statusDialogInfo = when (status) {
                    UserAvailabilityStatus.AVAILABLE -> StatusDialogData.StateAvailable()
                    UserAvailabilityStatus.BUSY -> StatusDialogData.StateBusy()
                    UserAvailabilityStatus.AWAY -> StatusDialogData.StateAway()
                    UserAvailabilityStatus.NONE -> StatusDialogData.StateNone()
                }
                userProfileState = userProfileState.copy(statusDialogData = statusDialogInfo)
            } else {
                changeStatus(status)
            }
        }
    }

    private fun setNotShowStatusRationaleAgainIfNeeded(status: UserAvailabilityStatus) {
        userProfileState.statusDialogData.let { dialogState ->
            if (dialogState?.isCheckBoxChecked == true) {
                viewModelScope.launch { dataStore.dontShowStatusRationaleAgain(status) }
            }
        }
    }

    private suspend fun shouldShowStatusRationaleDialog(status: UserAvailabilityStatus): Boolean =
        dataStore.shouldShowStatusRationaleFlow(status).first()

    fun clearErrorMessage() {
        userProfileState = userProfileState.copy(errorMessageCode = null)
    }

    fun onChangeProfilePictureClicked() {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.ProfileImagePicker.getRouteWithArgs()))
        }
    }

    sealed class ErrorCodes {
        object DownloadUserInfoError : ErrorCodes()
    }
}
