package com.wire.android.feature.auth.registration.personal.email

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.ui.dialog.ErrorMessage
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.extension.failure
import com.wire.android.core.extension.success
import com.wire.android.core.functional.Either
import com.wire.android.core.ui.SingleLiveEvent
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.auth.activation.usecase.EmailBlacklisted
import com.wire.android.feature.auth.activation.usecase.EmailInUse
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeParams
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeUseCase
import com.wire.android.shared.user.email.ValidateEmailError
import com.wire.android.shared.user.email.ValidateEmailParams
import com.wire.android.shared.user.email.ValidateEmailUseCase

class CreatePersonalAccountEmailViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val sendActivationUseCase: SendEmailActivationCodeUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _isValidEmailLiveData = MutableLiveData<Boolean>()
    val isValidEmailLiveData: LiveData<Boolean> = _isValidEmailLiveData

    private val _sendActivationCodeLiveData = SingleLiveEvent<Either<ErrorMessage, String>>()
    val sendActivationCodeLiveData: LiveData<Either<ErrorMessage, String>> = _sendActivationCodeLiveData

    private val _networkConnectionErrorLiveData = MutableLiveData<Unit>()
    val networkConnectionErrorLiveData: LiveData<Unit> = _networkConnectionErrorLiveData

    fun validateEmail(email: String) =
        validateEmailUseCase(viewModelScope, ValidateEmailParams(email), dispatcherProvider.default()) {
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

    private fun sendActivationCodeSuccess(email: String) = _sendActivationCodeLiveData.success(email)

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
