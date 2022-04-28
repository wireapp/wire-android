package com.wire.android.ui.authentication.login.sso

import androidx.annotation.VisibleForTesting
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.login.LoginError
import com.wire.android.ui.authentication.login.LoginViewModel
import com.wire.android.ui.authentication.login.toLoginError
import com.wire.android.util.EMPTY
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.wire.kalium.logic.feature.auth.sso.GetSSOLoginSessionUseCase

@ExperimentalMaterialApi
@HiltViewModel
class LoginSSOViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val ssoInitiateLoginUseCase: SSOInitiateLoginUseCase,
    private val getSSOLoginSessionUseCase: GetSSOLoginSessionUseCase,
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    navigationManager: NavigationManager,
) : LoginViewModel(navigationManager, clientScopeProviderFactory) {

    var loginState by mutableStateOf(
        LoginSSOState(ssoCode = TextFieldValue(savedStateHandle.get(SSO_CODE_SAVED_STATE_KEY) ?: String.EMPTY))
    )
        private set

    var openWebUrl = MutableSharedFlow<String>()

    fun login(serverConfig: ServerConfig) {
        loginState = loginState.copy(loading = true, loginSSOError = LoginError.None).updateLoginEnabled()
        viewModelScope.launch {
            ssoInitiateLoginUseCase(SSOInitiateLoginUseCase.Param.WithRedirect(loginState.ssoCode.text, serverConfig)).let { result ->
                when (result) {
                    is SSOInitiateLoginResult.Failure -> updateLoginError(result.toLoginSSOError())
                    is SSOInitiateLoginResult.Success -> openWebUrl(result.requestUrl)
                }
            }
        }
    }

    @VisibleForTesting
    fun establishSSOSession(cookie: String, serverConfigId: String) {
        viewModelScope.launch {
            //TODO: serverConfig should be fetched from serverConfigRepository by serverConfigId
            val authSession = getSSOLoginSessionUseCase(cookie, ServerConfig.STAGING)
                .let {
                    when (it) {
                        is SSOLoginSessionResult.Failure -> {
                            updateLoginError(it.toLoginError())
                            return@launch
                        }
                        is SSOLoginSessionResult.Success -> it.userSession
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

    fun onSSOCodeChange(newText: TextFieldValue) {
        // in case an error is showing e.g. inline error is should be cleared
        if (loginState.loginSSOError is LoginError.TextFieldError && newText != loginState.ssoCode) {
            clearLoginError()
        }
        loginState = loginState.copy(ssoCode = newText).updateLoginEnabled()
        savedStateHandle.set(SSO_CODE_SAVED_STATE_KEY, newText.text)
    }

    override fun updateLoginError(error: LoginError) {
        loginState = if (error is LoginError.None) {
            loginState.copy(loginSSOError = error)
        } else {
            loginState.copy(loading = false, loginSSOError = error).updateLoginEnabled()
        }
    }

    fun handleSSOResult(ssoLoginResult: DeepLinkResult.SSOLogin?) = when (ssoLoginResult) {
        is DeepLinkResult.SSOLogin.Success -> establishSSOSession(ssoLoginResult.cookie, ssoLoginResult.serverConfigId)
        is DeepLinkResult.SSOLogin.Failure -> updateLoginError(LoginError.DialogError.SSOResultError(ssoLoginResult.ssoError))
        else -> {}
    }


    private fun openWebUrl(url: String) {
        viewModelScope.launch {
            loginState = loginState.copy(loading = false, loginSSOError = LoginError.None).updateLoginEnabled()
            openWebUrl.emit(url)
        }
    }

    private fun LoginSSOState.updateLoginEnabled() =
        copy(loginEnabled = ssoCode.text.isNotEmpty() && !loading)

    private companion object {
        const val SSO_CODE_SAVED_STATE_KEY = "sso_code"
    }
}

private fun SSOInitiateLoginResult.Failure.toLoginSSOError() = when (this) {
    SSOInitiateLoginResult.Failure.InvalidCodeFormat -> LoginSSOError.TextFieldError.InvalidCodeFormatError
    SSOInitiateLoginResult.Failure.InvalidCode -> LoginError.DialogError.InvalidValue
    is SSOInitiateLoginResult.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
    SSOInitiateLoginResult.Failure.InvalidRedirect ->
        LoginError.DialogError.GenericError(CoreFailure.Unknown(IllegalArgumentException("Invalid Redirect")))
}

private fun SSOLoginSessionResult.Failure.toLoginError() = when (this) {
    SSOLoginSessionResult.Failure.InvalidCookie -> LoginError.DialogError.InvalidSSOCookie
    is SSOLoginSessionResult.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
}

