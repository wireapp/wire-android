package com.wire.android.feature.auth.registration.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CreateAccountUsernameViewModel : ViewModel() {

    private val _confirmationButtonEnabled = MutableLiveData<Boolean>()
    val confirmationButtonEnabled: LiveData<Boolean> = _confirmationButtonEnabled

    //TODO add proper validation
    fun validateUsername(username: String) {
        _confirmationButtonEnabled.value = true
    }
}
