package com.wire.android.ui.conversation.mention

import androidx.lifecycle.ViewModel
import com.wire.android.ui.conversation.mockAllMentionList
import com.wire.android.ui.conversation.mockUnreadMentionList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MentionViewModel : ViewModel() {

    private val _state = MutableStateFlow(MentionState())

    val state: StateFlow<MentionState>
        get() = _state

    init {
        _state.value = MentionState(
            unreadMentions = mockUnreadMentionList,
            allMentions = mockAllMentionList
        )
    }

}
