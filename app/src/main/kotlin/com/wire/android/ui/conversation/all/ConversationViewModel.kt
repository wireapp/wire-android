package com.wire.android.ui.conversation.all

import androidx.lifecycle.ViewModel
import com.wire.android.ui.conversation.conversationMockData
import com.wire.android.ui.conversation.newActivitiesMockData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ConversationViewModel : ViewModel() {

    private val _state = MutableStateFlow(ConversationState())

    val state: StateFlow<ConversationState>
        get() = _state

    init {
        _state.value = ConversationState(
            newActivities = newActivitiesMockData,
            conversations = conversationMockData
        )
    }

}
