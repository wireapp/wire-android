package com.wire.android.ui.home.conversationslist

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.navigation.NavigationCommand
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.HomeSnackbarState
import com.wire.android.ui.home.conversationslist.mock.mockAllMentionList
import com.wire.android.ui.home.conversationslist.mock.mockCallHistory
import com.wire.android.ui.home.conversationslist.mock.mockMissedCalls
import com.wire.android.ui.home.conversationslist.mock.mockUnreadMentionList
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationLastEvent
import com.wire.android.ui.home.conversationslist.model.NewActivity
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationDetails.Connection
import com.wire.kalium.logic.data.conversation.ConversationDetails.Group
import com.wire.kalium.logic.data.conversation.ConversationDetails.OneOne
import com.wire.kalium.logic.data.conversation.ConversationDetails.Self
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationsAndConnectionsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@ExperimentalMaterial3Api
@Suppress("MagicNumber", "TooManyFunctions", "LongParameterList")
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val answerCall: AnswerCallUseCase,
    private val observeConversationsAndConnections: ObserveConversationsAndConnectionsUseCase,
    private val dispatchers: DispatcherProvider,
    private val getSelf: GetSelfUserUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val userTypeMapper: UserTypeMapper,
    private val leaveGroupConversation: RemoveMemberFromConversationUseCase
) : ViewModel() {

    var state by mutableStateOf(ConversationListState())
        private set

    val snackBarState = MutableSharedFlow<HomeSnackbarState>()
    lateinit var selfUserId: UserId

    init {
        startObservingConversationsAndConnections()
    }

    private fun startObservingConversationsAndConnections() = viewModelScope.launch(dispatchers.io()) {
        observeConversationsAndConnections() // TODO AR-1736
            .combine(getSelf(), ::Pair)
            .collect { (list, selfUser) ->
                selfUserId = selfUser.id
                val detailedList = list.conversationList.toConversationsFoldersMap(selfUser.teamId)
                val newActivities = emptyList<NewActivity>()
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
                    newActivityCount = 0 // TODO: needs to be implemented
                )
            }
    }

    private fun List<ConversationDetails>.toConversationsFoldersMap(teamId: TeamId?): Map<ConversationFolder, List<ConversationItem>> =
        mapOf(ConversationFolder.Predefined.Conversations to this.toConversationItemList(teamId))

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
                        listOf(profileId)
                    )
                )
            )
        }
    }

    fun muteConversation(conversationId: ConversationId?, mutedConversationStatus: MutedConversationStatus) {
        conversationId?.let {
            viewModelScope.launch {
                when (updateConversationMutedStatus(conversationId, mutedConversationStatus, Date().time)) {
                    ConversationUpdateStatusResult.Failure -> snackBarState.emit(HomeSnackbarState.MutingOperationError)
                    ConversationUpdateStatusResult.Success ->
                        appLogger.d("MutedStatus changed for conversation: $conversationId to $mutedConversationStatus")
                }
            }
        }
    }

    fun joinOngoingCall(conversationId: ConversationId) {
        viewModelScope.launch {
            answerCall(conversationId = conversationId)
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(conversationId))
                )
            )
        }
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun addConversationToFavourites(id: String = "") {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun moveConversationToFolder(id: String = "") {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun moveConversationToArchive(id: String = "") {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun clearConversationContent(id: String = "") {
    }

    fun blockUser(id: UserId, userName: String) {
        viewModelScope.launch(dispatchers.io()) {
            val state = when (val result = blockUserUseCase(id)) {
                BlockUserResult.Success -> {
                    appLogger.d("User $id was blocked")
                    HomeSnackbarState.BlockingUserOperationSuccess(userName)
                }
                is BlockUserResult.Failure -> {
                    appLogger.d("Error while blocking user $id ; Error ${result.coreFailure}")
                    HomeSnackbarState.BlockingUserOperationError
                }
            }
            snackBarState.emit(state)
        }
        state = state.copy(blockUserDialogSate = null)
    }

    fun onDismissBlockUserDialog() {
        state = state.copy(blockUserDialogSate = null)
    }

    fun onBlockUserClicked(id: UserId, name: String) {
        state = state.copy(blockUserDialogSate = BlockUserDialogState(name, id))
    }

    fun leaveGroup(conversationId: ConversationId) = viewModelScope.launch(dispatchers.io()) {
        leaveGroupConversation(conversationId = conversationId, selfUserId)
    }

    private fun List<ConversationDetails>.toConversationItemList(teamId: TeamId?): List<ConversationItem> =
        filter { it is Group || it is OneOne || it is Connection }
            .map { it.toType(wireSessionImageLoader, teamId, userTypeMapper) }
}

private fun LegalHoldStatus.showLegalHoldIndicator() = this == LegalHoldStatus.ENABLED

private fun ConversationDetails.toType(
    wireSessionImageLoader: WireSessionImageLoader,
    selfTeamId: TeamId?,
    userTypeMapper: UserTypeMapper
): ConversationItem = when (this) {
    is Group -> {
        ConversationItem.GroupConversation(
            groupName = conversation.name.orEmpty(),
            conversationId = conversation.id,
            mutedStatus = conversation.mutedStatus,
            isLegalHold = legalHoldStatus.showLegalHoldIndicator(),
            lastEvent = ConversationLastEvent.None, // TODO implement unread events
            hasOnGoingCall = hasOngoingCall
        )
    }
    is OneOne -> {
        ConversationItem.PrivateConversation(
            userAvatarData = UserAvatarData(
                otherUser.previewPicture?.let { UserAvatarAsset(wireSessionImageLoader, it) },
                otherUser.availabilityStatus,
                otherUser.connectionStatus
            ),
            conversationInfo = ConversationInfo(
                name = otherUser.name.orEmpty(),
                membership = userTypeMapper.toMembership(userType)
            ),
            conversationId = conversation.id,
            mutedStatus = conversation.mutedStatus,
            isLegalHold = legalHoldStatus.showLegalHoldIndicator(),
            lastEvent = ConversationLastEvent.None, // TODO implement unread events
            userId = otherUser.id,
            blockingState = otherUser.getBlockingState(selfTeamId),
            connectionState = otherUser.connectionStatus
        )
    }
    is Connection -> {
        ConversationItem.ConnectionConversation(
            userAvatarData = UserAvatarData(
                otherUser?.previewPicture?.let { UserAvatarAsset(wireSessionImageLoader, it) },
                otherUser?.availabilityStatus ?: UserAvailabilityStatus.NONE
            ),
            conversationInfo = ConversationInfo(
                name = otherUser?.name.orEmpty(),
                membership = userTypeMapper.toMembership(userType)
            ),
            lastEvent = ConversationLastEvent.Connection(
                connection.status,
                connection.qualifiedToId
            ),
            conversationId = conversation.id,
            mutedStatus = conversation.mutedStatus,
            connectionState = connection.status
        )
    }
    is Self -> {
        throw IllegalArgumentException("Self conversations should not be visible to the user.")
    }
    else -> {
        throw IllegalArgumentException("$this conversations should not be visible to the user.")
    }
}

private fun OtherUser.getBlockingState(selfTeamId: TeamId?): BlockingState =
    when {
        connectionStatus == ConnectionState.BLOCKED -> BlockingState.BLOCKED
        teamId == selfTeamId -> BlockingState.CAN_NOT_BE_BLOCKED
        else -> BlockingState.NOT_BLOCKED
    }
