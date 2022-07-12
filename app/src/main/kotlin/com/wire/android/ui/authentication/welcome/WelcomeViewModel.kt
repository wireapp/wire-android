package com.wire.android.ui.authentication.welcome

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WelcomeScreenState(
    val showLogoutDialog: Boolean = false,
)

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    val currentSessionFlow: CurrentSessionFlowUseCase,
    dispatchers: DispatcherProvider,
) : ViewModel() {


    var state by mutableStateOf(WelcomeScreenState(false))

    var title = ""
    var body = ""
    var userId: QualifiedID? = null

    private val observeUserId = currentSessionFlow()
        .map { result ->
            if (result is CurrentSessionResult.Success) {
                appLogger.d("############### ${result.authSession}")
                userId = result.authSession.session.userId
                when (result.authSession.session) {
                    is AuthSession.Session.RemovedClient -> {
                        title = "Removed Device"
                        body = "You were signed out because your device was removed."
                        state = state.copy(showLogoutDialog = true)
                    }

                    is AuthSession.Session.UserDeleted -> {
                        title = "Deleted User"
                        body = "You were signed out because your account was deleted."
                        state = state.copy(showLogoutDialog = true)
                    }

                    else -> {
                        currentSessionFlow.deleteSession(userId!!)
                        state = state.copy(showLogoutDialog = false)
                        title = ""
                        body = ""
                    }
                }
            } else {
                if (state.showLogoutDialog)
                    state = state.copy(showLogoutDialog = false)
                null
            }
        }
        .distinctUntilChanged()
        .flowOn(dispatchers.io())
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    fun observer() {
        viewModelScope.launch {
            observeUserId.firstOrNull()
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
