package com.wire.android.ui.authentication.login.sso

import androidx.annotation.VisibleForTesting
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
import com.wire.android.ui.authentication.login.updateSSOLoginEnabled
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.sso.GetSSOLoginSessionUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import com.wire.kalium.logic.feature.client.RegisterClientResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ExperimentalMaterialApi
@HiltViewModel
class LoginSSOViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    qualifiedIdMapper: QualifiedIdMapper,
    private val ssoInitiateLoginUseCase: SSOInitiateLoginUseCase,
    private val getSSOLoginSessionUseCase: GetSSOLoginSessionUseCase,
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    userSessionsUseCaseFactory: UserSessionsUseCaseProvider.Factory,
    navigationManager: NavigationManager,
    authServerConfigProvider: AuthServerConfigProvider,
) : LoginViewModel(
    savedStateHandle,
    navigationManager,
    qualifiedIdMapper,
    clientScopeProviderFactory,
    userSessionsUseCaseFactory,
    authServerConfigProvider
) {

    var openWebUrl = MutableSharedFlow<String>()

    fun login() {
        loginState = loginState.copy(ssoLoginLoading = true, loginError = LoginError.None).updateSSOLoginEnabled()
        viewModelScope.launch {
            ssoInitiateLoginUseCase(SSOInitiateLoginUseCase.Param.WithRedirect(loginState.ssoCode.text)).let { result ->
                when (result) {
                    is SSOInitiateLoginResult.Failure -> updateSSOLoginError(result.toLoginSSOError())
                    is SSOInitiateLoginResult.Success -> openWebUrl(result.requestUrl)
                }
            }
        }
    }

    @VisibleForTesting
    fun establishSSOSession(cookie: String) {
        loginState = loginState.copy(ssoLoginLoading = true, loginError = LoginError.None).updateSSOLoginEnabled()
        viewModelScope.launch {
            val authSession = getSSOLoginSessionUseCase(cookie).let {
                when (it) {
                    is SSOLoginSessionResult.Failure -> {
                        updateSSOLoginError(it.toLoginError())
                        return@launch
                    }
                    is SSOLoginSessionResult.Success -> it.userSession
                }
            }
            val storedUserId = addAuthenticatedUser(authSession, false).let {
                when (it) {
                    is AddAuthenticatedUserUseCase.Result.Failure -> {
                        updateSSOLoginError(it.toLoginError())
                        return@launch
                    }
                    is AddAuthenticatedUserUseCase.Result.Success -> it.userId
                }
            }
            // TODO: show password dialog if BE required password for SSO
            registerClient(storedUserId, null).let {
                when (it) {
                    is RegisterClientResult.Success -> {
                        registerPushToken(storedUserId, it.client.id)
                        navigateToConvScreen()
                    }
                    is RegisterClientResult.Failure -> {
                        updateSSOLoginError(it.toLoginError())
                        return@launch
                    }
                }
            }
        }
    }

    fun onSSOCodeChange(newText: TextFieldValue) {
        // in case an error is showing e.g. inline error is should be cleared
        if (loginState.loginError is LoginError.TextFieldError && newText != loginState.ssoCode) {
            clearSSOLoginError()
        }
        loginState = loginState.copy(ssoCode = newText).updateSSOLoginEnabled()
        savedStateHandle.set(SSO_CODE_SAVED_STATE_KEY, newText.text)
    }

    fun handleSSOResult(ssoLoginResult: DeepLinkResult.SSOLogin?) = when (ssoLoginResult) {
        is DeepLinkResult.SSOLogin.Success -> {
            establishSSOSession(ssoLoginResult.cookie)
        }

        is DeepLinkResult.SSOLogin.Failure -> updateSSOLoginError(LoginError.DialogError.SSOResultError(ssoLoginResult.ssoError))
        null -> {}
    }

    private fun openWebUrl(url: String) {
        viewModelScope.launch {
            loginState = loginState.copy(ssoLoginLoading = false, loginError = LoginError.None).updateSSOLoginEnabled()
            openWebUrl.emit(url)
        }
    }
}

private fun SSOInitiateLoginResult.Failure.toLoginSSOError() = when (this) {
    SSOInitiateLoginResult.Failure.InvalidCodeFormat -> LoginError.TextFieldError.InvalidValue
    SSOInitiateLoginResult.Failure.InvalidCode -> LoginError.DialogError.InvalidCodeError
    is SSOInitiateLoginResult.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
    SSOInitiateLoginResult.Failure.InvalidRedirect ->
        LoginError.DialogError.GenericError(CoreFailure.Unknown(IllegalArgumentException("Invalid Redirect")))
}

private fun SSOLoginSessionResult.Failure.toLoginError() = when (this) {
    SSOLoginSessionResult.Failure.InvalidCookie -> LoginError.DialogError.InvalidSSOCookie
    is SSOLoginSessionResult.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
}

