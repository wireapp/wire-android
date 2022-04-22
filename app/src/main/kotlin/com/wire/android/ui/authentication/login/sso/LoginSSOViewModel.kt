package com.wire.android.ui.authentication.login.sso

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
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
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
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.sso.GetSSOLoginSessionUseCase

@ExperimentalMaterialApi
@HiltViewModel
class LoginSSOViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val ssoInitiateLoginUseCase: SSOInitiateLoginUseCase,
    private val getSSOLoginSessionUseCase: GetSSOLoginSessionUseCase,
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val navigationManager: NavigationManager,
    ) : ViewModel() {

    var loginState by mutableStateOf(
        LoginSSOState(ssoCode = TextFieldValue(savedStateHandle.get(SSO_CODE_SAVED_STATE_KEY) ?: String.EMPTY))
    )
        private set

    var openWebUrl = MutableSharedFlow<String>()

    fun login(serverConfig: ServerConfig) {
        loginState = loginState.copy(loading = true, loginSSOError = LoginSSOError.None).updateLoginEnabled()
        viewModelScope.launch {
            ssoInitiateLoginUseCase(SSOInitiateLoginUseCase.Param.WithRedirect(loginState.ssoCode.text, serverConfig)).let { result ->
                when(result) {
                    is SSOInitiateLoginResult.Failure -> updateLoginError(result.toLoginSSOError())
                    is SSOInitiateLoginResult.Success -> openWebUrl(result.requestUrl)
                }
            }
        }
    }

    fun establishSSOSession(ssoLoginResult: DeepLinkResult.SSOLogin.Success) {
        viewModelScope.launch {
            //TODO: serverConfig should be fetched of the serverConfigRepository
            val authSession = getSSOLoginSessionUseCase(ssoLoginResult.cookie, ServerConfig.STAGING)
                .let {
                    when (it) {
                        is SSOLoginSessionResult.Failure -> {
                            return@launch
                        }
                        is SSOLoginSessionResult.Success -> it.userSession
                    }
                }
            val storedUserId = addAuthenticatedUser(authSession, false).let {
                when (it) {
                    is AddAuthenticatedUserUseCase.Result.Failure -> {
                        return@launch
                    }
                    is AddAuthenticatedUserUseCase.Result.Success -> it.userId
                }
            }
            registerClient(storedUserId).let {
                when (it) {
                    is RegisterClientResult.Failure -> {
                        return@launch
                    }
                    is RegisterClientResult.Success -> navigateToConvScreen()
                }
            }
        }

    }

    private suspend fun registerClient(userId: UserId): RegisterClientResult {
        val clientScope = clientScopeProviderFactory.create(userId).clientScope
        return clientScope.register(null, null)
    }

    private suspend fun navigateToConvScreen() =
        navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))


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

    fun handleSSOResult(ssoLoginResult: DeepLinkResult.SSOLogin?) = when(ssoLoginResult){
        is DeepLinkResult.SSOLogin.Success -> establishSSOSession(ssoLoginResult)
        is DeepLinkResult.SSOLogin.Failure -> updateResultDialogError(LoginSSOError.DialogError.ResultError(ssoLoginResult.ssoError))
        else -> updateResultDialogError(LoginSSOError.DialogError.ResultError(SSOFailureCodes.Unknown))
    }

    private fun updateResultDialogError(loginError: LoginSSOError) {
        loginState = if (loginError is LoginSSOError.None) {
            loginState.copy(ssoResultError = loginError)
        } else {
            loginState.copy(ssoResultError = loginError).updateLoginEnabled()
        }
    }

    private fun openWebUrl(url: String) {
        viewModelScope.launch {
            loginState = loginState.copy(loading = false, loginSSOError = LoginSSOError.None).updateLoginEnabled()
            openWebUrl.emit(url)
        }
    }

    fun onDialogDismiss() {
        clearLoginError()
    }

    private fun clearLoginError() {
        updateLoginError(LoginSSOError.None)
    }

    private fun LoginSSOState.updateLoginEnabled() =
        copy(loginEnabled = ssoCode.text.isNotEmpty() && !loading)

    private companion object {
        const val SSO_CODE_SAVED_STATE_KEY = "sso_code"
    }
}

private fun SSOInitiateLoginResult.Failure.toLoginSSOError() = when (this) {
    SSOInitiateLoginResult.Failure.InvalidCode -> LoginSSOError.TextFieldError.InvalidCodeError
    is SSOInitiateLoginResult.Failure.Generic -> LoginSSOError.DialogError.GenericError(this.genericFailure)
    SSOInitiateLoginResult.Failure.InvalidRedirect ->
        LoginSSOError.DialogError.GenericError(CoreFailure.Unknown(IllegalArgumentException("Invalid Redirect")))
}
