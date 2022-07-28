package com.wire.android.ui.authentication.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.VoyagerNavigationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
) : ViewModel() {

    fun navigateBack() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

    fun goToLogin() {
        navigate(NavigationCommand(VoyagerNavigationItem.Login()))
    }

    fun goToCreateEnterpriseAccount() {
        navigate(NavigationCommand(VoyagerNavigationItem.CreateTeam))
    }

    fun goToCreatePrivateAccount() {
        navigate(NavigationCommand(VoyagerNavigationItem.CreatePersonalAccount))
    }
    private fun navigate(navigationCommand: NavigationCommand) {
        viewModelScope.launch {
            navigationManager.navigate(navigationCommand)
        }
    }
}
