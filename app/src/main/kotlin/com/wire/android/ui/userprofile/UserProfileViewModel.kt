package com.wire.android.ui.userprofile

import android.graphics.Bitmap
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
import com.wire.android.util.extension.toByteArray
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import com.wire.kalium.logic.functional.Either
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
    private val uploadUserAvatar: UploadUserAvatarUseCase,
    private val getSelf: GetSelfUserUseCase
) : ViewModel() {

    var userProfileState by mutableStateOf(SelfUserProfileState())
        private set

    init {
        // TODO: here we should have a loading state as the first initial state of the screen
        viewModelScope.launch {
            getSelf().collect {
                userProfileState = SelfUserProfileState(
                    avatarBitmap = null,
                    status = UserStatus.AVAILABLE,
                    fullName = it.name!!,
                    userName = it.handle!!,
                    teamName = it.team
                )
            }
        }
    }

    fun close() = viewModelScope.launch { navigationManager.navigateBack() }

    fun logout() {
        // TODO
        viewModelScope.launch {
            dataStore.clear() // TODO this should be moved to some service that will clear all the data in the app
            navigationManager.navigate(
                NavigationCommand(
                    NavigationItem.Authentication.route,
                    BackStackMode.CLEAR_WHOLE
                )
            )
        }
    }

    fun addAccount() {
        // TODO
    }

    fun editProfile() {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.Settings.route))
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

    fun changeUserProfile(avatarBitmap: Bitmap) {
        val backupBitmap = userProfileState.avatarBitmap

        changeUserProfile(avatarBitmap, onFailure = {
            userProfileState = userProfileState.copy(
                avatarBitmap = backupBitmap,
                isAvatarLoading = false,
                errorMessage = "Image could not be uploaded"
            )
        })
    }

    private fun changeUserProfile(avatarBitmap: Bitmap, onFailure: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                userProfileState = userProfileState.copy(avatarBitmap = avatarBitmap, isAvatarLoading = true)

                when (uploadUserAvatar("image/png", avatarBitmap.toByteArray())) {
                    is Either.Left -> onFailure()
                    is Either.Right -> userProfileState = userProfileState.copy(isAvatarLoading = false)
                }
            }
        }
    }

    fun clearErrorMessage() {
        userProfileState = userProfileState.copy(
            errorMessage = null
        )
    }
}
