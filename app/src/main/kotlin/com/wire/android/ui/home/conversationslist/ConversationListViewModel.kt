package com.wire.android.ui.home.conversationslist

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.model.UserAvatarAsset
import com.wire.android.model.UserStatus
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
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
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationDetails.Group
import com.wire.kalium.logic.data.conversation.ConversationDetails.OneOne
import com.wire.kalium.logic.data.conversation.ConversationDetails.Self
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@ExperimentalMaterial3Api
@Suppress("MagicNumber")
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val observeConversationDetailsList: ObserveConversationListDetailsUseCase,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase
) : ViewModel() {

    var state by mutableStateOf(ConversationListState())
        private set

    init {
        viewModelScope.launch {
            observeConversationDetailsList().collect { detailedList ->
                state = ConversationListState(
                    newActivities = listOf(),
                    conversations = conversationMockData(detailedList.toGeneralConversationList()),
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
                    destination = NavigationItem.Conversation.getRouteWithArgs(listOf(conversationId))
                )
            )
        }
    }

    fun openNewConversation() {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.NewConversation.getRouteWithArgs()
                )
            )
        }
    }

    fun muteConversation(conversationId: ConversationId?, mutedConversationStatus: MutedConversationStatus) {
        conversationId?.let {
            viewModelScope.launch {
                appLogger.d("Muting conversation: $conversationId")
                updateConversationMutedStatus(conversationId, mutedConversationStatus, Date().time)
            }
        }
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun addConversationToFavourites(id: String) {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun moveConversationToFolder(id: String) {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun moveConversationToArchive(id: String) {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun clearConversationContent(id: String) {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun blockUser(id: String) {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun leaveGroup(id: String) {
    }

    private fun List<ConversationDetails>.toGeneralConversationList(): List<GeneralConversation> = filter {
        it is Group || it is OneOne
    }.map { details ->
        val conversation = details.conversation
        when (details) {
            is Group -> {
                GeneralConversation(
                    ConversationType.GroupConversation(
                        groupColorValue = getConversationColor(conversation.id),
                        groupName = conversation.name.orEmpty(),
                        conversationId = conversation.id,
                        mutedStatus = conversation.mutedStatus
                    )
                )
            }
            is OneOne -> {
                val otherUser = details.otherUser
                GeneralConversation(
                    ConversationType.PrivateConversation(
                        userInfo = UserInfo(
                            otherUser.previewPicture?.let { UserAvatarAsset(it) },
                            UserStatus.NONE // TODO Get actual status
                        ),
                        conversationInfo = ConversationInfo(
                            name = otherUser.name.orEmpty(),
                            membership = Membership.None,
                            isLegalHold = true
                        ),
                        conversationId = conversation.id,
                        mutedStatus = conversation.mutedStatus
                    )
                )
            }
            is Self -> {
                throw IllegalArgumentException("Self conversations should not be visible to the user.")
            }
        }
    }
}
