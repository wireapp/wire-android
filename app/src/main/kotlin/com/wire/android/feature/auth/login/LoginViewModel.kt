package com.wire.android.feature.auth.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
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

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager
) : ViewModel() {

    var userIdentifier by mutableStateOf(
        savedStateHandle.get(USER_IDENTIFIER_SAVED_STATE_KEY) ?: String.EMPTY
    )
        private set

    var password by mutableStateOf(String.EMPTY)
        private set

    private val _loginResultLiveData = MutableLiveData<AuthenticationResult>()
    val loginResultLiveData: LiveData<AuthenticationResult> = _loginResultLiveData

    fun login() {
        viewModelScope.launch {
            when (val loginResult = loginUseCase(userIdentifier, password, true)) {
                is AuthenticationResult.Failure.Generic -> TODO()
                is AuthenticationResult.Failure.InvalidCredentials -> TODO()
                is AuthenticationResult.Failure.InvalidUserIdentifier -> TODO()
                is AuthenticationResult.Success -> _loginResultLiveData.value = loginResult
            }
        }
    }

    fun onUserIdentifierChange(newText: String) {
        userIdentifier = newText
        savedStateHandle.set(USER_IDENTIFIER_SAVED_STATE_KEY, userIdentifier)
    }

    fun onPasswordChange(newText: String) {
        password = newText
    }

    suspend fun navigateToConvScreen() =
        navigationManager.navigate(NavigationCommand(NavigationItem.Home.navigationRoute(), BackStackMode.CLEAR_WHOLE))

    private companion object {
        const val USER_IDENTIFIER_SAVED_STATE_KEY = "user_identifier"
    }
}
