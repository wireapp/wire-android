package com.wire.android.ui.conversation.all

import androidx.lifecycle.ViewModel
import com.wire.android.ui.conversation.conversationMockData
import com.wire.android.ui.conversation.newActivitiesMockData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AllConversationViewModel : ViewModel() {

    private val _state = MutableStateFlow(AllConversationState())

    val stateAll: StateFlow<AllConversationState>
        get() = _state

    init {
        _state.value = AllConversationState(
            newActivities = newActivitiesMockData,
            conversations = conversationMockData
        )
    }

}
