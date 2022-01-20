package com.wire.android.ui.conversation.call

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CallViewModel : ViewModel() {

    private val _state = MutableStateFlow(CallState())

    val state: StateFlow<CallState>
        get() = _state

    init {
        _state.value = CallState(
            mockMissedCalls,
            mockCallHistory
        )
    }

}
