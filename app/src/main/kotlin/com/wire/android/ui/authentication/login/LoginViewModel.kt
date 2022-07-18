package com.wire.android.ui.authentication.login

import androidx.annotation.VisibleForTesting
import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.welcome.WelcomeScreenState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.client.ClientCapability
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.RegisterTokenResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterialApi
@HiltViewModel
open class LoginViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val authServerConfigProvider: AuthServerConfigProvider,
    val currentSessionFlow: CurrentSessionFlowUseCase,
    dispatchers: DispatcherProvider
) : ViewModel() {

    val serverConfig = authServerConfigProvider.authServer.value


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
                    is AuthSession.Session.Invalid -> {
                        when ((result.authSession.session as AuthSession.Session.Invalid).reason) {
                            LogoutReason.SELF_LOGOUT -> {
                                title = "Self logout"
                                body = "Self Logout"
                                state = state.copy(showLogoutDialog = true)
                            }

                            LogoutReason.REMOVED_CLIENT -> {
                                title = "Removed Device"
                                body = "You were signed out because your device was removed."
                                state = state.copy(showLogoutDialog = true)
                            }

                            LogoutReason.DELETED_ACCOUNT -> {
                                title = "Deleted User"
                                body = "You were signed out because your account was deleted."
                                state = state.copy(showLogoutDialog = true)
                            }
                        }
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

    open fun updateLoginError(error: LoginError) {}

    fun onDialogDismiss() {
        clearLoginError()
    }

    fun clearLoginError() {
        updateLoginError(LoginError.None)
    }

    fun onTooManyDevicesError() {
        clearLoginError()
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
