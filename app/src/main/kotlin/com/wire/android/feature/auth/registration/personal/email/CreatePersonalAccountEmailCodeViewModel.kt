package com.wire.android.feature.auth.registration.personal.email

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.exception.ErrorMessage
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.extension.failure
import com.wire.android.core.extension.success
import com.wire.android.core.functional.Either
import com.wire.android.core.ui.SingleLiveEvent
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.auth.registration.personal.email.usecase.ActivateEmailParams
import com.wire.android.feature.auth.registration.personal.email.usecase.ActivateEmailUseCase
import com.wire.android.feature.auth.registration.personal.email.usecase.InvalidEmailCode

class CreatePersonalAccountEmailCodeViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val activateEmailUseCase: ActivateEmailUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _networkConnectionErrorLiveData = SingleLiveEvent<Unit>()
    val networkConnectionErrorLiveData: LiveData<Unit> = _networkConnectionErrorLiveData

    private val _activateEmailLiveData = SingleLiveEvent<Either<ErrorMessage, String>>()
    val activateEmailLiveData: LiveData<Either<ErrorMessage, String>> = _activateEmailLiveData

    fun activateEmail(email: String, code: String) = activateEmailUseCase(viewModelScope, ActivateEmailParams(email, code)) {
        it.fold(::activateEmailFailure) { activateEmailSuccess(code) }
    }

    private fun activateEmailSuccess(code: String) = _activateEmailLiveData.success(code)

    private fun activateEmailFailure(failure: Failure) {
        when (failure) {
            is NetworkConnection -> _networkConnectionErrorLiveData.value = Unit
            is InvalidEmailCode -> _activateEmailLiveData.failure(
                ErrorMessage(R.string.create_personal_account_email_code_invalid_code_error)
            )
        }
    }
}
