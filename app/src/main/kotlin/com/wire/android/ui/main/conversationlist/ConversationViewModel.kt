package com.wire.android.ui.main.conversationlist

import androidx.lifecycle.ViewModel
import com.wire.android.ui.main.conversationlist.mock.conversationMockData
import com.wire.android.ui.main.conversationlist.mock.mockAllMentionList
import com.wire.android.ui.main.conversationlist.mock.mockCallHistory
import com.wire.android.ui.main.conversationlist.mock.mockMissedCalls
import com.wire.android.ui.main.conversationlist.mock.mockUnreadMentionList
import com.wire.android.ui.main.conversationlist.mock.newActivitiesMockData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Suppress("MagicNumber")
class ConversationViewModel : ViewModel() {

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
