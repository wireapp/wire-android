package com.wire.android.feature.auth.registration.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CreateAccountUsernameViewModel : ViewModel() {

    private val _isValidUsernameLiveData = MutableLiveData<Boolean>()
    val isValidUsernameLiveData: LiveData<Boolean> = _isValidUsernameLiveData
    
    //TODO add proper validation
    fun validateUsername(username: String) {
        _isValidUsernameLiveData.value = true
    }
}
