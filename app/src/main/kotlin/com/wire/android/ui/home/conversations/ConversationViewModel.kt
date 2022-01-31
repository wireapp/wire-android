package com.wire.android.ui.home.conversations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.wire.android.ui.home.conversations.mock.mockMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject


@HiltViewModel
class ConversationViewModel @Inject constructor(
    //TODO: here we can extract the ID provided to the screen and fetch the data for the conversation
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(ConversationState())

    val conversationState: StateFlow<ConversationState>
        get() = _state

    init {
        _state.value = ConversationState(
            conversationName = "Some test conversation",
            messages = mockMessages
        )
    }
}
