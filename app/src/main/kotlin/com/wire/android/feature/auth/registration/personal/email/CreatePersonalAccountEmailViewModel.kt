package com.wire.android.feature.auth.registration.personal.email

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.extension.failure
import com.wire.android.core.extension.success
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.executors.OneShotUseCaseExecutor
import com.wire.android.feature.auth.activation.usecase.EmailBlacklisted
import com.wire.android.feature.auth.activation.usecase.EmailInUse
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeParams
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeUseCase
import com.wire.android.shared.user.email.ValidateEmailError
import com.wire.android.shared.user.email.ValidateEmailParams
import com.wire.android.shared.user.email.ValidateEmailUseCase
import kotlinx.coroutines.runBlocking

class CreatePersonalAccountEmailViewModel(
        private val oneShotExecutor: OneShotUseCaseExecutor,
        private val validateEmailUseCase: ValidateEmailUseCase,
        private val sendActivationUseCase: SendEmailActivationCodeUseCase
) : ViewModel() {

    private val _isValidEmailLiveData = MutableLiveData<Boolean>()
    private val _sendActivationCodeLiveData = MutableLiveData<Either<ErrorMessage, Unit>>()
    private val _networkConnectionErrorLiveData = MutableLiveData<Unit>()

    val isValidEmailLiveData: LiveData<Boolean> = _isValidEmailLiveData
    val sendActivationCodeLiveData: LiveData<Either<ErrorMessage, Unit>> =
        _sendActivationCodeLiveData
    val networkConnectionErrorLiveData: LiveData<Unit> = _networkConnectionErrorLiveData

    fun validateEmail(email: String) = runBlocking {
        oneShotExecutor.execute(validateEmailUseCase, ValidateEmailParams(email), viewModelScope) {
            it.fold(::validateEmailFailure) { updateEmailValidationStatus(true) }
        }
    }

    private fun validateEmailFailure(failure: Failure) {
        if (failure is ValidateEmailError) {
            updateEmailValidationStatus(false)
        }
    }

    private fun updateEmailValidationStatus(status: Boolean) {
        _isValidEmailLiveData.value = status
    }

    fun sendActivationCode(email: String) = runBlocking {
        oneShotExecutor.execute(sendActivationUseCase, SendEmailActivationCodeParams(email), viewModelScope) {
            it.fold(::sendActivationCodeFailure) { sendActivationCodeSuccess() }
        }
    }

    private fun sendActivationCodeSuccess() = _sendActivationCodeLiveData.success()

    private fun sendActivationCodeFailure(failure: Failure) {
        when (failure) {
            is NetworkConnection -> _networkConnectionErrorLiveData.value = Unit

            is EmailBlacklisted -> _sendActivationCodeLiveData.failure(
                ErrorMessage(R.string.create_personal_account_with_email_email_blacklisted_error)
            )
            is EmailInUse -> _sendActivationCodeLiveData.failure(
                ErrorMessage(R.string.create_personal_account_with_email_email_in_use_error)
            )
        }
    }
}

data class ErrorMessage(@StringRes val message: Int)
