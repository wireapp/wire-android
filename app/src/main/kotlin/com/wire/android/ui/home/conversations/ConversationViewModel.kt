package com.wire.android.ui.home.conversations

import androidx.lifecycle.ViewModel
import com.wire.android.ui.home.conversations.mock.conversationMockData
import com.wire.android.ui.home.conversations.mock.mockAllMentionList
import com.wire.android.ui.home.conversations.mock.mockCallHistory
import com.wire.android.ui.home.conversations.mock.mockMissedCalls
import com.wire.android.ui.home.conversations.mock.mockUnreadMentionList
import com.wire.android.ui.home.conversations.mock.newActivitiesMockData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Suppress("MagicNumber")
@HiltViewModel
class ConversationViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ConversationState())

    val state: StateFlow<ConversationState>
        get() = _state

    init {
        _state.value = ConversationState(
            newActivities = newActivitiesMockData,
            conversations = conversationMockData,
            missedCalls = mockMissedCalls,
            callHistory = mockCallHistory,
            unreadMentions = mockUnreadMentionList,
            allMentions = mockAllMentionList,
            unreadMentionsCount = 12,
            missedCallsCount = 100,
            newActivityCount = 1
        )
    }

}
