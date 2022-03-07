package com.wire.android.ui.userprofile

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStore
import com.wire.android.model.UserStatus
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.getTempAvatarUri
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

// Suppress for now after removing mockMethodForAvatar it should not complain
@Suppress("TooManyFunctions", "MagicNumber")
@ExperimentalMaterial3Api
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dataStore: UserDataStore,
    private val uploadUserAvatar: UploadUserAvatarUseCase,
    private val getSelf: GetSelfUserUseCase
) : ViewModel() {

    var userProfileState by mutableStateOf(SelfUserProfileState())
        private set

    init {
        // TODO: here we should have a loading state as the first initial state of the screen
        viewModelScope.launch {
            fetchSelfUser()
            observeAvatarChanges()
        }
    }

    private suspend fun observeAvatarChanges() {
        dataStore.shouldUpdateAvatar.collect { shouldUpdate ->
            if (shouldUpdate) {
//                getCurrentAvatarUri(LocalContext.current).path
            }
        }
    }

    private suspend fun fetchSelfUser() {
        getSelf().collect {
            userProfileState = SelfUserProfileState(
                status = UserStatus.AVAILABLE,
                fullName = it.name.orEmpty(),
                userName = it.handle.orEmpty(),
                teamName = it.team,
                // Add some mocked team
                otherAccounts = listOf(
                    OtherAccount("someId", "", it.name.orEmpty(), "Wire Swiss GmbH"),
                    OtherAccount("someId", "", "B. A. Baracus", "The A-Team"),
                )
            )
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
        userProfileState = userProfileState.copy(errorMessage = null)
    }

    fun onChangeProfilePictureClicked(ctx: Context) {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.ProfileImagePicker.getRouteWithArgs()))
        }
    }

    fun changeAvatar() {
        userProfileState = userProfileState.copy(isAvatarLoading = true)
    }

    //!! TODO: this method is made only to pass the mock bitmap, later on we will not need it !!
    fun mockMethodForAvatar(bitmap: Bitmap) {
        userProfileState = userProfileState.copy(avatarBitmap = bitmap)
    }

}
