package com.wire.android.ui.main.conversationlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

        viewModelScope.launch {
            delay(2000)
            _state.update {
                Log.d("TEST", "updating after 2000")
                it.copy(newActivities = emptyList(), conversations = emptyMap(),unreadMentionsCount = 5,missedCallsCount = 20)
            }
        }

        viewModelScope.launch {
            delay(6000)
            _state.update {
                Log.d("TEST", "updating after 6000")
                it.copy(conversations = com.wire.android.ui.main.conversationlist.conversationMockData1,unreadMentionsCount = 50)
            }
        }
    }

}
