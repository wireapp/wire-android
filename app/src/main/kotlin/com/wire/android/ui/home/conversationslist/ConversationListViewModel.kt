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
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import com.wire.android.appLogger
import com.wire.android.model.SnackBarMessage
import com.wire.android.ui.common.bottomsheet.conversation.ConversationTypeDetail
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.HomeSnackBarMessage
import com.wire.android.ui.home.conversations.search.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.android.ui.home.conversations.usecase.GetConversationsFromSearchUseCase
import com.wire.android.ui.home.conversationslist.common.previewConversationFoldersFlow
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationFolderItem
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserResult
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.ArchiveStatusUpdateResult
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
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
import kotlinx.coroutines.flow.Flow
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
    foldersWithConversations: Flow<PagingData<ConversationFolderItem>> = previewConversationFoldersFlow(),
) : ConversationListViewModel {
    override val conversationListState = ConversationListState(foldersWithConversations)
}

@Suppress("MagicNumber", "TooManyFunctions", "LongParameterList")
@HiltViewModel(assistedFactory = ConversationListViewModelImpl.Factory::class)
class ConversationListViewModelImpl @AssistedInject constructor(
    @Assisted val conversationsSource: ConversationsSource,
    dispatcher: DispatcherProvider,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val getConversationsPaginated: GetConversationsFromSearchUseCase,
    private val leaveConversation: LeaveConversationUseCase,
    private val deleteTeamConversation: DeleteTeamConversationUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val unblockUserUseCase: UnblockUserUseCase,
    private val clearConversationContentUseCase: ClearConversationContentUseCase,
    private val refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase,
    private val refreshConversationsWithoutMetadata: RefreshConversationsWithoutMetadataUseCase,
    private val updateConversationArchivedStatus: UpdateConversationArchivedStatusUseCase,
) : ConversationListViewModel, ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(conversationsSource: ConversationsSource): ConversationListViewModelImpl
    }

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    override val infoMessage = _infoMessage.asSharedFlow()

    private var _requestInProgress: Boolean by mutableStateOf(false)
    override val requestInProgress: Boolean get() = _requestInProgress

    override val closeBottomSheet = MutableSharedFlow<Unit>()

    private val searchQueryFlow: MutableStateFlow<String> = MutableStateFlow("")

    private val containsNewActivitiesSection = conversationsSource == ConversationsSource.MAIN
    private val conversationsFlow: Flow<PagingData<ConversationFolderItem>> = searchQueryFlow
        .debounce { if (it.isEmpty()) 0L else DEFAULT_SEARCH_QUERY_DEBOUNCE }
        .onStart { emit("") }
        .distinctUntilChanged()
        .flatMapLatest { searchQuery ->
            getConversationsPaginated(
                searchQuery = searchQuery,
                fromArchive = conversationsSource == ConversationsSource.ARCHIVE,
                onlyInteractionEnabled = false,
                newActivitiesOnTop = containsNewActivitiesSection,
            ).map {
                it.insertSeparators { before, after ->
                    when {
                        // do not add separators if the list shouldn't show conversations grouped into different folders
                        !containsNewActivitiesSection -> null

                        before == null && after != null && after.hasNewActivitiesToShow ->
                            // list starts with items with "new activities"
                            ConversationFolder.Predefined.NewActivities

                        before == null && after != null && !after.hasNewActivitiesToShow ->
                            // list doesn't contain any items with "new activities"
                            ConversationFolder.Predefined.Conversations

                        before != null && before.hasNewActivitiesToShow && after != null && !after.hasNewActivitiesToShow ->
                            // end of "new activities" section and beginning of "conversations" section
                            ConversationFolder.Predefined.Conversations

                        else -> null
                    }
                }
            }
        }
        .flowOn(dispatcher.io())

    override val conversationListState: ConversationListState = ConversationListState(
        foldersWithConversations = conversationsFlow
    )

    override fun searchQueryChanged(searchQuery: String) {
        viewModelScope.launch {
            searchQueryFlow.emit(searchQuery)
        }
    }

    override suspend fun refreshMissingMetadata() {
        viewModelScope.launch {
            refreshUsersWithoutMetadata()
            refreshConversationsWithoutMetadata()
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
