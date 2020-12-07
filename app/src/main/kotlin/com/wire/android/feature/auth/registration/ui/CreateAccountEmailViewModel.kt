package com.wire.android.feature.auth.registration.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.extension.failure
import com.wire.android.core.extension.success
import com.wire.android.core.functional.Either
import com.wire.android.core.ui.SingleLiveEvent
import com.wire.android.core.ui.dialog.ErrorMessage
import com.wire.android.core.ui.dialog.GeneralErrorMessage
import com.wire.android.core.ui.dialog.NetworkErrorMessage
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.auth.activation.usecase.EmailBlacklisted
import com.wire.android.feature.auth.activation.usecase.EmailInUse
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeFailure
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeParams
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeUseCase
import com.wire.android.shared.user.email.ValidateEmailError
import com.wire.android.shared.user.email.ValidateEmailParams
import com.wire.android.shared.user.email.ValidateEmailUseCase

class CreateAccountEmailViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val sendActivationUseCase: SendEmailActivationCodeUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _confirmationButtonEnabledLiveData = MutableLiveData<Boolean>()
    val confirmationButtonEnabled: LiveData<Boolean> = _confirmationButtonEnabledLiveData

    private val _emailValidationLiveData = MutableLiveData<ErrorMessage>()
    val emailValidationErrorLiveData: LiveData<ErrorMessage> = _emailValidationLiveData

    private val _sendActivationCodeLiveData = SingleLiveEvent<Either<ErrorMessage, String>>()
    val sendActivationCodeLiveData: LiveData<Either<ErrorMessage, String>> = _sendActivationCodeLiveData

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
        _confirmationButtonEnabledLiveData.value = status
    }

    fun sendActivationCode(email: String) =
        sendActivationUseCase(viewModelScope, SendEmailActivationCodeParams(email)) {
            it.fold(::sendActivationCodeFailure) { sendActivationCodeSuccess(email) }
        }

    private fun sendActivationCodeSuccess(email: String) = _sendActivationCodeLiveData.success(email)

    private fun sendActivationCodeFailure(failure: Failure) {
        when (failure) {
            is SendEmailActivationCodeFailure -> handleActivationErrors(failure)
            else -> handleGeneralErrors(failure)
        }
    }

    private fun handleGeneralErrors(failure: Failure) {
        val errorMessage = when (failure) {
            is NetworkConnection -> NetworkErrorMessage
            else -> GeneralErrorMessage
        }
        _sendActivationCodeLiveData.failure(errorMessage)
    }

    private fun handleActivationErrors(failure: SendEmailActivationCodeFailure) {
        val errorMessage = when (failure) {
            is EmailBlacklisted ->
                ErrorMessage(R.string.create_personal_account_with_email_email_blacklisted_error)
            is EmailInUse ->
                ErrorMessage(R.string.create_personal_account_with_email_email_in_use_error)
        }
        _emailValidationLiveData.value = errorMessage
    }
}
