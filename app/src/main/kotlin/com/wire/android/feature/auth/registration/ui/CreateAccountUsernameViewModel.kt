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
import com.wire.android.shared.user.username.CheckUsernameError
import com.wire.android.shared.user.username.CheckUsernameExistsParams
import com.wire.android.shared.user.username.CheckUsernameExistsUseCase
import com.wire.android.shared.user.username.GenerateRandomUsernameUseCase
import com.wire.android.shared.user.username.UpdateUsernameParams
import com.wire.android.shared.user.username.UpdateUsernameUseCase
import com.wire.android.shared.user.username.UsernameAlreadyExists
import com.wire.android.shared.user.username.UsernameGeneralError
import com.wire.android.shared.user.username.UsernameInvalid
import com.wire.android.shared.user.username.UsernameTooLong
import com.wire.android.shared.user.username.UsernameTooShort
import com.wire.android.shared.user.username.ValidateUsernameError
import com.wire.android.shared.user.username.ValidateUsernameParams
import com.wire.android.shared.user.username.ValidateUsernameUseCase
import kotlinx.coroutines.runBlocking

class CreateAccountUsernameViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val validateUsernameUseCase: ValidateUsernameUseCase,
    private val checkUsernameExistsUseCase: CheckUsernameExistsUseCase,
    private val updateUsernameUseCase: UpdateUsernameUseCase,
    private val generateRandomUsernameUseCase: GenerateRandomUsernameUseCase,
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _confirmationButtonEnabled = MutableLiveData<Boolean>()
    val confirmationButtonEnabled: LiveData<Boolean> = _confirmationButtonEnabled

    private val _usernameValidationLiveData = MutableLiveData<Either<ErrorMessage, Unit>>()
    val usernameValidationLiveData: LiveData<Either<ErrorMessage, Unit>> = _usernameValidationLiveData

    private val _generatedUsernameLiveData = MutableLiveData<String>()
    val generatedUsernameLiveData: LiveData<String> = _generatedUsernameLiveData

    private val _dialogErrorLiveData = SingleLiveEvent<ErrorMessage>()
    val dialogErrorLiveData: LiveData<ErrorMessage> = _dialogErrorLiveData

    fun validateUsername(username: String) = runBlocking {
        val params = ValidateUsernameParams(username)
        validateUsernameUseCase(viewModelScope, params) {
            it.fold(::handleFailure) {
                updateConfirmationButtonStatus(true)
                _usernameValidationLiveData.failure(ErrorMessage.EMPTY)
            }
        }
    }

    fun onConfirmationButtonClicked(username: String) = runBlocking {
        val params = CheckUsernameExistsParams(username)
        checkUsernameExistsUseCase(viewModelScope, params) {
            it.fold(::handleFailure, ::checkUsernameSuccess)
        }
    }

    fun generateUsername() = runBlocking {
        generateRandomUsernameUseCase(viewModelScope, Unit) {
            it.fold(::handleFailure, ::generateUsernameSuccess)
        }
    }

    private fun generateUsernameSuccess(username: String) {
        updateConfirmationButtonStatus(true)
        _generatedUsernameLiveData.value = username
    }

    private fun checkUsernameSuccess(username: String) = runBlocking {
        updateConfirmationButtonStatus(true)
        updateUsernameUseCase(viewModelScope, UpdateUsernameParams(username)) {
            it.fold(::handleFailure) { updateUsernameSuccess() }
        }
    }

    private fun updateUsernameSuccess() {
        _usernameValidationLiveData.success()
    }

    private fun handleFailure(failure: Failure) {
        updateConfirmationButtonStatus(false)
        when (failure) {
            is ValidateUsernameError -> handleUsernameValidationErrors(failure)
            is CheckUsernameError -> handleCheckUsernameExistsErrors(failure)
            else -> handleGeneralErrors(failure)
        }
    }

    private fun handleCheckUsernameExistsErrors(failure: CheckUsernameError) {
        val errorMessage = when (failure) {
            UsernameAlreadyExists -> ErrorMessage(R.string.create_account_with_username_error_already_taken)
            UsernameGeneralError -> GeneralErrorMessage
        }
        _usernameValidationLiveData.failure(errorMessage)
    }

    private fun handleGeneralErrors(failure: Failure) {
        val errorMessage = when (failure) {
            is NetworkConnection -> NetworkErrorMessage
            else -> GeneralErrorMessage
        }
        _dialogErrorLiveData.value = errorMessage
    }

    private fun handleUsernameValidationErrors(failure: ValidateUsernameError) {
        val errorMessage = when (failure) {
            UsernameTooShort -> ErrorMessage(R.string.create_account_with_username_error_too_short)
            UsernameInvalid -> ErrorMessage(R.string.create_account_with_username_error_invalid_characters)
            UsernameTooLong -> ErrorMessage(R.string.create_account_with_username_error_too_long)
        }
        _usernameValidationLiveData.failure(errorMessage)
    }

    private fun updateConfirmationButtonStatus(enabled: Boolean) {
        _confirmationButtonEnabled.value = enabled
    }
}
