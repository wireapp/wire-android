package com.wire.android.ui.authentication.login

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterialApi
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager
) : ViewModel() {

    var userIdentifier by mutableStateOf(TextFieldValue(savedStateHandle.get(USER_IDENTIFIER_SAVED_STATE_KEY) ?: String.EMPTY))
        private set

    var password by mutableStateOf(TextFieldValue(String.EMPTY))
        private set

    private val _loginResultLiveData = MutableLiveData<AuthenticationResult>()
    val loginResultLiveData: LiveData<AuthenticationResult> = _loginResultLiveData

    fun login() {
        viewModelScope.launch {
            when (val loginResult = loginUseCase(userIdentifier.text, password.text, true)) {
                is AuthenticationResult.Failure.Generic -> TODO()
                is AuthenticationResult.Failure.InvalidCredentials -> TODO()
                is AuthenticationResult.Failure.InvalidUserIdentifier -> TODO()
                is AuthenticationResult.Success -> _loginResultLiveData.value = loginResult
            }
        }
    }

    fun onUserIdentifierChange(newText: TextFieldValue) {
        userIdentifier = newText
        savedStateHandle.set(USER_IDENTIFIER_SAVED_STATE_KEY, userIdentifier.text)
    }

    fun onPasswordChange(newText: TextFieldValue) {
        password = newText
    }

    suspend fun navigateToConvScreen() =
        navigationManager.navigate(NavigationCommand(NavigationItem.Home.navigationRoute(), BackStackMode.CLEAR_WHOLE))

    private companion object {
        const val USER_IDENTIFIER_SAVED_STATE_KEY = "user_identifier"
    }
}
