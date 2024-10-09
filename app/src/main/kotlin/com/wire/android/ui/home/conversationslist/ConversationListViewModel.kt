/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.home.conversationslist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.mapper.toUIPreview
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.SnackBarMessage
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.bottomsheet.conversation.ConversationTypeDetail
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.HomeSnackBarMessage
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversations.search.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.android.ui.home.conversationslist.common.previewConversationFolders
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.BlockState
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationDetails.Connection
import com.wire.kalium.logic.data.conversation.ConversationDetails.Group
import com.wire.kalium.logic.data.conversation.ConversationDetails.OneOne
import com.wire.kalium.logic.data.conversation.ConversationDetails.Self
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.conversation.UnreadEventCount
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.UnreadEventType
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserResult
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.ArchiveStatusUpdateResult
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RefreshConversationsWithoutMetadataUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationArchivedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.team.Result
import com.wire.kalium.util.DateTimeUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.Date

interface ConversationListViewModel {
    val infoMessage: SharedFlow<SnackBarMessage> get() = MutableSharedFlow()
    val closeBottomSheet: SharedFlow<Unit> get() = MutableSharedFlow()
    val requestInProgress: Boolean get() = false
    val conversationListState: ConversationListState get() = ConversationListState()
    suspend fun refreshMissingMetadata() {}
    fun moveConversationToArchive(
        dialogState: DialogState,
        timestamp: Long = DateTimeUtil.currentInstant().toEpochMilliseconds()
    ) {}
    fun blockUser(blockUserState: BlockUserDialogState) {}
    fun unblockUser(userId: UserId) {}
    fun deleteGroup(groupDialogState: GroupDialogState) {}
    fun leaveGroup(leaveGroupState: GroupDialogState) {}
    fun clearConversationContent(dialogState: DialogState) {}
    fun muteConversation(conversationId: ConversationId?, mutedConversationStatus: MutedConversationStatus) {}
    fun addConversationToFavourites() {}
    fun moveConversationToFolder() {}
    fun searchQueryChanged(searchQuery: String) {}
}

class ConversationListViewModelPreview(
    foldersWithConversations: ImmutableMap<ConversationFolder, List<ConversationItem>> = previewConversationFolders(),
    searchQuery: String = "",
) : ConversationListViewModel {
    override val conversationListState = ConversationListState(searchQuery, foldersWithConversations)
}

@Suppress("MagicNumber", "TooManyFunctions", "LongParameterList")
@HiltViewModel(assistedFactory = ConversationListViewModelImpl.Factory::class)
class ConversationListViewModelImpl @AssistedInject constructor(
    @Assisted val conversationsSource: ConversationsSource,
    private val dispatcher: DispatcherProvider,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val observeConversationListDetails: ObserveConversationListDetailsUseCase,
    private val leaveConversation: LeaveConversationUseCase,
    private val deleteTeamConversation: DeleteTeamConversationUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val unblockUserUseCase: UnblockUserUseCase,
    private val clearConversationContentUseCase: ClearConversationContentUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val userTypeMapper: UserTypeMapper,
    private val refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase,
    private val refreshConversationsWithoutMetadata: RefreshConversationsWithoutMetadataUseCase,
    private val updateConversationArchivedStatus: UpdateConversationArchivedStatusUseCase,
) : ConversationListViewModel, ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(conversationsSource: ConversationsSource): ConversationListViewModelImpl
    }

    private var _conversationListState by mutableStateOf(ConversationListState())
    override val conversationListState: ConversationListState get() = _conversationListState

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    override val infoMessage = _infoMessage.asSharedFlow()

    private var _requestInProgress: Boolean by mutableStateOf(false)
    override val requestInProgress: Boolean get() = _requestInProgress

    override val closeBottomSheet = MutableSharedFlow<Unit>()

    private val searchQueryFlow: MutableStateFlow<String> = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQueryFlow
                .debounce { if (it.isEmpty()) 0L else DEFAULT_SEARCH_QUERY_DEBOUNCE }
                .onStart { emit("") }
                .distinctUntilChanged()
                .flatMapLatest { searchQuery: String ->
                    observeConversationListDetails(
                        fromArchive = conversationsSource == ConversationsSource.ARCHIVE
                    ).map {
                        it.map { conversationDetails ->
                            conversationDetails.toConversationItem(
                                wireSessionImageLoader = wireSessionImageLoader,
                                userTypeMapper = userTypeMapper
                            )
                        }
                    }.map { conversationItems ->
                        conversationItems.withFolders(source = conversationsSource)
                            .toImmutableMap() to searchQuery
                    }
                }
                .map { (conversationsWithFolders, searchQuery) ->
                    _conversationListState.copy(
                        foldersWithConversations = if (searchQuery.isEmpty()) {
                            conversationsWithFolders
                        } else {
                            searchConversation(
                                conversationDetails = conversationsWithFolders.values.flatten(),
                                searchQuery = searchQuery
                            ).withFolders(source = conversationsSource).toImmutableMap()
                        },
                        searchQuery = searchQuery
                    )
                }
                .flowOn(dispatcher.io())
                .collect {
                    _conversationListState = it
                }
        }
    }

    override fun searchQueryChanged(searchQuery: String) {
        viewModelScope.launch {
            searchQueryFlow.emit(searchQuery)
        }
    }

    // Mateusz : First iteration, just filter stuff
    // next iteration : SQL- query ?
    private fun searchConversation(
        conversationDetails: List<ConversationItem>,
        searchQuery: String
    ): List<ConversationItem> {
        val matchingConversations = conversationDetails.filter { details ->
            when (details) {
                is ConversationItem.ConnectionConversation -> details.conversationInfo.name.contains(
                    searchQuery,
                    true
                )

                is ConversationItem.GroupConversation -> details.groupName.contains(
                    searchQuery,
                    true
                )

                is ConversationItem.PrivateConversation -> details.conversationInfo.name.contains(
                    searchQuery,
                    true
                )
            }
        }
        return matchingConversations
    }

    override suspend fun refreshMissingMetadata() {
        viewModelScope.launch {
            refreshUsersWithoutMetadata()
            refreshConversationsWithoutMetadata()
        }
    }

    @Suppress("ComplexMethod")
    private fun List<ConversationItem>.withFolders(source: ConversationsSource): Map<ConversationFolder, List<ConversationItem>> {
        return when (source) {
            ConversationsSource.ARCHIVE -> {
                buildMap {
                    if (this@withFolders.isNotEmpty()) {
                        put(ConversationFolder.WithoutHeader, this@withFolders)
                    }
                }
            }

            ConversationsSource.MAIN -> {
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

                        MutedConversationStatus.OnlyMentionsAndRepliesAllowed ->
                            when (it.badgeEventType) {
                                BadgeEventType.UnreadReply -> true
                                BadgeEventType.UnreadMention -> true
                                BadgeEventType.ReceivedConnectionRequest -> true
                                else -> false
                            }

                        MutedConversationStatus.AllMuted -> false
                    } || (it is ConversationItem.GroupConversation && it.hasOnGoingCall)
                }

                val remainingConversations = this - unreadConversations.toSet()

                buildMap {
                    if (unreadConversations.isNotEmpty()) {
                        put(ConversationFolder.Predefined.NewActivities, unreadConversations)
                    }
                    if (remainingConversations.isNotEmpty()) {
                        put(ConversationFolder.Predefined.Conversations, remainingConversations)
                    }
                }
            }
        }
    }

    override fun muteConversation(
        conversationId: ConversationId?,
        mutedConversationStatus: MutedConversationStatus
    ) {
        conversationId?.let {
            viewModelScope.launch {
                when (updateConversationMutedStatus(conversationId, mutedConversationStatus, Date().time)) {
                    ConversationUpdateStatusResult.Failure -> _infoMessage.emit(HomeSnackBarMessage.MutingOperationError)

                    ConversationUpdateStatusResult.Success ->
                        appLogger.d("MutedStatus changed for conversation: $conversationId to $mutedConversationStatus")
                }
            }
        }
    }

    override fun blockUser(blockUserState: BlockUserDialogState) {
        viewModelScope.launch {
            _requestInProgress = true
            val state = when (val result = blockUserUseCase(blockUserState.userId)) {
                BlockUserResult.Success -> {
                    appLogger.d("User ${blockUserState.userId} was blocked")
                    HomeSnackBarMessage.BlockingUserOperationSuccess(blockUserState.userName)
                }

                is BlockUserResult.Failure -> {
                    appLogger.d(
                        "Error while blocking user ${blockUserState.userId} ;" +
                                " Error ${result.coreFailure}"
                    )
                    HomeSnackBarMessage.BlockingUserOperationError
                }
            }
            _infoMessage.emit(state)
            _requestInProgress = false
        }
    }

    override fun unblockUser(userId: UserId) {
        viewModelScope.launch {
            _requestInProgress = true
            when (val result = unblockUserUseCase(userId)) {
                UnblockUserResult.Success -> {
                    appLogger.i("User $userId was unblocked")
                    closeBottomSheet.emit(Unit)
                }

                is UnblockUserResult.Failure -> {
                    appLogger.e(
                        "Error while unblocking user $userId ;" +
                                " Error ${result.coreFailure}"
                    )
                    _infoMessage.emit(HomeSnackBarMessage.UnblockingUserOperationError)
                }
            }
            _requestInProgress = false
        }
    }

    override fun leaveGroup(leaveGroupState: GroupDialogState) {
        viewModelScope.launch {
            _requestInProgress = true
            val response = leaveConversation(leaveGroupState.conversationId)
            when (response) {
                is RemoveMemberFromConversationUseCase.Result.Failure ->
                    _infoMessage.emit(HomeSnackBarMessage.LeaveConversationError)

                RemoveMemberFromConversationUseCase.Result.Success -> {
                    _infoMessage.emit(HomeSnackBarMessage.LeftConversationSuccess)
                }
            }
            _requestInProgress = false
        }
    }

    override fun deleteGroup(groupDialogState: GroupDialogState) {
        viewModelScope.launch {
            _requestInProgress = true
            when (deleteTeamConversation(groupDialogState.conversationId)) {
                is Result.Failure.GenericFailure -> _infoMessage.emit(HomeSnackBarMessage.DeleteConversationGroupError)
                Result.Failure.NoTeamFailure -> _infoMessage.emit(HomeSnackBarMessage.DeleteConversationGroupError)
                Result.Success -> _infoMessage.emit(
                    HomeSnackBarMessage.DeletedConversationGroupSuccess(groupDialogState.conversationName)
                )
            }
            _requestInProgress = false
        }
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    override fun addConversationToFavourites() {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    override fun moveConversationToFolder() {
    }

    override fun moveConversationToArchive(dialogState: DialogState, timestamp: Long) {
        with(dialogState) {
            viewModelScope.launch {
                val isArchiving = !isArchived

                _requestInProgress = true
                val result = updateConversationArchivedStatus(
                    conversationId = conversationId,
                    shouldArchiveConversation = isArchiving,
                    onlyLocally = !dialogState.isMember,
                    archivedStatusTimestamp = timestamp
                )
                _requestInProgress = false
                when (result) {
                    is ArchiveStatusUpdateResult.Failure -> {
                        _infoMessage.emit(
                            HomeSnackBarMessage.UpdateArchivingStatusError(
                                isArchiving
                            )
                        )
                    }

                    is ArchiveStatusUpdateResult.Success -> {
                        _infoMessage.emit(
                            HomeSnackBarMessage.UpdateArchivingStatusSuccess(
                                isArchiving
                            )
                        )
                    }
                }
            }
        }
    }

    override fun clearConversationContent(dialogState: DialogState) {
        viewModelScope.launch {
            _requestInProgress = true
            with(dialogState) {
                val result = clearConversationContentUseCase(conversationId)
                _requestInProgress = false
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
            _infoMessage.emit(HomeSnackBarMessage.ClearConversationContentFailure(isGroup))
        } else {
            _infoMessage.emit(HomeSnackBarMessage.ClearConversationContentSuccess(isGroup))
        }
    }
}

fun Conversation.LegalHoldStatus.showLegalHoldIndicator() = this == Conversation.LegalHoldStatus.ENABLED

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
            isLegalHold = conversation.legalHoldStatus.showLegalHoldIndicator(),
            lastMessageContent = lastMessage.toUIPreview(unreadEventCount),
            badgeEventType = parseConversationEventType(
                conversation.mutedStatus,
                unreadEventCount
            ),
            hasOnGoingCall = hasOngoingCall && this.isSelfUserMember,
            isSelfUserCreator = isSelfUserCreator,
            isSelfUserMember = isSelfUserMember,
            teamId = conversation.teamId,
            selfMemberRole = selfRole,
            isArchived = conversation.archived,
            mlsVerificationStatus = conversation.mlsVerificationStatus,
            proteusVerificationStatus = conversation.proteusVerificationStatus
        )
    }

    is OneOne -> {
        ConversationItem.PrivateConversation(
            userAvatarData = UserAvatarData(
                asset = otherUser.previewPicture?.let { UserAvatarAsset(wireSessionImageLoader, it) },
                availabilityStatus = otherUser.availabilityStatus,
                connectionState = otherUser.connectionStatus,
                nameBasedAvatar = NameBasedAvatar(otherUser.name, otherUser.accentId)
            ),
            conversationInfo = ConversationInfo(
                name = otherUser.name.orEmpty(),
                membership = userTypeMapper.toMembership(userType),
                isSenderUnavailable = otherUser.isUnavailableUser
            ),
            conversationId = conversation.id,
            mutedStatus = conversation.mutedStatus,
            isLegalHold = conversation.legalHoldStatus.showLegalHoldIndicator(),
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
            teamId = otherUser.teamId,
            isArchived = conversation.archived,
            mlsVerificationStatus = conversation.mlsVerificationStatus,
            proteusVerificationStatus = conversation.proteusVerificationStatus
        )
    }

    is Connection -> {
        ConversationItem.ConnectionConversation(
            userAvatarData = UserAvatarData(
                asset = otherUser?.previewPicture?.let { UserAvatarAsset(wireSessionImageLoader, it) },
                availabilityStatus = otherUser?.availabilityStatus ?: UserAvailabilityStatus.NONE,
                nameBasedAvatar = NameBasedAvatar(otherUser?.name, otherUser?.accentId ?: -1)
            ),
            conversationInfo = ConversationInfo(
                name = otherUser?.name.orEmpty(),
                membership = userTypeMapper.toMembership(userType),
                isSenderUnavailable = otherUser?.isUnavailableUser ?: true
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
    if (connectionState == ConnectionState.SENT) {
        BadgeEventType.SentConnectRequest
    } else {
        BadgeEventType.ReceivedConnectionRequest
    }

fun parsePrivateConversationEventType(
    connectionState: ConnectionState,
    isDeleted: Boolean,
    eventType: BadgeEventType
) =
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
