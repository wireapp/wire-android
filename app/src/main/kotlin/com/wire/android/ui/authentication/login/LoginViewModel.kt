package com.wire.android.ui.authentication.login

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.LoginUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterialApi::class)
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
) : ViewModel() {

    var loginState by mutableStateOf(
        LoginState(
            userIdentifier = TextFieldValue(savedStateHandle.get(USER_IDENTIFIER_SAVED_STATE_KEY) ?: String.EMPTY),
            password = TextFieldValue(String.EMPTY)
        )
    )
        private set

    fun login(serverConfig: ServerConfig) {
        loginState = loginState.copy(loading = true, loginError = LoginError.None).updateLoginEnabled()
        viewModelScope.launch {
            val loginResult = loginUseCase(loginState.userIdentifier.text, loginState.password.text, true, serverConfig)
            val loginError =
                if (loginResult is AuthenticationResult.Success) registerClient(loginResult.userSession).toLoginError()
                else loginResult.toLoginError()
            // TODO what if user logs in but doesn't register a new device?
            loginState = loginState.copy(loading = false, loginError = loginError).updateLoginEnabled()
            if (loginError is LoginError.None)
                navigateToConvScreen()
        }
    }

    private suspend fun registerClient(authSession: AuthSession): RegisterClientResult {
        val clientScope = clientScopeProviderFactory.create(authSession).clientScope
        return clientScope.register(loginState.password.text, null)
    }

    fun onUserIdentifierChange(newText: TextFieldValue) {
        // in case an error is showing e.g. inline error is should be cleared
        if (loginState.loginError !is LoginError.None) {
            clearLoginError()
        }
        loginState = loginState.copy(userIdentifier = newText).updateLoginEnabled()
        savedStateHandle.set(USER_IDENTIFIER_SAVED_STATE_KEY, newText.text)
    }

    fun onPasswordChange(newText: TextFieldValue) {
        loginState = loginState.copy(password = newText).updateLoginEnabled()
    }

    fun clearLoginError() {
        loginState = loginState.copy(loginError = LoginError.None)
    }

    private fun LoginState.updateLoginEnabled() =
        copy(loginEnabled = userIdentifier.text.isNotEmpty() && password.text.isNotEmpty() && !loading)

    // TODO: login error Mapper ?
    private fun AuthenticationResult.toLoginError() =
        when (this) {
            is AuthenticationResult.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
            AuthenticationResult.Failure.InvalidCredentials -> LoginError.DialogError.InvalidCredentialsError
            AuthenticationResult.Failure.InvalidUserIdentifier -> LoginError.TextFieldError.InvalidUserIdentifierError
            is AuthenticationResult.Success -> LoginError.None
        }

    private fun RegisterClientResult.toLoginError() =
        when(this) {
            is RegisterClientResult.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
            is RegisterClientResult.Failure.ProteusFailure -> LoginError.DialogError.GenericError(CoreFailure.Unknown(this.e))
            RegisterClientResult.Failure.InvalidCredentials -> LoginError.DialogError.InvalidCredentialsError
            RegisterClientResult.Failure.TooManyClients -> LoginError.TooManyDevicesError
            is RegisterClientResult.Success -> LoginError.None
        }

    private suspend fun navigateToConvScreen() =
        navigationManager.navigate(NavigationCommand(NavigationItem.Home.navigationRoute(), BackStackMode.CLEAR_WHOLE))

    private companion object {
        const val USER_IDENTIFIER_SAVED_STATE_KEY = "user_identifier"
    }
}
