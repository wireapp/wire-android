package com.wire.android.ui.authentication.devices.register


import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import com.wire.kalium.logic.feature.session.RegisterTokenResult
import com.wire.kalium.logic.feature.session.RegisterTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterialApi::class)
@HiltViewModel
class RegisterDeviceViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val registerClientUseCase: RegisterClientUseCase,
    private val pushTokenUseCase: RegisterTokenUseCase
) : ViewModel() {

    var state: RegisterDeviceState by mutableStateOf(RegisterDeviceState())
        private set

    fun onPasswordChange(newText: TextFieldValue) {
        if (state.password != newText)
            state = state.copy(password = newText, error = RegisterDeviceError.None, continueEnabled = newText.text.isNotEmpty())
    }

    fun onErrorDismiss() {
        state = state.copy(error = RegisterDeviceError.None)
    }

    fun onContinue() {
        state = state.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            if (!validatePasswordUseCase(state.password.text))
                state = state.copy(loading = false, continueEnabled = true, error = RegisterDeviceError.InvalidCredentialsError)
            else when (val registerDeviceResult = registerClientUseCase(
                RegisterClientUseCase.RegisterClientParam(
                    password = state.password.text,
                    capabilities = null,
                ))) {
                is RegisterClientResult.Failure.TooManyClients -> navigateToRemoveDevicesScreen()
                is RegisterClientResult.Success -> {
                    registerPushToken(registerDeviceResult.client.clientId.value)
                    navigateToHomeScreen()}
                is RegisterClientResult.Failure.Generic -> state = state.copy(
                        loading = false,
                        continueEnabled = true,
                        error = RegisterDeviceError.GenericError(registerDeviceResult.genericFailure)
                )
                RegisterClientResult.Failure.InvalidCredentials -> state = state.copy(
                    loading = false,
                    continueEnabled = true,
                    error = RegisterDeviceError.InvalidCredentialsError
                )
            }
        }
    }

    private suspend fun registerPushToken(clientId: String) {
        pushTokenUseCase(BuildConfig.SENDER_ID, clientId).let { registerTokenResult ->
            when (registerTokenResult) {
                is RegisterTokenResult.Success ->
                    appLogger.i("PushToken Registered Successfully")
                is RegisterTokenResult.Failure ->
                    //TODO: handle failure in settings to allow the user to retry tokenRegistration
                    appLogger.i("PushToken Registration Failed: $registerTokenResult")
            }
        }
    }

    private suspend fun navigateToRemoveDevicesScreen() =
        navigationManager.navigate(NavigationCommand(NavigationItem.RemoveDevices.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))

    private suspend fun navigateToHomeScreen() =
        navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
}
