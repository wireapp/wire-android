package com.wire.android.feature.auth.registration.personal.email

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.wire.android.core.exception.ErrorMessage
import com.wire.android.core.extension.success
import com.wire.android.core.functional.Either
import com.wire.android.core.ui.SingleLiveEvent

class CreatePersonalAccountEmailPasswordViewModel : ViewModel() {

    private val _continueEnabledLiveData = SingleLiveEvent<Boolean>()
    val continueEnabledLiveData: LiveData<Boolean> = _continueEnabledLiveData

    private val _networkConnectionErrorLiveData = SingleLiveEvent<Unit>()
    val networkConnectionErrorLiveData: LiveData<Unit> = _networkConnectionErrorLiveData

    private val _registerStatusLiveData = SingleLiveEvent<Either<ErrorMessage, Unit>>()
    val registerStatusLiveData: LiveData<Either<ErrorMessage, Unit>> = _registerStatusLiveData

    fun validatePassword(password: String) {
        //TODO validation via use case
        _continueEnabledLiveData.value = password.length > 2
    }

    fun registerUser(name: String, email: String, password: String, activationCode: String) {
        //TODO registration via use case
        _registerStatusLiveData.success()
    }
}
