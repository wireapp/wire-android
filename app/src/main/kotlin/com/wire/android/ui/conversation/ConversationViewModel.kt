package com.wire.android.ui.conversation

import androidx.lifecycle.ViewModel
import com.wire.android.ui.conversation.model.AvailabilityStatus
import com.wire.android.ui.conversation.model.Conversation
import com.wire.android.ui.conversation.model.ConversationInfo
import com.wire.android.ui.conversation.model.Membership
import com.wire.android.ui.conversation.model.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ConversationViewModel : ViewModel() {

    private val _state = MutableStateFlow(ConversationState())

    val state: StateFlow<ConversationState>
        get() = _state

    init {
        _state.value = ConversationState(
            conversations = mockData
        )
    }

}
