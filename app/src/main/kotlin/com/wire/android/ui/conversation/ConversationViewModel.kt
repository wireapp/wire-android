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
            newConservationActivities = newActivitiesMockData,
            conversations = conversationMockData
        )
    }

}
