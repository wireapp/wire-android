package com.wire.android.ui.home.conversationslist

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.HomeCommonManager
import com.wire.android.ui.home.conversationslist.mock.conversationMockData
import com.wire.android.ui.home.conversationslist.mock.mockAllMentionList
import com.wire.android.ui.home.conversationslist.mock.mockCallHistory
import com.wire.android.ui.home.conversationslist.mock.mockMissedCalls
import com.wire.android.ui.home.conversationslist.mock.mockUnreadMentionList
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.UserInfo
import com.wire.android.util.getConversationColor
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationId
import com.wire.kalium.logic.feature.conversation.GetConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterial3Api
@Suppress("MagicNumber")
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val getConversations: GetConversationsUseCase,
    homeCommonManager: HomeCommonManager
) : ViewModel() {

    private val scrollBridge = homeCommonManager.scrollBridge!!

    fun updateScrollPosition(newScrollIndex: Int) {
        viewModelScope.launch {
            scrollBridge.updateScrollPosition(newScrollIndex)
        }
    }

    var state by mutableStateOf(ConversationListState())
        private set

    init {
        viewModelScope.launch {
            getConversations()
                .collect { conversations ->
                    state = ConversationListState(
                        newActivities = listOf(),
                        conversations = conversationMockData(conversations.toGeneralConversationList()),
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
    }

    fun openConversation(conversationId: ConversationId) {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.Conversation.getRoute(extraRouteId = "${conversationId.domain}/${conversationId.value}")
                )
            )
        }
    }

    //TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun muteConversation(id: String) {

    }

    //TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun addConversationToFavourites(id: String) {

    }

    //TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun moveConversationToFolder(id: String) {

    }

    //TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun moveConversationToArchive(id: String) {

    }

    //TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun clearConversationContent(id: String) {

    }

    //TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun blockUser(id: String) {

    }

    //TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun leaveGroup(id: String) {

    }

    private fun List<Conversation>.toGeneralConversationList() = map { conversation ->
        if (isPrivateChat(conversation)) {
            GeneralConversation(
                ConversationType.PrivateConversation(
                    userInfo = UserInfo(),
                    conversationInfo = ConversationInfo(
                        name = "Some private chat",
                        membership = Membership.None,
                        isLegalHold = true
                    ),
                    conversationId = conversation.id
                )
            )
        } else {
            GeneralConversation(
                ConversationType.GroupConversation(
                    groupColorValue = getConversationColor(conversation.id),
                    groupName = conversation.name!!,
                    conversationId = conversation.id
                )
            )
        }
    }

    //TODO
    private fun isPrivateChat(conversation: Conversation) = conversation.name.isNullOrEmpty()

}
