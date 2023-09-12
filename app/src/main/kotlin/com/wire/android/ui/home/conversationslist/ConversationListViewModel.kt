/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.home.conversationslist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.mapper.toUIPreview
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.bottomsheet.conversation.ConversationTypeDetail
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.HomeSnackbarState
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversations.search.SearchPeopleViewModel
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.BlockState
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationDetails.Connection
import com.wire.kalium.logic.data.conversation.ConversationDetails.Group
import com.wire.kalium.logic.data.conversation.ConversationDetails.OneOne
import com.wire.kalium.logic.data.conversation.ConversationDetails.Self
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.conversation.UnreadEventCount
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.UnreadEventType
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserResult
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RefreshConversationsWithoutMetadataUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.team.Result
import com.wire.kalium.logic.functional.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@Suppress("MagicNumber", "TooManyFunctions", "LongParameterList")
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val dispatcher: DispatcherProvider,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val answerCall: AnswerCallUseCase,
    private val observeConversationListDetails: ObserveConversationListDetailsUseCase,
    private val leaveConversation: LeaveConversationUseCase,
    private val deleteTeamConversation: DeleteTeamConversationUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val unblockUserUseCase: UnblockUserUseCase,
    private val clearConversationContentUseCase: ClearConversationContentUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val userTypeMapper: UserTypeMapper,
    private val endCall: EndCallUseCase,
    private val refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase,
    private val refreshConversationsWithoutMetadata: RefreshConversationsWithoutMetadataUseCase
) : ViewModel() {

    var conversationListState by mutableStateOf(ConversationListState())

    val homeSnackBarState = MutableSharedFlow<HomeSnackbarState>()

    val closeBottomSheet = MutableSharedFlow<Unit>()

    var requestInProgress: Boolean by mutableStateOf(false)

    private val mutableSearchQueryFlow = MutableStateFlow("")

    private val searchQueryFlow = mutableSearchQueryFlow
        .asStateFlow()
        .debounce(SearchPeopleViewModel.DEFAULT_SEARCH_QUERY_DEBOUNCE)

    var establishedCallConversationId: QualifiedID? = null
    private var conversationId: QualifiedID? = null

    private fun observeEstablishedCall() = viewModelScope.launch {
        observeEstablishedCalls()
            .distinctUntilChanged()
            .collect {
                val hasEstablishedCall = it.isNotEmpty()
                establishedCallConversationId = if (it.isNotEmpty()) {
                    it.first().conversationId
                } else {
                    null
                }
                conversationListState = conversationListState.copy(hasEstablishedCall = hasEstablishedCall)
            }
    }

    init {
        viewModelScope.launch {
            observeEstablishedCall()
        }
        viewModelScope.launch {
            searchQueryFlow.combine(
                observeConversationListDetails()
                    .map {
                        it.map { conversationDetails ->
                            conversationDetails.toConversationItem(
                                wireSessionImageLoader,
                                userTypeMapper
                            )
                        }
                    }
            )
                .map { (searchQuery, conversationItems) -> conversationItems.withFolders().toImmutableMap() to searchQuery }
                .collect { (conversationsWithFolders, searchQuery) ->
                    conversationListState = conversationListState.copy(
                        conversationSearchResult = if (searchQuery.isEmpty()) {
                            conversationsWithFolders
                        } else {
                            searchConversation(
                                conversationsWithFolders.values.flatten(),
                                searchQuery
                            ).withFolders().toImmutableMap()
                        },
                        hasNoConversations = conversationsWithFolders.isEmpty(),
                        foldersWithConversations = conversationsWithFolders,
                        // TODO: missing other lists and counters (for bottom tabs if we decide to bring them back)
                        searchQuery = searchQuery
                    )
                }
        }
    }

    fun showCallingPermissionDialog() {
        conversationListState = conversationListState.copy(shouldShowCallingPermissionDialog = true)
    }

    fun dismissCallingPermissionDialog() {
        conversationListState = conversationListState.copy(shouldShowCallingPermissionDialog = false)
    }

    // Mateusz : First iteration, just filter stuff
    // next iteration : SQL- query ?
    private fun searchConversation(conversationDetails: List<ConversationItem>, searchQuery: String): List<ConversationItem> {
        val matchingConversations = conversationDetails.filter { details ->
            when (details) {
                is ConversationItem.ConnectionConversation -> details.conversationInfo.name.contains(searchQuery, true)
                is ConversationItem.GroupConversation -> details.groupName.contains(searchQuery, true)
                is ConversationItem.PrivateConversation -> details.conversationInfo.name.contains(searchQuery, true)
            }
        }
        return matchingConversations
    }

    suspend fun refreshMissingMetadata() {
        viewModelScope.launch {
            refreshUsersWithoutMetadata()
            refreshConversationsWithoutMetadata()
        }
    }

    @Suppress("ComplexMethod")
    private fun List<ConversationItem>.withFolders(): Map<ConversationFolder, List<ConversationItem>> {
        val unreadConversations = filter {
            when (it.mutedStatus) {
                MutedConversationStatus.AllAllowed -> when (it.badgeEventType) {
                    BadgeEventType.Blocked -> false
                    BadgeEventType.Deleted -> false
                    BadgeEventType.Knock -> true
                    BadgeEventType.MissedCall -> true
                    BadgeEventType.None -> false
                    BadgeEventType.ReceivedConnectionRequest -> true
                    BadgeEventType.SentConnectRequest -> false
                    BadgeEventType.UnreadMention -> true
                    is BadgeEventType.UnreadMessage -> true
                    BadgeEventType.UnreadReply -> true
                }

                MutedConversationStatus.OnlyMentionsAndRepliesAllowed -> when (it.badgeEventType) {
                    BadgeEventType.UnreadReply -> true
                    BadgeEventType.UnreadMention -> true
                    BadgeEventType.ReceivedConnectionRequest -> true
                    else -> false
                }

                MutedConversationStatus.AllMuted -> false
            } || (it is ConversationItem.GroupConversation && it.hasOnGoingCall)
        }

        val remainingConversations = this - unreadConversations.toSet()

        return buildMap {
            if (unreadConversations.isNotEmpty()) put(ConversationFolder.Predefined.NewActivities, unreadConversations)
            if (remainingConversations.isNotEmpty()) put(ConversationFolder.Predefined.Conversations, remainingConversations)
        }
    }

    @Suppress("ComplexMethod", "NoMultipleSpaces")
    private fun List<ConversationDetails>.toConversationsFoldersMap(): Map<ConversationFolder, List<ConversationItem>> {
        val unreadConversations = filter {
            when (it.conversation.mutedStatus) {
                MutedConversationStatus.AllAllowed ->
                    when (it) {
                        is Group -> it.unreadEventCount.isNotEmpty()
                        is OneOne -> it.unreadEventCount.isNotEmpty()
                        else -> false // TODO should connection requests also be listed on "new activities"?
                    }

                MutedConversationStatus.OnlyMentionsAndRepliesAllowed ->
                    when (it) {
                        is Group -> it.unreadEventCount.containsKey(UnreadEventType.MENTION) ||
                                it.unreadEventCount.containsKey(UnreadEventType.REPLY)

                        is OneOne -> it.unreadEventCount.containsKey(UnreadEventType.MENTION) ||
                                it.unreadEventCount.containsKey(UnreadEventType.REPLY)

                        else -> false
                    }

                else -> false
            } ||
                    (it is Connection && it.connection.status == ConnectionState.PENDING) ||
                    (it is Group && it.hasOngoingCall)
        }

        val remainingConversations = this - unreadConversations.toSet()

        val unreadConversationsItems = unreadConversations.toConversationItemList()
        val remainingConversationsItems = remainingConversations.toConversationItemList()

        return buildMap {
            if (unreadConversationsItems.isNotEmpty()) put(ConversationFolder.Predefined.NewActivities, unreadConversationsItems)
            if (remainingConversationsItems.isNotEmpty()) put(ConversationFolder.Predefined.Conversations, remainingConversationsItems)
        }
    }

    fun muteConversation(conversationId: ConversationId?, mutedConversationStatus: MutedConversationStatus) {
        conversationId?.let {
            viewModelScope.launch {
                when (updateConversationMutedStatus(conversationId, mutedConversationStatus, Date().time)) {
                    ConversationUpdateStatusResult.Failure -> homeSnackBarState.emit(HomeSnackbarState.MutingOperationError)
                    ConversationUpdateStatusResult.Success ->
                        appLogger.d("MutedStatus changed for conversation: $conversationId to $mutedConversationStatus")
                }
            }
        }
    }

    fun joinAnyway(conversationId: ConversationId, onJoined: (ConversationId) -> Unit) {
        viewModelScope.launch {
            establishedCallConversationId?.let {
                endCall(it)
                delay(DELAY_END_CALL)
            }
            joinOngoingCall(conversationId, onJoined)
        }
    }

    fun joinOngoingCall(conversationId: ConversationId, onJoined: (ConversationId) -> Unit) {
        this.conversationId = conversationId
        viewModelScope.launch {
            if (conversationListState.hasEstablishedCall) {
                showJoinCallAnywayDialog()
            } else {
                dismissJoinCallAnywayDialog()
                answerCall(conversationId = conversationId)
                onJoined(conversationId)
            }
        }
    }

    private fun showJoinCallAnywayDialog() {
        conversationListState = conversationListState.copy(shouldShowJoinAnywayDialog = true)
    }

    fun dismissJoinCallAnywayDialog() {
        conversationListState = conversationListState.copy(shouldShowJoinAnywayDialog = false)
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
            homeSnackBarState.emit(state)
            requestInProgress = false
        }
    }

    fun unblockUser(userId: UserId) {
        viewModelScope.launch(dispatcher.io()) {
            requestInProgress = true
            when (val result = unblockUserUseCase(userId)) {
                UnblockUserResult.Success -> {
                    appLogger.i("User $userId was unblocked")
                    closeBottomSheet.emit(Unit)
                }

                is UnblockUserResult.Failure -> {
                    appLogger.e("Error while unblocking user $userId ; Error ${result.coreFailure}")
                    homeSnackBarState.emit(HomeSnackbarState.UnblockingUserOperationError)
                }
            }
            requestInProgress = false
        }
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
                    homeSnackBarState.emit(HomeSnackbarState.LeaveConversationError)

                RemoveMemberFromConversationUseCase.Result.Success -> {
                    homeSnackBarState.emit(HomeSnackbarState.LeftConversationSuccess)
                }
            }
            requestInProgress = false
        }
    }

    fun deleteGroup(groupDialogState: GroupDialogState) {
        viewModelScope.launch {
            requestInProgress = true
            when (withContext(dispatcher.io()) { deleteTeamConversation(groupDialogState.conversationId) }) {
                is Result.Failure.GenericFailure -> homeSnackBarState.emit(HomeSnackbarState.DeleteConversationGroupError)
                Result.Failure.NoTeamFailure -> homeSnackBarState.emit(HomeSnackbarState.DeleteConversationGroupError)
                Result.Success -> homeSnackBarState.emit(
                    HomeSnackbarState.DeletedConversationGroupSuccess(groupDialogState.conversationName)
                )
            }
            requestInProgress = false
        }
    }

    private fun List<ConversationDetails>.toConversationItemList(): List<ConversationItem> =
        filter { it is Group || it is OneOne || it is Connection }
            .map {
                it.toConversationItem(wireSessionImageLoader, userTypeMapper)
            }

    fun searchConversation(searchQuery: TextFieldValue) {
        viewModelScope.launch {
            mutableSearchQueryFlow.emit(searchQuery.text)
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

    fun clearConversationContent(dialogState: DialogState) {
        viewModelScope.launch {
            requestInProgress = true
            with(dialogState) {
                val result = withContext(dispatcher.io()) { clearConversationContentUseCase(conversationId) }
                requestInProgress = false
                clearContentSnackbarResult(result, conversationTypeDetail)
            }
        }
    }

    @Suppress("MultiLineIfElse")
    private suspend fun clearContentSnackbarResult(
        clearContentResult: ClearConversationContentUseCase.Result,
        conversationTypeDetail: ConversationTypeDetail
    ) {
        if (conversationTypeDetail is ConversationTypeDetail.Connection) {
            throw IllegalStateException("Unsupported conversation type to clear content, something went wrong?")
        }

        val isGroup = conversationTypeDetail is ConversationTypeDetail.Group

        if (clearContentResult is ClearConversationContentUseCase.Result.Failure) {
            homeSnackBarState.emit(HomeSnackbarState.ClearConversationContentFailure(isGroup))
        } else {
            homeSnackBarState.emit(HomeSnackbarState.ClearConversationContentSuccess(isGroup))
        }
    }

    companion object {
        const val DELAY_END_CALL = 200L
    }
}

fun LegalHoldStatus.showLegalHoldIndicator() = this == LegalHoldStatus.ENABLED

@Suppress("LongMethod")
private fun ConversationDetails.toConversationItem(
    wireSessionImageLoader: WireSessionImageLoader,
    userTypeMapper: UserTypeMapper
): ConversationItem = when (this) {
    is Group -> {
        ConversationItem.GroupConversation(
            groupName = conversation.name.orEmpty(),
            conversationId = conversation.id,
            mutedStatus = conversation.mutedStatus,
            isLegalHold = legalHoldStatus.showLegalHoldIndicator(),
            lastMessageContent = lastMessage.toUIPreview(unreadEventCount),
            badgeEventType = parseConversationEventType(
                conversation.mutedStatus,
                unreadEventCount
            ),
            hasOnGoingCall = hasOngoingCall && this.isSelfUserMember,
            isSelfUserCreator = isSelfUserCreator,
            isSelfUserMember = isSelfUserMember,
            teamId = conversation.teamId,
            selfMemberRole = selfRole
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
                isSenderUnavailable = otherUser.isUnavailableUser
            ),
            conversationId = conversation.id,
            mutedStatus = conversation.mutedStatus,
            isLegalHold = legalHoldStatus.showLegalHoldIndicator(),
            lastMessageContent = lastMessage.toUIPreview(unreadEventCount),
            badgeEventType = parsePrivateConversationEventType(
                otherUser.connectionStatus,
                otherUser.deleted,
                parseConversationEventType(
                    conversation.mutedStatus,
                    unreadEventCount
                )
            ),
            userId = otherUser.id,
            blockingState = otherUser.BlockState,
            teamId = otherUser.teamId
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
            lastMessageContent = UILastMessageContent.Connection(
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

fun parsePrivateConversationEventType(connectionState: ConnectionState, isDeleted: Boolean, eventType: BadgeEventType) =
    if (connectionState == ConnectionState.BLOCKED) {
        BadgeEventType.Blocked
    } else if (isDeleted) {
        BadgeEventType.Deleted
    } else {
        eventType
    }

fun parseConversationEventType(
    mutedStatus: MutedConversationStatus,
    unreadEventCount: UnreadEventCount
): BadgeEventType = when (mutedStatus) {
    MutedConversationStatus.AllMuted -> BadgeEventType.None
    MutedConversationStatus.OnlyMentionsAndRepliesAllowed ->
        when {
            unreadEventCount.containsKey(UnreadEventType.MENTION) -> BadgeEventType.UnreadMention
            unreadEventCount.containsKey(UnreadEventType.REPLY) -> BadgeEventType.UnreadReply
            unreadEventCount.containsKey(UnreadEventType.MISSED_CALL) -> BadgeEventType.MissedCall
            else -> BadgeEventType.None
        }

    else -> {
        val unreadMessagesCount = unreadEventCount.values.sum()
        when {
            unreadEventCount.containsKey(UnreadEventType.KNOCK) -> BadgeEventType.Knock
            unreadEventCount.containsKey(UnreadEventType.MISSED_CALL) -> BadgeEventType.MissedCall
            unreadEventCount.containsKey(UnreadEventType.MENTION) -> BadgeEventType.UnreadMention
            unreadEventCount.containsKey(UnreadEventType.REPLY) -> BadgeEventType.UnreadReply
            unreadMessagesCount > 0 -> BadgeEventType.UnreadMessage(unreadMessagesCount)
            else -> BadgeEventType.None
        }
    }
}
