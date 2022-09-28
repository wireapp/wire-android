package com.wire.android.ui.authentication.login

import androidx.annotation.VisibleForTesting
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.client.ClientCapability
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterialApi
@HiltViewModel
@Suppress("TooManyFunctions")
open class LoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    authServerConfigProvider: AuthServerConfigProvider
) : ViewModel() {
    private val _loginStateFlow = MutableStateFlow(
        LoginState(
            ssoCode = TextFieldValue(savedStateHandle[SSO_CODE_SAVED_STATE_KEY] ?: String.EMPTY),
            userIdentifier = TextFieldValue(savedStateHandle[USER_IDENTIFIER_SAVED_STATE_KEY] ?: String.EMPTY),
            password = TextFieldValue(String.EMPTY)
        )
    )
    val loginStateFlow: StateFlow<LoginState> = _loginStateFlow

    protected fun updateLoginState(newState: LoginState) {
        viewModelScope.launch { _loginStateFlow.emit(newState) }
    }

    val serverConfig = authServerConfigProvider.authServer.value

    open fun updateSSOLoginError(error: LoginError) {
        val newState = if (error is LoginError.None) {
            loginStateFlow.value.copy(loginError = error)
        } else {
            loginStateFlow.value.copy(ssoLoginLoading = false, loginError = error).updateSSOLoginEnabled()
        }
        updateLoginState(newState)
    }

    open fun updateEmailLoginError(error: LoginError) {
        val newState = if (error is LoginError.None) {
            loginStateFlow.value.copy(loginError = error)
        } else {
            loginStateFlow.value.copy(emailLoginLoading = false, loginError = error).updateEmailLoginEnabled()
        }
        updateLoginState(newState)
    }

    fun onDialogDismiss() {
        clearLoginErrors()
    }

    private fun clearLoginErrors() {
        clearSSOLoginError()
        clearEmailLoginError()
    }

    fun clearSSOLoginError() {
        updateSSOLoginError(LoginError.None)
    }

    fun clearEmailLoginError() {
        updateEmailLoginError(LoginError.None)
    }

    fun onTooManyDevicesError() {
        clearLoginErrors()
        navigateToRemoveDevicesScreen()
    }

    suspend fun registerClient(
        userId: UserId,
        password: String?,
        capabilities: List<ClientCapability>? = null
    ): RegisterClientResult {
        val clientScope = clientScopeProviderFactory.create(userId).clientScope
        return clientScope.getOrRegister(
            RegisterClientUseCase.RegisterClientParam(
                password = password,
                capabilities = capabilities
            )
        )
    }

    fun navigateBack() = viewModelScope.launch {
        navigationManager.navigateBack()
    }

    @VisibleForTesting
    fun navigateToConvScreen() = viewModelScope.launch {
        navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
    }

    private fun navigateToRemoveDevicesScreen() = viewModelScope.launch {
        navigationManager.navigate(NavigationCommand(NavigationItem.RemoveDevices.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
    }

    fun updateTheApp() {
        // todo : update the app after releasing on the store
    }

    companion object {
        const val SSO_CODE_SAVED_STATE_KEY = "sso_code"
        const val USER_IDENTIFIER_SAVED_STATE_KEY = "user_identifier"
    }
}

fun AuthenticationResult.Failure.toLoginError() = when (this) {
    is AuthenticationResult.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
    AuthenticationResult.Failure.InvalidCredentials -> LoginError.DialogError.InvalidCredentialsError
    AuthenticationResult.Failure.InvalidUserIdentifier -> LoginError.TextFieldError.InvalidValue
}

fun RegisterClientResult.Failure.toLoginError() = when (this) {
    is RegisterClientResult.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
    RegisterClientResult.Failure.InvalidCredentials -> LoginError.DialogError.InvalidCredentialsError
    RegisterClientResult.Failure.TooManyClients -> LoginError.TooManyDevicesError
    RegisterClientResult.Failure.PasswordAuthRequired -> LoginError.DialogError.PasswordNeededToRegisterClient
}

fun AddAuthenticatedUserUseCase.Result.Failure.toLoginError(): LoginError = when (this) {
    is AddAuthenticatedUserUseCase.Result.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
    AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists -> LoginError.DialogError.UserAlreadyExists
}
