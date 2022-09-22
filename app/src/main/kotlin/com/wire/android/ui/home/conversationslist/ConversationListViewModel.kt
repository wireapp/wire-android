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
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.HomeSnackbarState
import com.wire.android.ui.home.conversationslist.mock.mockAllMentionList
import com.wire.android.ui.home.conversationslist.mock.mockCallHistory
import com.wire.android.ui.home.conversationslist.mock.mockUnreadMentionList
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.BlockState
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationLastEvent
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationDetails.Connection
import com.wire.kalium.logic.data.conversation.ConversationDetails.Group
import com.wire.kalium.logic.data.conversation.ConversationDetails.OneOne
import com.wire.kalium.logic.data.conversation.ConversationDetails.Self
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationViewUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.team.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import com.wire.kalium.logic.data.conversation.ConversationView
import com.wire.kalium.logic.feature.call.CallStatus

@ExperimentalMaterial3Api
@Suppress("MagicNumber", "TooManyFunctions", "LongParameterList")
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dispatcher: DispatcherProvider,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val answerCall: AnswerCallUseCase,
    private val observeConversationViewUseCase: ObserveConversationViewUseCase,
    private val leaveConversation: LeaveConversationUseCase,
    private val deleteTeamConversation: DeleteTeamConversationUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val userTypeMapper: UserTypeMapper
) : ViewModel() {

    var state by mutableStateOf(ConversationListState())
        private set

    val snackBarState = MutableSharedFlow<HomeSnackbarState>()

    var requestInProgress: Boolean by mutableStateOf(false)

    init {
        startObservingConversationsAndConnections()
    }

    private fun startObservingConversationsAndConnections() = viewModelScope.launch {
        observeConversationViewUseCase()
            .flowOn(dispatcher.io())
            .collect { conversationListDetails ->
                state = ConversationListState(
                    conversations = conversationListDetails.toConversationsFoldersViewMap(),
                    callHistory = mockCallHistory, // TODO: needs to be implemented
                    unreadMentions = mockUnreadMentionList, // TODO: needs to be implemented
                    allMentions = mockAllMentionList, // TODO: needs to be implemented
                    newActivityCount = 0L,
                    unreadMentionsCount = 0L, // TODO: needs to be implemented on Kalium side
                    missedCallsCount = 0L // TODO: needs to be implemented on Kalium side
                )

            }
    }

    private fun List<ConversationView>.toConversationsFoldersViewMap(): Map<ConversationFolder, List<ConversationItem>> {
        val unreadConversations = this.filter { it.unreadConversationsCount > 0L }
        val remainingConversations = this - unreadConversations.toSet()
        return mapOf(
            ConversationFolder.Predefined.NewActivities to unreadConversations.toConversationViewList(),
            ConversationFolder.Predefined.Conversations to remainingConversations.toConversationViewList()
        )
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

    fun blockUser(blockUserState: BlockUserDialogState) {
        viewModelScope.launch(dispatcher.io()) {
            requestInProgress = true
            val state = when (val result = blockUserUseCase(blockUserState.userId)) {
                BlockUserResult.Success -> {
                    appLogger.d("User ${blockUserState.userId} was blocked")
                    HomeSnackbarState.BlockingUserOperationSuccess(blockUserState.userName)
                }

                is BlockUserResult.Failure -> {
                    appLogger.d("Error while blocking user ${blockUserState.userId} ; Error ${result.coreFailure}")
                    HomeSnackbarState.BlockingUserOperationError
                }
            }
            snackBarState.emit(state)
        }
        requestInProgress = false
    }

    fun leaveGroup(leaveGroupState: GroupDialogState) {
        viewModelScope.launch {
            requestInProgress = true
            val response = withContext(dispatcher.io()) {
                leaveConversation(
                    leaveGroupState.conversationId
                )
            }
            when (response) {
                is RemoveMemberFromConversationUseCase.Result.Failure ->
                    snackBarState.emit(HomeSnackbarState.LeaveConversationError)

                RemoveMemberFromConversationUseCase.Result.Success -> {
                    snackBarState.emit(HomeSnackbarState.LeftConversationSuccess)
                }
            }
            requestInProgress = false
        }
    }

    fun deleteGroup(groupDialogState: GroupDialogState) {
        viewModelScope.launch {
            requestInProgress = true
            when (withContext(dispatcher.io()) { deleteTeamConversation(groupDialogState.conversationId) }) {
                is Result.Failure.GenericFailure -> snackBarState.emit(HomeSnackbarState.DeleteConversationGroupError)
                Result.Failure.NoTeamFailure -> snackBarState.emit(HomeSnackbarState.DeleteConversationGroupError)
                Result.Success -> snackBarState.emit(HomeSnackbarState.DeletedConversationGroupSuccess(groupDialogState.conversationName))
            }
        }
        requestInProgress = false
    }

    private fun List<ConversationView>.toConversationViewList(): List<ConversationItem> =
        filter { it.name != null }.map {
            appLogger.e(it.name.toString() + "   " + it.otherUserId.toString())
            when (it.type) {
                Conversation.Type.SELF -> TODO()
                Conversation.Type.ONE_ON_ONE -> ConversationItem.PrivateConversation(
                    name = it.name.orEmpty(),
                    userAvatarData = UserAvatarData(
                        it.previewAssetId?.let { UserAvatarAsset(wireSessionImageLoader, it) },
                        it.userAvailabilityStatus ?: UserAvailabilityStatus.NONE,
                        it.connectionStatus
                    ),
                    conversationInfo = ConversationInfo(
                        name = it.name.orEmpty(),
                        membership = userTypeMapper.toMembership(it.userType),
                        unavailable = it.isUnavailableUser
                    ),
                    conversationId = it.id,
                    mutedStatus = it.mutedStatus,
                    isLegalHold = it.legalHoldStatus.showLegalHoldIndicator(),
                    lastEvent = ConversationLastEvent.None, // TODO implement unread events
                    badgeEventType = parsePrivateConversationEventType(
                        it.connectionStatus, parseConversationEventType(it.mutedStatus, it.mentionsCount, it.unreadConversationsCount)
                    ),
                    userId = it.otherUserId!!,
                    blockingState = BlockingState.NOT_BLOCKED // Todo: get it from query
                )

                Conversation.Type.GROUP

                -> ConversationItem.GroupConversation(
                    name = it.name.orEmpty(),
                    conversationId = it.id,
                    mutedStatus = MutedConversationStatus.AllAllowed,
                    isLegalHold = it.legalHoldStatus.showLegalHoldIndicator(),
                    lastEvent = ConversationLastEvent.None,
                    badgeEventType = if (it.unreadConversationsCount != 0L) BadgeEventType.UnreadMessage(it.unreadConversationsCount) else BadgeEventType.None,
                    hasOnGoingCall = it.callStatus != null && it.callStatus == CallStatus.STILL_ONGOING,
                    isCreator = it.isCreator,
                    isSelfUserMember = it.isMember
                )

                Conversation.Type.CONNECTION_PENDING -> ConversationItem.ConnectionConversation(
                    name = it.name.orEmpty(),
                    userAvatarData = UserAvatarData(
                        it.previewAssetId?.let { UserAvatarAsset(wireSessionImageLoader, it) },
                        it.userAvailabilityStatus ?: UserAvailabilityStatus.NONE,
                        it.connectionStatus
                    ),
                    conversationInfo = ConversationInfo(
                        name = it.name.orEmpty(),
                        membership = userTypeMapper.toMembership(it.userType),
                        unavailable = it.isUnavailableUser
                    ),
                    conversationId = it.id,
                    mutedStatus = MutedConversationStatus.AllAllowed,
                    isLegalHold = it.legalHoldStatus.showLegalHoldIndicator(),
                    lastEvent = ConversationLastEvent.Connection(
                        it.connectionStatus,
                        it.otherUserId!!
                    ),
                    badgeEventType = parseConnectionEventType(it.connectionStatus)
                )
            }

        }
}

private fun LegalHoldStatus.showLegalHoldIndicator() = this == LegalHoldStatus.ENABLED

private fun parseConnectionEventType(connectionState: ConnectionState) =
    if (connectionState == ConnectionState.SENT) BadgeEventType.SentConnectRequest else BadgeEventType.ReceivedConnectionRequest

private fun parsePrivateConversationEventType(connectionState: ConnectionState, eventType: BadgeEventType) =
    if (connectionState == ConnectionState.BLOCKED) BadgeEventType.Blocked
    else eventType

private fun parseConversationEventType(
    mutedStatus: MutedConversationStatus,
    unreadMentionsCount: Long,
    unreadMessagesCount: Long
): BadgeEventType = when (mutedStatus) {
    MutedConversationStatus.AllMuted -> BadgeEventType.None
    MutedConversationStatus.OnlyMentionsAllowed ->
        if (unreadMentionsCount > 0) BadgeEventType.UnreadMention
        else BadgeEventType.None

    else -> when {
        unreadMentionsCount > 0 -> BadgeEventType.UnreadMention
        unreadMessagesCount > 0 -> BadgeEventType.UnreadMessage(unreadMessagesCount)
        else -> BadgeEventType.None
    }
}
