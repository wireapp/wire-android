package com.wire.android.ui.authentication.create.username

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterialApi::class)
@HiltViewModel
class CreateAccountUsernameViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val validateUserHandleUseCase: ValidateUserHandleUseCase
): ViewModel() {
    var state: CreateAccountUsernameViewState by mutableStateOf(CreateAccountUsernameViewState())
        private set

    fun onUsernameChange(newText: TextFieldValue) {
        state = state.copy(
            username = newText,
            error = CreateAccountUsernameViewState.UsernameError.None,
            continueEnabled = newText.text.isNotEmpty() && !state.loading)
    }

    fun onErrorDismiss() {
        state = state.copy(error = CreateAccountUsernameViewState.UsernameError.None)
    }

    fun onContinue() {
        state = state.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            val usernameError = if(validateUserHandleUseCase(state.username.text.trim())) CreateAccountUsernameViewState.UsernameError.None
            else CreateAccountUsernameViewState.UsernameError.TextFieldError.UsernameInvalidError
            //TODO change username request
            state = state.copy(loading = false, continueEnabled = true, error = usernameError)
            if(usernameError is CreateAccountUsernameViewState.UsernameError.None)
                navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
        }
    }
}
