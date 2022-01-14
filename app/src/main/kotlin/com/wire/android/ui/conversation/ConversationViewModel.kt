package com.wire.android.ui.conversation

import androidx.lifecycle.ViewModel
import com.wire.android.ui.conversation.model.Conversation
import com.wire.android.ui.conversation.model.Membership
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

val mockData = listOf(
    Conversation("some test value", Membership.Quest),
    Conversation("some other test value"),
    Conversation("and once more 1", Membership.External),
    Conversation("and once more 2"),
    Conversation("and once more 3", Membership.External),
    Conversation("and once more 4", Membership.External),
)


val mockData1 = listOf(
    Conversation("some test value", Membership.Quest,true),
    Conversation("some other test value", isLegalHold = true),
    Conversation("and once more 1", Membership.External),
    Conversation("and once more 2", isLegalHold = true),
    Conversation("and once more 3", Membership.External),
    Conversation("and once more 4", Membership.External),
)
