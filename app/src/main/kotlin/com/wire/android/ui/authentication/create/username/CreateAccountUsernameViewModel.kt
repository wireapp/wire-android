package com.wire.android.ui.authentication.create.username

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.kalium.logic.feature.auth.ValidateUserHandleResult
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleResult
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterialApi::class)
@HiltViewModel
class CreateAccountUsernameViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val validateUserHandleUseCase: ValidateUserHandleUseCase,
    private val setUserHandleUseCase: SetUserHandleUseCase
) : ViewModel() {
    var state: CreateAccountUsernameViewState by mutableStateOf(CreateAccountUsernameViewState())
        private set

    fun onUsernameChange(newText: TextFieldValue) {
        state = state.copy(
            username = newText,
            error = CreateAccountUsernameViewState.UsernameError.None,
            continueEnabled = newText.text.isNotEmpty() && !state.loading
        )
    }

    fun onErrorDismiss() {
        state = state.copy(error = CreateAccountUsernameViewState.UsernameError.None)
    }

    fun onContinue() {
        state = state.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            val usernameError = if (!validateUserHandleUseCase(state.username.text.trim()).isValid)
                CreateAccountUsernameViewState.UsernameError.TextFieldError.UsernameInvalidError
            else when (val result = setUserHandleUseCase(state.username.text.trim())) {
                is SetUserHandleResult.Failure.Generic ->
                    CreateAccountUsernameViewState.UsernameError.DialogError.GenericError(result.error)
                SetUserHandleResult.Failure.HandleExists ->
                    CreateAccountUsernameViewState.UsernameError.TextFieldError.UsernameTakenError
                SetUserHandleResult.Failure.InvalidHandle ->
                    CreateAccountUsernameViewState.UsernameError.TextFieldError.UsernameInvalidError
                SetUserHandleResult.Success -> CreateAccountUsernameViewState.UsernameError.None
            }
            state = state.copy(loading = false, continueEnabled = true, error = usernameError)
            if (usernameError is CreateAccountUsernameViewState.UsernameError.None)
                navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
        }
    }
}
