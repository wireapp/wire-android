package com.wire.android.ui.authentication.login.email

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
import com.wire.android.ui.authentication.login.LoginEmailError
import com.wire.android.ui.authentication.login.LoginEmailState
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.LoginUseCase
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterialApi
@HiltViewModel
class LoginEmailViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager
) : ViewModel() {

    var loginState by mutableStateOf(
        LoginEmailState(
            userIdentifier = TextFieldValue(savedStateHandle.get(USER_IDENTIFIER_SAVED_STATE_KEY) ?: String.EMPTY),
            password = TextFieldValue(String.EMPTY)
        )
    )
        private set

    fun login(serverConfig: ServerConfig) {
        loginState = loginState.copy(loading = true, loginEmailError = LoginEmailError.None).updateLoginEnabled()
        viewModelScope.launch {
            val authSession = loginUseCase(loginState.userIdentifier.text, loginState.password.text, true, serverConfig)
                .let {
                    when (it) {
                        is AuthenticationResult.Failure -> {
                            updateLoginError(it.toLoginError())
                            return@launch
                        }
                        is AuthenticationResult.Success -> it.userSession
                    }
                }
            val storedUserId = addAuthenticatedUser(authSession, false).let {
                when (it) {
                    is AddAuthenticatedUserUseCase.Result.Failure -> {
                        updateLoginError(it.toLoginError())
                        return@launch
                    }
                    is AddAuthenticatedUserUseCase.Result.Success -> it.userId
                }
            }
            registerClient(storedUserId).let {
                when (it) {
                    is RegisterClientResult.Failure -> {
                        updateLoginError(it.toLoginError())
                        return@launch
                    }
                    is RegisterClientResult.Success -> navigateToConvScreen()
                }
            }
        }
    }

    private suspend fun registerClient(userId: UserId): RegisterClientResult {
        val clientScope = clientScopeProviderFactory.create(userId).clientScope
        return clientScope.register(loginState.password.text, null)
    }

    fun onUserIdentifierChange(newText: TextFieldValue) {
        // in case an error is showing e.g. inline error is should be cleared
        if (loginState.loginEmailError is LoginEmailError.TextFieldError && newText != loginState.userIdentifier) {
            clearLoginError()
        }
        loginState = loginState.copy(userIdentifier = newText).updateLoginEnabled()
        savedStateHandle.set(USER_IDENTIFIER_SAVED_STATE_KEY, newText.text)
    }

    fun onPasswordChange(newText: TextFieldValue) {
        loginState = loginState.copy(password = newText).updateLoginEnabled()
    }

    private fun updateLoginError(loginError: LoginEmailError) {
        loginState = if (loginError is LoginEmailError.None) {
            loginState.copy(loginEmailError = loginError)
        } else {
            loginState.copy(loading = false, loginEmailError = loginError).updateLoginEnabled()
        }

    }

    fun onDialogDismiss() {
        clearLoginError()
    }

    private fun clearLoginError() {
        updateLoginError(LoginEmailError.None)
    }

    fun onTooManyDevicesError() {
        clearLoginError()
        viewModelScope.launch {
            navigateToRemoveDevicesScreen()
        }
    }

    private fun LoginEmailState.updateLoginEnabled() =
        copy(loginEnabled = userIdentifier.text.isNotEmpty() && password.text.isNotEmpty() && !loading)


    private suspend fun navigateToRemoveDevicesScreen() =
        navigationManager.navigate(NavigationCommand(NavigationItem.RemoveDevices.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))

    private suspend fun navigateToConvScreen() =
        navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))

    private companion object {
        const val USER_IDENTIFIER_SAVED_STATE_KEY = "user_identifier"
    }
}

// TODO: login error Mapper ?
private fun AuthenticationResult.Failure.toLoginError() = when (this) {
    is AuthenticationResult.Failure.Generic -> LoginEmailError.DialogError.GenericError(this.genericFailure)
    AuthenticationResult.Failure.InvalidCredentials -> LoginEmailError.DialogError.InvalidCredentialsError
    AuthenticationResult.Failure.InvalidUserIdentifier -> LoginEmailError.TextFieldError.InvalidUserIdentifierError
}

private fun RegisterClientResult.Failure.toLoginError() = when (this) {
    is RegisterClientResult.Failure.Generic -> LoginEmailError.DialogError.GenericError(this.genericFailure)
    RegisterClientResult.Failure.InvalidCredentials -> LoginEmailError.DialogError.InvalidCredentialsError
    RegisterClientResult.Failure.TooManyClients -> LoginEmailError.TooManyDevicesError
}

private fun AddAuthenticatedUserUseCase.Result.Failure.toLoginError(): LoginEmailError = when (this) {
    is AddAuthenticatedUserUseCase.Result.Failure.Generic -> LoginEmailError.DialogError.GenericError(this.genericFailure)
    AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists -> LoginEmailError.DialogError.UserAlreadyExists
}
