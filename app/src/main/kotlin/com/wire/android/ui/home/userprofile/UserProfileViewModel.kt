package com.wire.android.ui.home.userprofile

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.common.UserStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class UserProfileViewModel @Inject constructor(private val navigationManager: NavigationManager) : ViewModel() {

    private val _state = MutableStateFlow<UserProfileState>(
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

    val state: StateFlow<UserProfileState>
        get() = _state

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
        //TODO
    }
}
