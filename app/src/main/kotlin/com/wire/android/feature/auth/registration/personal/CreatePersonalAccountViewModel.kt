package com.wire.android.feature.auth.registration.personal

import androidx.lifecycle.ViewModel
import com.wire.android.core.accessibility.Accessibility

class CreatePersonalAccountViewModel(private val accessibility: Accessibility) : ViewModel() {

    fun shouldShowKeyboard() = !accessibility.isTalkbackEnabled()
}
