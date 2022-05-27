package com.wire.android.ui.home.conversationslist

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserStatus
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversationslist.mock.mockAllMentionList
import com.wire.android.ui.home.conversationslist.mock.mockCallHistory
import com.wire.android.ui.home.conversationslist.mock.mockMissedCalls
import com.wire.android.ui.home.conversationslist.mock.mockNewActivities
import com.wire.android.ui.home.conversationslist.mock.mockUnreadMentionList
import com.wire.android.ui.home.conversationslist.model.ConnectionInfo
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.NewActivity
import com.wire.android.ui.home.conversationslist.model.PendingConnectionItem
import com.wire.android.ui.home.conversationslist.model.UserInfo
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getConversationColor
import com.wire.kalium.logic.data.connection.ConnectionDetails
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationDetails.Group
import com.wire.kalium.logic.data.conversation.ConversationDetails.OneOne
import com.wire.kalium.logic.data.conversation.ConversationDetails.Self
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.conversation.UserType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.ObserveConnectionListUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.message.MarkMessagesAsNotifiedUseCase
import com.wire.kalium.logic.util.toStringDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@ExperimentalMaterial3Api
@Suppress("MagicNumber")
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val observeConversationDetailsList: ObserveConversationListDetailsUseCase,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val markMessagesAsNotified: MarkMessagesAsNotifiedUseCase,
    private val observeConnectionList: ObserveConnectionListUseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    var state by mutableStateOf(ConversationListState())
        private set

    var errorState by mutableStateOf<ConversationOperationErrorState?>(null)

    init {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                combine(
                    observeConversationDetailsList(), //TODO AR-1736
                    observeConnectionList().onStart { emit(listOf()) },
                    ::Pair
                )
                    .collect { (conversations, connections) ->
                        val detailedList = conversations.toConversationsFoldersMap()
                        val newActivities = mockNewActivities
//                            prepareActivities(connections) TODO AR-1733
                        val missedCalls = mockMissedCalls // TODO: needs to be implemented
                        val unreadMentions = mockUnreadMentionList // TODO: needs to be implemented

                        state = ConversationListState(
                            newActivities = newActivities,
                            conversations = detailedList,
                            missedCalls = missedCalls,
                            callHistory = mockCallHistory, // TODO: needs to be implemented
                            unreadMentions = unreadMentions,
                            allMentions = mockAllMentionList, // TODO: needs to be implemented
                            unreadMentionsCount = unreadMentions.size,
                            missedCallsCount = missedCalls.size,
                            newActivityCount = newActivities.size
                        )
                    }
            }
        }

        viewModelScope.launch {
            markMessagesAsNotified(null, System.currentTimeMillis().toStringDate()) // TODO Failure is ignored
        }
    }

    // TODO AR-1733
    private fun prepareActivities(connections: List<ConnectionDetails>) =
        connections.map {
            NewActivity(eventType = EventType.ConnectRequest, it.conversation.toItem(
                private = { privateConversation ->
                    PendingConnectionItem(
                        connectionInfo = ConnectionInfo(
                            it.connection.status.toString(), //TODO pass also user
                            it.connection.from
                        ), privateConversation
                    )
                },
                group = { groupConversation ->
                    PendingConnectionItem(
                        connectionInfo = ConnectionInfo(
                            it.connection.status.toString(),
                            it.connection.from
                        ), groupConversation
                    )
                }
            ))
        }

    private fun List<ConversationDetails>.toConversationsFoldersMap(): Map<ConversationFolder, List<ConversationItem>> =
        mapOf(ConversationFolder.Predefined.Conversations to this.toGeneralConversationList())

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

    fun openUserProfile(profileId: UserId) {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.OtherUserProfile.getRouteWithArgs(
                        listOf(profileId.domain, profileId.value)
                    )
                )
            )
        }
    }

    fun muteConversation(conversationId: ConversationId?, mutedConversationStatus: MutedConversationStatus) {
        conversationId?.let {
            viewModelScope.launch {
                when (updateConversationMutedStatus(conversationId, mutedConversationStatus, Date().time)) {
                    ConversationUpdateStatusResult.Failure -> errorState = ConversationOperationErrorState.MutingOperationErrorState()
                    ConversationUpdateStatusResult.Success -> appLogger.d("MutedStatus changed for conversation: $conversationId")
                }
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

    private fun List<ConversationDetails>.toGeneralConversationList(): List<ConversationItem> =
        filter { it is Group || it is OneOne }
            .map {
                it.toItem(
                    private = { privateConversation -> GeneralConversation(privateConversation) },
                    group = { groupConversation -> GeneralConversation(groupConversation) },
                )
            }

}

private fun LegalHoldStatus.showLegalHoldIndicator() = this == LegalHoldStatus.ENABLED

private fun ConversationDetails.toItem(
    private: (ConversationType.PrivateConversation) -> ConversationItem,
    group: (ConversationType.GroupConversation) -> ConversationItem,
): ConversationItem = when (this) {
    is Group -> {
        group(
            ConversationType.GroupConversation(
                groupColorValue = getConversationColor(conversation.id),
                groupName = conversation.name.orEmpty(),
                conversationId = conversation.id,
                mutedStatus = conversation.mutedStatus,
                isLegalHold = legalHoldStatus.showLegalHoldIndicator()
            )
        )
    }
    is OneOne -> {
        private(
            ConversationType.PrivateConversation(
                userInfo = UserInfo(
                    otherUser.previewPicture?.let { UserAvatarAsset(it) },
                    UserStatus.NONE // TODO Get actual status
                ),
                conversationInfo = ConversationInfo(
                    name = otherUser.name.orEmpty(),
                    membership = userType.toMembership()
                ),
                conversationId = conversation.id,
                mutedStatus = conversation.mutedStatus,
                isLegalHold = legalHoldStatus.showLegalHoldIndicator()
            )
        )
    }
    is Self -> {
        throw IllegalArgumentException("Self conversations should not be visible to the user.")
    }
}

private fun UserType.toMembership(): Membership {
    return when (this) {
        UserType.GUEST -> Membership.Guest
        UserType.FEDERATED -> Membership.Federated
        UserType.EXTERNAL -> Membership.External
        UserType.INTERNAL -> Membership.None
        else -> {
            throw IllegalStateException("Unknown UserType")
        }
    }
}
