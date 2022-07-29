package com.wire.android.ui.authentication.login.email

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.UserSessionsUseCaseProvider
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.login.LoginError
import com.wire.android.ui.authentication.login.LoginViewModel
import com.wire.android.ui.authentication.login.toLoginError
import com.wire.android.ui.authentication.login.updateLoginEnabled
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.LoginUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ExperimentalMaterialApi
@HiltViewModel
class LoginEmailViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    userSessionsUseCaseFactory: UserSessionsUseCaseProvider.Factory,
    private val savedStateHandle: SavedStateHandle,
    navigationManager: NavigationManager,
    authServerConfigProvider: AuthServerConfigProvider,
) : LoginViewModel(savedStateHandle, navigationManager, clientScopeProviderFactory, userSessionsUseCaseFactory, authServerConfigProvider) {

    fun login() {
        loginState = loginState.copy(loading = true, loginError = LoginError.None).updateLoginEnabled()
        viewModelScope.launch {
            val authSession = loginUseCase(loginState.userIdentifier.text, loginState.password.text, true)
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
            registerClient(storedUserId, loginState.password.text).let {
                when (it) {
                    is RegisterClientResult.Failure -> {
                        updateLoginError(it.toLoginError())
                        return@launch
                    }

                    is RegisterClientResult.Success -> {
                        registerPushToken(storedUserId, it.client.id)
                        navigateToConvScreen()
                    }
                }
            }
        }
    }

    fun onUserIdentifierChange(newText: TextFieldValue) {
        // in case an error is showing e.g. inline error is should be cleared
        if (loginState.loginError is LoginError.TextFieldError && newText != loginState.userIdentifier) {
            clearLoginError()
        }
        loginState = loginState.copy(userIdentifier = newText).updateLoginEnabled()
        savedStateHandle.set(USER_IDENTIFIER_SAVED_STATE_KEY, newText.text)
    }

    fun onPasswordChange(newText: TextFieldValue) {
        loginState = loginState.copy(password = newText).updateLoginEnabled()
    }
}
