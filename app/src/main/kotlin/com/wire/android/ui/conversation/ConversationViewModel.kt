package com.wire.android.ui.conversation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ConversationViewModel : ViewModel() {

    private val _state = MutableStateFlow(ConversationState())

    val state: StateFlow<ConversationState>
        get() = _state

    init {
        _state.value = ConversationState(
            conversations = listOf(
                Conversation("some test value"),
                Conversation("some other test value"),
                Conversation("and once more")
            )
        )
    }

}
