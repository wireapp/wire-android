package com.wire.android.ui.userprofile

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStore
import com.wire.android.model.UserStatus
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.feature.asset.GetPublicAssetUseCase
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// Suppress for now after removing mockMethodForAvatar it should not complain
@Suppress("TooManyFunctions", "MagicNumber")
@ExperimentalMaterial3Api
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dataStore: UserDataStore,
    private val getPublicAsset: GetPublicAssetUseCase,
    private val getSelf: GetSelfUserUseCase
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
            getSelf().collect { selfUser ->
                with(selfUser) {
                    // Load user avatar raw image data
                    completePicture?.let { updateUserAvatar(it) }

                    // Update user data state
                    userProfileState = SelfUserProfileState(
                        status = UserStatus.AVAILABLE,
                        fullName = name.orEmpty(),
                        userName = handle.orEmpty(),
                        teamName = team,

                        // TODO: remove mocked team when other accounts functionality is ready
                        otherAccounts = listOf(
                            OtherAccount("someId", "", name.orEmpty(), "Wire Swiss GmbH"),
                            OtherAccount("someId", "", "B. A. Baracus", "The A-Team"),
                        )
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
            withContext(Dispatchers.IO) {
                try {
                    showLoadingAvatar(true)
                    userProfileState = userProfileState.copy(
                        avatarAssetByteArray = (getPublicAsset(avatarAssetId) as PublicAssetResult.Success).asset
                    )

                    // Update avatar asset id on user data store
                    // TODO: obtain the asset id through a useCase once we also store assets ids
                    dataStore.updateUserAvatarAssetId(avatarAssetId)
                } catch (e: ClassCastException) {
                    // Show error snackbar if avatar download fails
                    showErrorMessage()
                } finally {
                    showLoadingAvatar(false)
                }
            }
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

    fun logout() {
        // TODO
        viewModelScope.launch {
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

    fun changeStatus(status: UserStatus) {
        setNotShowStatusRationaleAgainIfNeeded(status)
        userProfileState = userProfileState.copy(status = status)
        dismissStatusDialog()
    }

    fun dialogCheckBoxStateChanged(isChecked: Boolean) {
        userProfileState.run {
            userProfileState = copy(statusDialogData = statusDialogData?.changeCheckBoxState(isChecked))
        }
    }

    fun changeStatusClick(status: UserStatus) {
        if (userProfileState.status == status) return

        viewModelScope.launch {
            if (shouldShowStatusRationaleDialog(status)) {
                val statusDialogInfo = when (status) {
                    UserStatus.AVAILABLE -> StatusDialogData.StateAvailable()
                    UserStatus.BUSY -> StatusDialogData.StateBusy()
                    UserStatus.AWAY -> StatusDialogData.StateAway()
                    UserStatus.NONE -> StatusDialogData.StateNone()
                }
                userProfileState = userProfileState.copy(statusDialogData = statusDialogInfo)
            } else {
                changeStatus(status)
            }
        }
    }

    private fun setNotShowStatusRationaleAgainIfNeeded(status: UserStatus) {
        userProfileState.statusDialogData.let { dialogState ->
            if (dialogState?.isCheckBoxChecked == true) {
                viewModelScope.launch { dataStore.dontShowStatusRationaleAgain(status) }
            }
        }
    }

    private suspend fun shouldShowStatusRationaleDialog(status: UserStatus): Boolean =
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
        object DownloadUserInfoError: ErrorCodes()
    }
}
