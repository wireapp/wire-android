package com.wire.android.ui.authentication.welcome

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val authServerConfigProvider: AuthServerConfigProvider,
    private val getSessions: GetSessionsUseCase
) : ViewModel() {

    var state by mutableStateOf(authServerConfigProvider.authServer.value)
        private set

    var isThereActiveSession by mutableStateOf(false)


    init {
        observerAuthServer()
        checkNumberOfSessions()
    }

    private fun observerAuthServer() {
        viewModelScope.launch {
            authServerConfigProvider.authServer.collect {
                state = it
            }
        }
    }

    private fun checkNumberOfSessions() {
        viewModelScope.launch {
            getSessions().let {
                when (it) {
                    is GetAllSessionsResult.Success -> {
                        isThereActiveSession =
                            it.sessions.filterIsInstance<AccountInfo.Valid>().isNullOrEmpty().not()
                    }
                    is GetAllSessionsResult.Failure.Generic -> {}
                    GetAllSessionsResult.Failure.NoSessionFound -> {
                        isThereActiveSession = false
                    }
                }
            }
        }
    }

    fun navigateBack() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

    fun goToLogin() {
        navigate(NavigationCommand(NavigationItem.Login.getRouteWithArgs()))
    }

    fun goToCreateEnterpriseAccount() {
        navigate(NavigationCommand(NavigationItem.CreateTeam.getRouteWithArgs()))
    }

    fun goToCreatePrivateAccount() {
        navigate(NavigationCommand(NavigationItem.CreatePersonalAccount.getRouteWithArgs()))
    }

    private fun navigate(navigationCommand: NavigationCommand) {
        viewModelScope.launch {
            navigationManager.navigate(navigationCommand)
        }
    }
}
