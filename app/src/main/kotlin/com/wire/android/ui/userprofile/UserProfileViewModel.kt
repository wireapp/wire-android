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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dataStore: UserDataStore
) : ViewModel() {

    var userProfileState by mutableStateOf<UserProfileState>(
        UserProfileState(
            "",
            UserStatus.BUSY,
            "Tester Tost_long_long_long long  long  long  long  long  long ",
            "@userName_long_long_long_long_long_long_long_long_long_long",
            "Best team ever long  long  long  long  long  long  long  long  long ",
            listOf(
                OtherAccount("someId", "", "Other Name 0", "team A"),
//                OtherAccount("someId", "", "Other Name 1", "team B"),
//                OtherAccount("someId", "", "Other Name 2", "team C"),
//                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "New Name")
            )
        )
    )
        private set

    fun close() = viewModelScope.launch { navigationManager.navigateBack() }

    fun logout() {
        //TODO
        viewModelScope.launch {
            dataStore.clear() //TODO this should be moved to some service that will clear all the data in the app
            navigationManager.navigate(
                NavigationCommand(
                    NavigationItem.Authentication.route,
                    BackStackMode.CLEAR_WHOLE
                )
            )
        }
    }

    fun addAccount() {
        //TODO
    }

    fun editProfile() {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.Settings.route))
        }
    }

    fun dismissDialog() {
        userProfileState = userProfileState.copy(dialogState = DialogState.None)
    }

    fun changeStatus(status: UserStatus) {
        setNotShowStatusRationaleAgainIfNeeded(status)
        //TODO
        dismissDialog()
    }

    fun notShowStatusRationaleAgain(doNotShow: Boolean, status: UserStatus) {
        userProfileState.run {
            if (dialogState is DialogState.StatusInfo) {
                userProfileState = copy(dialogState = dialogState.changeCheckBoxState(doNotShow))
            }
        }
    }

    fun changeStatusClick(status: UserStatus) {
        if (userProfileState.status == status) return

        viewModelScope.launch {
            if (shouldShowStatusRationaleDialog(status)) {
                val dialogStatus = when (status) {
                    UserStatus.AVAILABLE -> DialogState.StatusInfo.StateAvailable()
                    UserStatus.BUSY -> DialogState.StatusInfo.StateBusy()
                    UserStatus.AWAY -> DialogState.StatusInfo.StateAway()
                    UserStatus.NONE -> DialogState.StatusInfo.StateNone()
                }
                userProfileState = userProfileState.copy(dialogState = dialogStatus)
            } else {
                changeStatus(status)
            }
        }
    }

    private fun setNotShowStatusRationaleAgainIfNeeded(status: UserStatus) {
        userProfileState.dialogState.let { dialogState ->
            if (dialogState is DialogState.StatusInfo && dialogState.isCheckBoxChecked) {
                viewModelScope.launch {
                    when (status) {
                        UserStatus.AVAILABLE -> dataStore.donNotShowStatusRationaleAvailable()
                        UserStatus.BUSY -> dataStore.donNotShowStatusRationaleBusy()
                        UserStatus.AWAY -> dataStore.donNotShowStatusRationaleAway()
                        UserStatus.NONE -> dataStore.donNotShowStatusRationaleNone()
                    }
                }
            }
        }
    }

    private suspend fun shouldShowStatusRationaleDialog(status: UserStatus): Boolean {
        return when (status) {
            UserStatus.AVAILABLE -> dataStore.shouldShowStatusRationaleAvailableFlow
            UserStatus.BUSY -> dataStore.shouldShowStatusRationaleBusyFlow
            UserStatus.AWAY -> dataStore.shouldShowStatusRationaleAwayFlow
            UserStatus.NONE -> dataStore.shouldShowStatusRationaleNoneFlow
        }.first()
    }
}
