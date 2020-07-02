package com.wire.android.feature.auth.registration.personal

import androidx.lifecycle.ViewModel
import com.wire.android.core.accessibility.AccessibilityManager

class CreatePersonalAccountViewModel(private val accessibilityManagerWrapper: AccessibilityManager) : ViewModel() {

    fun shouldShowKeyboard() = !accessibilityManagerWrapper.isTalkbackEnabled()
}
