package com.wire.android.feature.auth.registration.personal.email

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.functional.Either
import com.wire.android.core.ui.event.Event
import com.wire.android.core.ui.event.MutableEvent
import com.wire.android.core.ui.event.failureEvent
import com.wire.android.core.ui.event.successEvent
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.auth.activation.usecase.EmailBlacklisted
import com.wire.android.feature.auth.activation.usecase.EmailInUse
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeParams
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeUseCase
import com.wire.android.shared.user.email.ValidateEmailError
import com.wire.android.shared.user.email.ValidateEmailParams
import com.wire.android.shared.user.email.ValidateEmailUseCase
import kotlinx.coroutines.Dispatchers

class CreatePersonalAccountEmailViewModel(
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val sendActivationUseCase: SendEmailActivationCodeUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor() {

    private val _isValidEmailLiveData = MutableLiveData<Boolean>()
    val isValidEmailLiveData: LiveData<Boolean> = _isValidEmailLiveData

    private val _sendActivationCodeEvent = MutableEvent<Either<ErrorMessage, String>>()
    val sendActivationCodeEvent: Event<Either<ErrorMessage, String>> = _sendActivationCodeEvent

    private val _networkConnectionErrorLiveData = MutableLiveData<Unit>()
    val networkConnectionErrorLiveData: LiveData<Unit> = _networkConnectionErrorLiveData

    fun validateEmail(email: String) =
        validateEmailUseCase(viewModelScope, ValidateEmailParams(email), Dispatchers.Default) {
            it.fold(::validateEmailFailure) { updateEmailValidationStatus(true) }
        }

    private fun validateEmailFailure(failure: Failure) {
        if (failure is ValidateEmailError) {
            updateEmailValidationStatus(false)
        }
    }

    private fun updateEmailValidationStatus(status: Boolean) {
        _isValidEmailLiveData.value = status
    }

    fun sendActivationCode(email: String) =
        sendActivationUseCase(viewModelScope, SendEmailActivationCodeParams(email)) {
            it.fold(::sendActivationCodeFailure) { sendActivationCodeSuccess(email) }
        }

    private fun sendActivationCodeSuccess(email: String) = _sendActivationCodeEvent.successEvent(email)

    private fun sendActivationCodeFailure(failure: Failure) {
        when (failure) {
            is NetworkConnection -> _networkConnectionErrorLiveData.value = Unit

            is EmailBlacklisted -> _sendActivationCodeEvent.failureEvent(
                ErrorMessage(R.string.create_personal_account_with_email_email_blacklisted_error)
            )
            is EmailInUse -> _sendActivationCodeEvent.failureEvent(
                ErrorMessage(R.string.create_personal_account_with_email_email_in_use_error)
            )
        }
    }
}

data class ErrorMessage(@StringRes val message: Int)
