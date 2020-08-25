package com.wire.android.feature.auth.login.email.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.core.exception.Failure
import com.wire.android.core.extension.failure
import com.wire.android.core.extension.success
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.ui.SingleLiveEvent
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.auth.login.email.usecase.LoginWithEmailUseCase
import com.wire.android.feature.auth.login.email.usecase.LoginWithEmailUseCaseParams
import com.wire.android.shared.user.email.ValidateEmailParams
import com.wire.android.shared.user.email.ValidateEmailUseCase
import kotlinx.coroutines.Dispatchers

class LoginWithEmailViewModel(
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val loginWithEmailUseCase: LoginWithEmailUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor() {

    private val isValidEmailLiveData = SingleLiveEvent<Boolean>()
    private val isValidPasswordLiveData = SingleLiveEvent<Boolean>()
    val continueEnabledLiveData: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(isValidEmailLiveData) { value = allInputsAreValid() }
        addSource(isValidPasswordLiveData) { value = allInputsAreValid() }
    }

    private val _loginResultLiveData = SingleLiveEvent<Either<Failure, Unit>>()
    val loginResultLiveData: LiveData<Either<Failure, Unit>> = _loginResultLiveData

    fun validateEmail(email: String) =
        validateEmailUseCase(viewModelScope, ValidateEmailParams(email), Dispatchers.Default) { result ->
            isValidEmailLiveData.value = result.isRight
        }

    fun validatePassword(password: String) {
        isValidPasswordLiveData.value = password.isNotEmpty()
    }

    fun login(email: String, password: String) {
        loginWithEmailUseCase(viewModelScope, LoginWithEmailUseCaseParams(email = email, password = password)) { result ->
            result.onSuccess {
                _loginResultLiveData.success()
            }.onFailure {
                _loginResultLiveData.failure(it)
            }
        }
    }

    private fun allInputsAreValid() =
        isValidEmailLiveData.value ?: false &&
            isValidPasswordLiveData.value ?: false
}
