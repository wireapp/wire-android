package com.wire.android.feature.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.wire.android.feature.auth.login.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import androidx.lifecycle.viewModelScope
import com.wire.android.util.EMPTY
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userIdentifier = mutableStateOf(
        savedStateHandle.get(USER_IDENTIFIER_SAVED_STATE_KEY) ?: String.EMPTY
    )
    fun onUserIdentifierChange(newText: String) {
        userIdentifier.value = newText
        savedStateHandle.set(USER_IDENTIFIER_SAVED_STATE_KEY, userIdentifier.value)
    }

    private val password = mutableStateOf(String.EMPTY)
    fun onPasswordChange(newText: String) {
        password.value = newText
    }

    private val _loginResultLiveData = MutableLiveData<AuthenticationResult>()
    val loginResultLiveData: LiveData<AuthenticationResult> = _loginResultLiveData

    fun login() {
        viewModelScope.launch {
            when (val loginResult = loginUseCase(userIdentifier.value, password.value)) {
                is AuthenticationResult.Failure.Generic -> TODO()
                is AuthenticationResult.Failure.InvalidCredentials -> TODO()
                is AuthenticationResult.Failure.InvalidUserIdentifier -> TODO()
                is AuthenticationResult.Success -> _loginResultLiveData.value = loginResult
            }
        }
    }

    private companion object {
        const val USER_IDENTIFIER_SAVED_STATE_KEY = "user_identifier"
    }

}
