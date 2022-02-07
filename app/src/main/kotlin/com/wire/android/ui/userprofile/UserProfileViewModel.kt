package com.wire.android.ui.userprofile

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.model.UserStatus
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val navigationManager: NavigationManager
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

    fun onDismissDialog() {
        userProfileState = userProfileState.copy(dialogState = DialogState.None)
    }

    fun changeStatus(status: UserStatus) {
        //TODO
        onDismissDialog()
    }

    fun doNotShowStatusDialogCheckChanged(doNotShow: Boolean) {
        userProfileState.run {
            if (dialogState is DialogState.StatusChange) {
                userProfileState = copy(dialogState = dialogState.changeCheckBoxState(doNotShow))
            }
        }
    }

    fun changeStatusClick(status: UserStatus) {
        if (userProfileState.status == status) return

//        if (!shouldShowDialog) {
//            changeStatus(status)
//        } else {
        val dialogStatus = when (status) {
            UserStatus.AVAILABLE -> DialogState.StatusChange.StateAvailable()
            UserStatus.BUSY -> DialogState.StatusChange.StateBusy()
            UserStatus.AWAY -> DialogState.StatusChange.StateAway()
            UserStatus.NONE -> DialogState.StatusChange.StateNone()
        }
        userProfileState = userProfileState.copy(dialogState = dialogStatus)
//        }
    }
}
