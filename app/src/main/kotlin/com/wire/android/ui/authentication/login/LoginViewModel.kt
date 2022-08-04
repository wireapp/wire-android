package com.wire.android.ui.authentication.login

import androidx.annotation.VisibleForTesting
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.NoSession
import com.wire.android.di.UserSessionsUseCaseProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.client.ClientCapability
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import com.wire.kalium.logic.feature.session.RegisterTokenResult
import com.wire.kalium.logic.functional.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterialApi
@HiltViewModel
open class LoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    @NoSession qualifiedIdMapper: QualifiedIdMapper,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val userSessionsUseCaseFactory: UserSessionsUseCaseProvider.Factory,
    authServerConfigProvider: AuthServerConfigProvider
) : ViewModel() {
    var loginState by mutableStateOf(
        LoginState(
            ssoCode = TextFieldValue(savedStateHandle.get(SSO_CODE_SAVED_STATE_KEY) ?: String.EMPTY),
            userIdentifier = TextFieldValue(savedStateHandle[USER_IDENTIFIER_SAVED_STATE_KEY] ?: String.EMPTY),
            password = TextFieldValue(String.EMPTY)
        )
    )
        protected set

    val userId: QualifiedID? = savedStateHandle.get<String>(EXTRA_USER_ID)?.let {
        qualifiedIdMapper.fromStringToQualifiedID(it)
    }

    val serverConfig = authServerConfigProvider.authServer.value

    init {
        viewModelScope.launch {
            if (userId != null)
                userSessionsUseCaseFactory.create().sessionsUseCase.getUserSession(userId).map {
                    if (it.session is AuthSession.Session.Invalid) {
                        with(it.session as AuthSession.Session.Invalid) {
                            val loginError = when (this.reason) {
                                LogoutReason.SELF_LOGOUT -> LoginError.None
                                LogoutReason.REMOVED_CLIENT -> LoginError.DialogError.InvalidSession.RemovedClient
                                LogoutReason.DELETED_ACCOUNT -> LoginError.DialogError.InvalidSession.DeletedAccount
                                LogoutReason.SESSION_EXPIRED -> LoginError.DialogError.InvalidSession.SessionExpired
                            }
                            loginState = loginState.copy(loginError = loginError)
                        }
                    }
                }
        }
    }

    open fun updateSSOLoginError(error: LoginError) {
        loginState = if (error is LoginError.None) {
            loginState.copy(loginError = error)
        } else {
            loginState.copy(ssoLoginLoading = false, loginError = error).updateSSOLoginEnabled()
        }
    }

    open fun updateEmailLoginError(error: LoginError) {
        loginState = if (error is LoginError.None) {
            loginState.copy(loginError = error)
        } else {
            loginState.copy(emailLoginLoading = false, loginError = error).updateEmailLoginEnabled()
        }
    }

    private fun deleteInvalidSession() {
        if (loginState.loginError is LoginError.DialogError.InvalidSession && userId != null) {
            userSessionsUseCaseFactory.create().sessionsUseCase
                .deleteInvalidSession(userId)
        }
    }

    fun onDialogDismiss() {
        deleteInvalidSession()
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
        return clientScope.register(
            RegisterClientUseCase.RegisterClientParam(
                password = password,
                capabilities = capabilities
            )
        )
    }

    suspend fun registerPushToken(userId: UserId, clientId: ClientId) {
        val clientScope = clientScopeProviderFactory.create(userId).clientScope
        clientScope.registerPushToken(BuildConfig.SENDER_ID, clientId).let { registerTokenResult ->
            when (registerTokenResult) {
                is RegisterTokenResult.Success ->
                    appLogger.i("PushToken Registered Successfully")

                is RegisterTokenResult.Failure ->
                    //TODO: handle failure in settings to allow the user to retry tokenRegistration
                    appLogger.i("PushToken Registration Failed: $registerTokenResult")
            }
        }
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
