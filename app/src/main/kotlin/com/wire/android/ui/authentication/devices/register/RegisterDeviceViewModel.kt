package com.wire.android.ui.authentication.devices.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class RegisterDeviceViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val registerClientUseCase: RegisterClientUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
) : ViewModel() {

    var state: RegisterDeviceState by mutableStateOf(RegisterDeviceState())
        private set

    init {
        runBlocking {
            updateState(state.copy(loading = true))
            isPasswordRequired().let {
                updateState(state.copy(loading = false))
                when (it) {
                    is IsPasswordRequiredUseCase.Result.Failure -> {
                        updateErrorState(RegisterDeviceError.GenericError(it.cause))
                    }
                    is IsPasswordRequiredUseCase.Result.Success -> {
                        updateState(state.copy(isPasswordRequired = it.value))
                    }
                }
            }
            when (state.isPasswordRequired) {
                true -> {}
                false -> registerClient(null)
            }
        }
    }

    fun onPasswordChange(newText: TextFieldValue) {
        if (state.password != newText)
            updateState(state.copy(password = newText, error = RegisterDeviceError.None, continueEnabled = newText.text.isNotEmpty()))
    }

    fun onErrorDismiss() {
        updateErrorState(RegisterDeviceError.None)
    }

    private suspend fun registerClient(password: String?) {
        updateState(state.copy(loading = true, continueEnabled = false))
        when (val registerDeviceResult = registerClientUseCase(
            RegisterClientUseCase.RegisterClientParam(
                password = password,
                capabilities = null,
            )
        )) {
            is RegisterClientResult.Failure.TooManyClients -> navigateToRemoveDevicesScreen()
            is RegisterClientResult.Success -> {
                navigateToHomeScreen()
            }
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
            RegisterClientResult.Failure.PasswordAuthRequired -> state = state.copy(
                loading = false,
                isPasswordRequired = true
            )
        }
    }

    fun onContinue() {
        viewModelScope.launch {
            registerClient(state.password.text)
        }
    }

    fun updateErrorState(error: RegisterDeviceError) {
        updateState(state.copy(error = error))
    }

    fun updateState(newState: RegisterDeviceState) {
        state = newState
    }

    private suspend fun navigateToRemoveDevicesScreen() =
        navigationManager.navigate(NavigationCommand(NavigationItem.RemoveDevices.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))

    private suspend fun navigateToHomeScreen() =
        navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
}
