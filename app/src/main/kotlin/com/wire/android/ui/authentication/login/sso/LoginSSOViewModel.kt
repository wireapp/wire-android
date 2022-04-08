package com.wire.android.ui.authentication.login.sso

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.configuration.ServerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterialApi
@HiltViewModel
class LoginSSOViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager
) : ViewModel() {

    var loginState by mutableStateOf(
        LoginSSOState(ssoCode = TextFieldValue(savedStateHandle.get(SSO_CODE_SAVED_STATE_KEY) ?: String.EMPTY))
    )
        private set

    fun login(serverConfig: ServerConfig) {
        loginState = loginState.copy(loading = true, loginSSOError = LoginSSOError.None).updateLoginEnabled()
        viewModelScope.launch {
            // TODO implement logic
        }
    }

    fun onSSOCodeChange(newText: TextFieldValue) {
        // in case an error is showing e.g. inline error is should be cleared
        if (loginState.loginSSOError is LoginSSOError.TextFieldError && newText != loginState.ssoCode) {
            clearLoginError()
        }
        loginState = loginState.copy(ssoCode = newText).updateLoginEnabled()
        savedStateHandle.set(SSO_CODE_SAVED_STATE_KEY, newText.text)
    }

    private fun updateLoginError(loginError: LoginSSOError) {
        loginState = if (loginError is LoginSSOError.None) {
            loginState.copy(loginSSOError = loginError)
        } else {
            loginState.copy(loading = false, loginSSOError = loginError).updateLoginEnabled()
        }
    }

    fun onDialogDismiss() {
        clearLoginError()
    }

    private fun clearLoginError() {
        updateLoginError(LoginSSOError.None)
    }

    fun onTooManyDevicesError() {
        clearLoginError()
        viewModelScope.launch {
            navigateToRemoveDevicesScreen()
        }
    }

    private fun LoginSSOState.updateLoginEnabled() =
        copy(loginEnabled = ssoCode.text.isNotEmpty() && !loading)


    private suspend fun navigateToRemoveDevicesScreen() =
        navigationManager.navigate(NavigationCommand(NavigationItem.RemoveDevices.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))

    private suspend fun navigateToConvScreen() =
        navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))

    private companion object {
        const val SSO_CODE_SAVED_STATE_KEY = "sso_code"
    }
}
