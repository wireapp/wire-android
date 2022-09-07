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
import com.wire.android.ui.home.conversationslist.mock.mockMissedCalls
import com.wire.android.ui.home.conversationslist.mock.mockUnreadMentionList
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationLastEvent
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.ui.home.conversationslist.model.getBlockingState
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
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.IsSelfUserMemberResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationsAndConnectionsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveIsSelfUserMemberUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.team.Result
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@ExperimentalMaterial3Api
@Suppress("MagicNumber", "TooManyFunctions", "LongParameterList")
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dispatcher: DispatcherProvider,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val answerCall: AnswerCallUseCase,
    private val observeConversationsAndConnections: ObserveConversationsAndConnectionsUseCase,
    private val removeMemberFromConversation: RemoveMemberFromConversationUseCase,
    private val deleteTeamConversation: DeleteTeamConversationUseCase,
    private val observeSelfUser: GetSelfUserUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val userTypeMapper: UserTypeMapper
) : ViewModel() {

    var state by mutableStateOf(ConversationListState())
        private set

    val snackBarState = MutableSharedFlow<HomeSnackbarState>()
    lateinit var selfUserId: UserId

    var requestInProgress: Boolean by mutableStateOf(false)

    init {
        startObservingConversationsAndConnections()
    }

    private fun startObservingConversationsAndConnections() = viewModelScope.launch(dispatcher.io()) {
        observeConversationsAndConnections() // TODO AR-1736
            .combine(observeSelfUser(), ::Pair)
            .collect { (conversationListDetails, selfUser) ->
                with(conversationListDetails) {
                    selfUserId = selfUser.id
                    state = ConversationListState(
                        conversations = conversationList.toConversationsFoldersMap(selfUser),
                        missedCalls = mockMissedCalls, // TODO: needs to be implemented
                        callHistory = mockCallHistory, // TODO: needs to be implemented
                        unreadMentions = mockUnreadMentionList, // TODO: needs to be implemented
                        allMentions = mockAllMentionList, // TODO: needs to be implemented
                        newActivityCount = unreadConversationsCount,
                        unreadMentionsCount = mentionsCount, // TODO: needs to be implemented on Kalium side
                        missedCallsCount = missedCallsCount // TODO: needs to be implemented on Kalium side
                    )
                }
            }
    }

    private fun List<ConversationDetails>.toConversationsFoldersMap(selfUser: SelfUser?): Map<ConversationFolder, List<ConversationItem>> {
        val unreadConversations = this.filter {
            when (it.conversation.mutedStatus) {
                MutedConversationStatus.AllAllowed ->
                    when (it) {
                        is Group -> it.unreadMessagesCount > 0
                        is OneOne -> it.unreadMessagesCount > 0
                        else -> false  // TODO should connection requests also be listed on "new activities"?
                    }
                MutedConversationStatus.OnlyMentionsAllowed ->
                    when (it) {
                        is Group -> it.unreadMentionsCount > 0
                        is OneOne -> it.unreadMentionsCount > 0
                        else -> false
                    }
                else -> false
            }
        }
        val remainingConversations = this - unreadConversations.toSet()
        return mapOf(
            ConversationFolder.Predefined.NewActivities to unreadConversations.toConversationItemList(selfUser),
            ConversationFolder.Predefined.Conversations to remainingConversations.toConversationItemList(selfUser)
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
                val selfUser = observeSelfUser().first()
                removeMemberFromConversation(
                    leaveGroupState.conversationId,
                    selfUser.id
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

    private fun List<ConversationDetails>.toConversationItemList(selfUser: SelfUser?): List<ConversationItem> =
        filter { it is Group || it is OneOne || it is Connection }
            .map {
                it.toConversationItem(wireSessionImageLoader, selfUser, userTypeMapper) }
}

private fun LegalHoldStatus.showLegalHoldIndicator() = this == LegalHoldStatus.ENABLED

@Suppress("LongMethod")
private fun ConversationDetails.toConversationItem(
    wireSessionImageLoader: WireSessionImageLoader,
    selfUser: SelfUser?,
    userTypeMapper: UserTypeMapper
): ConversationItem = when (this) {
    is Group -> {
        ConversationItem.GroupConversation(
            groupName = conversation.name.orEmpty(),
            conversationId = conversation.id,
            mutedStatus = conversation.mutedStatus,
            isLegalHold = legalHoldStatus.showLegalHoldIndicator(),
            lastEvent = ConversationLastEvent.None, // TODO implement unread events
            badgeEventType = parseConversationEventType(conversation.mutedStatus, unreadMentionsCount, unreadMessagesCount),
            hasOnGoingCall = hasOngoingCall,
            isCreator = selfUser?.teamId != null
                    && conversation.creatorId.value == selfUser.id.value,
            isSelfUserMember = isSelfUserMember
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
                membership = userTypeMapper.toMembership(userType),
                unavailable = otherUser.isUnavailableUser
            ),
            conversationId = conversation.id,
            mutedStatus = conversation.mutedStatus,
            isLegalHold = legalHoldStatus.showLegalHoldIndicator(),
            lastEvent = ConversationLastEvent.None, // TODO implement unread events
            badgeEventType = parsePrivateConversationEventType(
                connectionState, parseConversationEventType(conversation.mutedStatus, unreadMentionsCount, unreadMessagesCount)
            ),
            userId = otherUser.id,
            blockingState = otherUser.getBlockingState(selfUser?.teamId)
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
            badgeEventType = parseConnectionEventType(connection.status),
            conversationId = conversation.id,
            mutedStatus = conversation.mutedStatus
        )
    }
    is Self -> {
        throw IllegalArgumentException("Self conversations should not be visible to the user.")
    }
    else -> {
        throw IllegalArgumentException("$this conversations should not be visible to the user.")
    }
}

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
