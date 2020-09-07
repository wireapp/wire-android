package com.wire.android.feature.auth.registration.personal.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.extension.failure
import com.wire.android.core.extension.success
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.ui.SingleLiveEvent
import com.wire.android.core.ui.dialog.ErrorMessage
import com.wire.android.core.ui.dialog.GeneralErrorMessage
import com.wire.android.core.ui.dialog.NetworkErrorMessage
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.auth.registration.personal.usecase.EmailInUse
import com.wire.android.feature.auth.registration.personal.usecase.InvalidEmailActivationCode
import com.wire.android.feature.auth.registration.personal.usecase.RegisterPersonalAccountParams
import com.wire.android.feature.auth.registration.personal.usecase.RegisterPersonalAccountUseCase
import com.wire.android.feature.auth.registration.personal.usecase.UnauthorizedEmail
import com.wire.android.shared.user.password.ValidatePasswordParams
import com.wire.android.shared.user.password.ValidatePasswordUseCase

class CreatePersonalAccountPasswordViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val registerUseCase: RegisterPersonalAccountUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _continueEnabledLiveData = SingleLiveEvent<Boolean>()
    val continueEnabledLiveData: LiveData<Boolean> = _continueEnabledLiveData

    private val _registerStatusLiveData = SingleLiveEvent<Either<ErrorMessage, Unit>>()
    val registerStatusLiveData: LiveData<Either<ErrorMessage, Unit>> = _registerStatusLiveData

    fun minPasswordLength() = validatePasswordUseCase.minLength()

    fun validatePassword(password: String) =
        validatePasswordUseCase(viewModelScope, ValidatePasswordParams(password)) {
            _continueEnabledLiveData.value = it.isRight
        }

    fun registerUser(name: String, email: String, password: String, code: String) =
        registerUseCase(
            viewModelScope,
            RegisterPersonalAccountParams(name = name, email = email, password = password, activationCode = code)
        ) {
            it.onSuccess {
                _registerStatusLiveData.success()
            }.onFailure {
                val errorMessage = when (it) {
                    is NetworkConnection -> NetworkErrorMessage
                    is UnauthorizedEmail -> ErrorMessage(R.string.create_personal_account_unauthorized_email_error)
                    is InvalidEmailActivationCode -> ErrorMessage(R.string.create_personal_account_invalid_activation_code_error)
                    is EmailInUse -> ErrorMessage(R.string.create_personal_account_email_in_use_error)
                    else -> GeneralErrorMessage
                }
                _registerStatusLiveData.failure(errorMessage)
            }
        }
}
