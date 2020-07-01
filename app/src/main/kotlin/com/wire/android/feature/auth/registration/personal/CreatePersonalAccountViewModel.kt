package com.wire.android.feature.auth.registration.personal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wire.android.core.accessibility.AccessibilityManager

class CreatePersonalAccountViewModel(
        private val accessibilityManagerWrapper: AccessibilityManager
) : ViewModel() {

    private val _keyboardDisplayLiveData = MutableLiveData<Unit>()
    val keyboardDisplayLiveData: LiveData<Unit> = _keyboardDisplayLiveData

    init {
        shouldShowKeyboard()
    }

    private fun shouldShowKeyboard() {
        if (!accessibilityManagerWrapper.isTalkbackEnabled()) {
            _keyboardDisplayLiveData.value = Unit
        }
    }
}
