package com.wire.android.core.accessibility

import androidx.lifecycle.ViewModel

class InputFocusViewModel(private val accessibility: Accessibility) : ViewModel() {

    fun canFocusWithKeyboard() = !accessibility.isTalkbackEnabled()
}
