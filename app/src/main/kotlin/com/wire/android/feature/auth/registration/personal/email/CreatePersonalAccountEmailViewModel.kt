package com.wire.android.feature.auth.registration.personal.email

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

//TODO: add use cases & layers below
class CreatePersonalAccountEmailViewModel : ViewModel() {

    private val _isValidEmailLiveData = MutableLiveData<Boolean>()

    val isValidEmailLiveData: LiveData<Boolean> = _isValidEmailLiveData

    fun validateEmail(email: String) {
//        validateEmailUseCase(viewModelScope, ValidateEmailParams(email), Dispatchers.Default) {
//            it.fold(::validateEmailFailure) { updateEmailValidationStatus(true) }
//        }
        updateEmailValidationStatus(email.length > 3) //dummy check
    }


    private fun updateEmailValidationStatus(status: Boolean) {
        _isValidEmailLiveData.value = status
    }
}
