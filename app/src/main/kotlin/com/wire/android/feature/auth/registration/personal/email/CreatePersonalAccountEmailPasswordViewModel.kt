package com.wire.android.feature.auth.registration.personal.email

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.core.exception.ErrorMessage
import com.wire.android.core.extension.success
import com.wire.android.core.functional.Either
import com.wire.android.core.ui.SingleLiveEvent
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.shared.user.password.ValidatePasswordParams
import com.wire.android.shared.user.password.ValidatePasswordUseCase

class CreatePersonalAccountEmailPasswordViewModel(
    private val validatePasswordUseCase: ValidatePasswordUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor() {

    private val _continueEnabledLiveData = SingleLiveEvent<Boolean>()
    val continueEnabledLiveData: LiveData<Boolean> = _continueEnabledLiveData

    private val _networkConnectionErrorLiveData = SingleLiveEvent<Unit>()
    val networkConnectionErrorLiveData: LiveData<Unit> = _networkConnectionErrorLiveData

    private val _registerStatusLiveData = SingleLiveEvent<Either<ErrorMessage, Unit>>()
    val registerStatusLiveData: LiveData<Either<ErrorMessage, Unit>> = _registerStatusLiveData

    fun minPasswordLength() = validatePasswordUseCase.minLength()

    fun validatePassword(password: String) =
        validatePasswordUseCase(viewModelScope, ValidatePasswordParams(password)) {
            _continueEnabledLiveData.value = it.isRight
        }

    fun registerUser(name: String, email: String, password: String, activationCode: String) {
        //TODO registration via use case
        _registerStatusLiveData.success()
    }
}
