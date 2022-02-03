package com.wire.android.ui.home.conversationslist

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversationslist.mock.conversationMockData
import com.wire.android.ui.home.conversationslist.mock.mockAllMentionList
import com.wire.android.ui.home.conversationslist.mock.mockCallHistory
import com.wire.android.ui.home.conversationslist.mock.mockMissedCalls
import com.wire.android.ui.home.conversationslist.mock.mockUnreadMentionList
import com.wire.android.ui.home.conversationslist.mock.newActivitiesMockData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterial3Api
@Suppress("MagicNumber")
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val navigationManager: NavigationManager
) : ViewModel() {

    private val _state = MutableStateFlow(ConversationListState())

    val state: StateFlow<ConversationListState>
        get() = _state

    init {
        _state.value = ConversationListState(
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

    fun openConversation(conversationId: String) {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.Conversation.createRoute(conversationId = conversationId)
                )
            )
        }
    }

    fun muteConversation(id: String) {

    }

    fun addConversationToFavourites(id: String) {

    }

    fun moveConversationToFolder(id: String) {

    }

    fun moveConversationToArchive(id: String) {

    }

    fun clearConversationContent(id: String) {

    }

    fun blockUser(id: String) {

    }

    fun leaveGroup(id: String) {

    }

}
