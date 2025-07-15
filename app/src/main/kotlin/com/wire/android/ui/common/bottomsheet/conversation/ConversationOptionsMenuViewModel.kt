/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.common.bottomsheet.conversation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.wire.android.appLogger
import com.wire.android.di.ViewModelScopedPreview
import com.wire.android.model.SnackBarMessage
import com.wire.android.ui.common.ActionsManager
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.home.HomeSnackBarMessage
import com.wire.android.ui.home.conversationslist.model.DeleteGroupDialogState
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.LeaveGroupDialogState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.workmanager.worker.ConversationDeletionLocallyStatus
import com.wire.android.workmanager.worker.enqueueConversationDeletionLocally
import com.wire.android.workmanager.worker.observeConversationDeletionStatusLocally
import com.wire.kalium.logic.data.conversation.ConversationFolder
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
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationArchivedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.conversation.folder.AddConversationToFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFolderUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.team.Result
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.util.DateTimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@ViewModelScopedPreview
interface ConversationOptionsMenuViewModel : ActionsManager<ConversationOptionsMenuViewAction> {
    val leaveGroupDialogState: VisibilityState<LeaveGroupDialogState> get() = VisibilityState()
    val deleteGroupDialogState: VisibilityState<DeleteGroupDialogState> get() = VisibilityState()
    val deleteGroupLocallyDialogState: VisibilityState<DeleteGroupDialogState> get() = VisibilityState()
    val blockUserDialogState: VisibilityState<BlockUserDialogState> get() = VisibilityState()
    val unblockUserDialogState: VisibilityState<UnblockUserDialogState> get() = VisibilityState()
    val clearContentDialogState: VisibilityState<DialogState> get() = VisibilityState()
    val archiveConversationDialogState: VisibilityState<DialogState> get() = VisibilityState()

    fun observeConversationStateFlow(conversationId: ConversationId): StateFlow<ConversationOptionsMenuState> =
        MutableStateFlow(ConversationOptionsMenuState.Loading)

    fun changeFavoriteState(conversationId: ConversationId, conversationName: String, addToFavorite: Boolean) {}
    fun removeFromFolder(conversationId: ConversationId, conversationName: String, folder: ConversationFolder) {}
    fun changeMutedState(conversationId: ConversationId, mutedConversationStatus: MutedConversationStatus) {}
    fun leaveGroup(conversationId: ConversationId, conversationName: String, shouldDelete: Boolean) {}
    fun deleteGroupLocally(conversationId: ConversationId, conversationName: String) {}
    fun deleteGroup(conversationId: ConversationId, conversationName: String) {}
    fun blockUser(userId: UserId, userName: String) {}
    fun unblockUser(userId: UserId, userName: String) {}
    fun clearConversationContent(conversationId: ConversationId, conversationTypeDetail: ConversationTypeDetail) {}
    fun moveToArchive(conversationId: ConversationId, shouldArchive: Boolean, isSelfAMember: Boolean) {}
}

@HiltViewModel
class ConversationOptionsMenuViewModelImpl @Inject constructor(
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeSelfUser: ObserveSelfUserUseCase,
    private val addConversationToFavorites: AddConversationToFavoritesUseCase,
    private val removeConversationFromFavorites: RemoveConversationFromFavoritesUseCase,
    private val removeConversationFromFolder: RemoveConversationFromFolderUseCase,
    private val updateConversationArchivedStatus: UpdateConversationArchivedStatusUseCase,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val deleteTeamConversation: DeleteTeamConversationUseCase,
    private val leaveConversation: LeaveConversationUseCase,
    private val blockUser: BlockUserUseCase,
    private val unblockUser: UnblockUserUseCase,
    private val clearConversationContent: ClearConversationContentUseCase,
    private val workManager: WorkManager,
    private val dispatchers: DispatcherProvider,
) : ConversationOptionsMenuViewModel, ActionsViewModel<ConversationOptionsMenuViewAction>() {
    private val conversationStateFlow: ConcurrentHashMap<ConversationId, StateFlow<ConversationOptionsMenuState>> = ConcurrentHashMap()
    override val leaveGroupDialogState: VisibilityState<LeaveGroupDialogState> by mutableStateOf(VisibilityState())
    override val deleteGroupDialogState: VisibilityState<DeleteGroupDialogState> by mutableStateOf(VisibilityState())
    override val deleteGroupLocallyDialogState: VisibilityState<DeleteGroupDialogState> by mutableStateOf(VisibilityState())
    override val blockUserDialogState: VisibilityState<BlockUserDialogState> by mutableStateOf(VisibilityState())
    override val unblockUserDialogState: VisibilityState<UnblockUserDialogState> by mutableStateOf(VisibilityState())
    override val clearContentDialogState: VisibilityState<DialogState> by mutableStateOf(VisibilityState())
    override val archiveConversationDialogState: VisibilityState<DialogState> by mutableStateOf(VisibilityState())

    override fun observeConversationStateFlow(conversationId: ConversationId): StateFlow<ConversationOptionsMenuState> =
        conversationStateFlow.getOrPut(conversationId) {
            flowOf(conversationId)
                .flatMapConcat {
                    combine(
                        observeSelfUser(),
                        observeConversationDetails(conversationId),
                        observeIsDeletingConversationLocally(conversationId)
                    ) { selfUser, conversationDetailResult, isDeletingConversationLocallyRunning ->
                        when (conversationDetailResult) {
                            is ObserveConversationDetailsUseCase.Result.Success ->
                                conversationDetailResult.conversationDetails
                                    .toConversationOptionsData(selfUser, isDeletingConversationLocallyRunning)
                                    ?.let { ConversationOptionsMenuState.Conversation(it) } ?: ConversationOptionsMenuState.NotAvailable
                            is ObserveConversationDetailsUseCase.Result.Failure -> ConversationOptionsMenuState.NotAvailable
                        }
                    }
                }
                .distinctUntilChanged()
                .onCompletion { conversationStateFlow.remove(conversationId) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 500L),
                    initialValue = ConversationOptionsMenuState.Loading,
                )
        }

    override fun changeFavoriteState(conversationId: ConversationId, conversationName: String, addToFavorite: Boolean) {
        viewModelScope.launch {
            if (addToFavorite) {
                withContext(dispatchers.io()) {
                    addConversationToFavorites(conversationId)
                }.let { result ->
                    when (result) {
                        is AddConversationToFavoritesUseCase.Result.Failure ->
                            onMessage(HomeSnackBarMessage.UpdateFavoriteStatusError(conversationName, true))
                        AddConversationToFavoritesUseCase.Result.Success ->
                            onMessage(HomeSnackBarMessage.UpdateFavoriteStatusSuccess(conversationName, true))
                    }
                }
            } else {
                withContext(dispatchers.io()) {
                    removeConversationFromFavorites(conversationId)
                }.let { result ->
                    when (result) {
                        is RemoveConversationFromFavoritesUseCase.Result.Failure ->
                            onMessage(HomeSnackBarMessage.UpdateFavoriteStatusError(conversationName, false))
                        RemoveConversationFromFavoritesUseCase.Result.Success ->
                            onMessage(HomeSnackBarMessage.UpdateFavoriteStatusSuccess(conversationName, false))
                    }
                }
            }
        }
    }

    override fun removeFromFolder(conversationId: ConversationId, conversationName: String, folder: ConversationFolder) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                removeConversationFromFolder.invoke(conversationId, folder.id)
            }.let { result ->
                when (result) {
                    is RemoveConversationFromFolderUseCase.Result.Failure ->
                        onMessage(HomeSnackBarMessage.RemoveFromFolderError(conversationName))
                    RemoveConversationFromFolderUseCase.Result.Success ->
                        onMessage(HomeSnackBarMessage.RemoveFromFolderSuccess(conversationName, folder.name))
                }
            }
        }
    }

    override fun moveToArchive(conversationId: ConversationId, shouldArchive: Boolean, isSelfAMember: Boolean) {
        viewModelScope.launch {
            archiveConversationDialogState.update { it.copy(loading = true) }
            withContext(dispatchers.io()) {
                updateConversationArchivedStatus(
                    conversationId = conversationId,
                    shouldArchiveConversation = shouldArchive,
                    onlyLocally = !isSelfAMember,
                    archivedStatusTimestamp = DateTimeUtil.currentInstant().toEpochMilliseconds()
                )
            }.let { result ->
                when (result) {
                    is ArchiveStatusUpdateResult.Failure -> onMessage(HomeSnackBarMessage.UpdateArchivingStatusError(shouldArchive))
                    is ArchiveStatusUpdateResult.Success -> onMessage(HomeSnackBarMessage.UpdateArchivingStatusSuccess(shouldArchive))
                }
            }
            archiveConversationDialogState.dismiss()
        }
    }

    override fun changeMutedState(conversationId: ConversationId, mutedConversationStatus: MutedConversationStatus) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                updateConversationMutedStatus(
                    conversationId = conversationId,
                    mutedConversationStatus = mutedConversationStatus,
                    mutedStatusTimestamp = DateTimeUtil.currentInstant().toEpochMilliseconds()
                )
            }.let { result ->
                when (result) {
                    ConversationUpdateStatusResult.Failure -> onMessage(HomeSnackBarMessage.MutingOperationError)
                    ConversationUpdateStatusResult.Success ->
                        appLogger.d("MutedStatus changed for conversation: $conversationId to $mutedConversationStatus")
                }
            }
        }
    }

    override fun leaveGroup(conversationId: ConversationId, conversationName: String, shouldDelete: Boolean) {
        viewModelScope.launch {
            leaveGroupDialogState.update { it.copy(loading = true) }
            withContext(dispatchers.io()) {
                leaveConversation(conversationId)
            }.let { result ->
                when (result) {
                    is RemoveMemberFromConversationUseCase.Result.Failure -> onMessage(HomeSnackBarMessage.LeaveConversationError)
                    is RemoveMemberFromConversationUseCase.Result.Success -> when {
                        shouldDelete -> when (enqueueDeleteGroupLocally(conversationId)) {
                            false -> onMessage(HomeSnackBarMessage.LeaveConversationError)
                            true -> {
                                sendAction(ConversationOptionsMenuViewAction.Left(conversationId, conversationName))
                                onMessage(HomeSnackBarMessage.LeftConversationSuccess)
                            }
                        }

                        !shouldDelete -> {
                            sendAction(ConversationOptionsMenuViewAction.Left(conversationId, conversationName))
                            onMessage(HomeSnackBarMessage.LeftConversationSuccess)
                        }
                    }
                }
            }
            leaveGroupDialogState.dismiss()
        }
    }

    override fun deleteGroupLocally(conversationId: ConversationId, conversationName: String) {
        viewModelScope.launch {
            deleteGroupLocallyDialogState.update { it.copy(loading = true) }
            when (enqueueDeleteGroupLocally(conversationId)) {
                true -> {
                    sendAction(ConversationOptionsMenuViewAction.DeletedLocally(conversationId, conversationName))
                    onMessage(HomeSnackBarMessage.DeleteConversationGroupLocallySuccess(conversationName))
                }
                false -> onMessage(HomeSnackBarMessage.DeleteConversationGroupError)
            }
            deleteGroupLocallyDialogState.dismiss()
        }
    }

    private suspend fun enqueueDeleteGroupLocally(conversationId: ConversationId): Boolean =
        workManager.enqueueConversationDeletionLocally(conversationId)
            .first { it == ConversationDeletionLocallyStatus.SUCCEEDED || it == ConversationDeletionLocallyStatus.FAILED }
            .let { it == ConversationDeletionLocallyStatus.SUCCEEDED }

    override fun deleteGroup(conversationId: ConversationId, conversationName: String) {
        viewModelScope.launch {
            deleteGroupDialogState.update { it.copy(loading = true) }
            withContext(dispatchers.io()) {
                deleteTeamConversation(conversationId)
            }.let { result ->
                when (result) {
                    is Result.Failure -> onMessage(HomeSnackBarMessage.DeleteConversationGroupError)
                    Result.Failure.NoTeamFailure -> onMessage(HomeSnackBarMessage.DeleteConversationGroupError)
                    Result.Success -> {
                        sendAction(ConversationOptionsMenuViewAction.Deleted(conversationId, conversationName))
                        onMessage(HomeSnackBarMessage.DeletedConversationGroupSuccess(conversationName))
                    }

                }
            }
            deleteGroupDialogState.dismiss()
        }
    }

    override fun blockUser(userId: UserId, userName: String) {
        viewModelScope.launch {
            blockUserDialogState.update { it.copy(loading = true) }
            withContext(dispatchers.io()) {
                blockUser(userId)
            }.let { result ->
                when (result) {
                    BlockUserResult.Success -> {
                        appLogger.d("User $userId was blocked")
                        onMessage(HomeSnackBarMessage.BlockingUserOperationSuccess(userName))
                    }

                    is BlockUserResult.Failure -> {
                        appLogger.d("Error while blocking user $userId ; Error ${result.coreFailure}")
                        onMessage(HomeSnackBarMessage.BlockingUserOperationError)
                    }
                }
            }
            blockUserDialogState.dismiss()
        }
    }

    override fun unblockUser(userId: UserId, userName: String) {
        viewModelScope.launch {
            unblockUserDialogState.update { it.copy(loading = true) }
            withContext(dispatchers.io()) {
                unblockUser(userId)
            }.let { result ->
                when (result) {
                    UnblockUserResult.Success -> {
                        appLogger.i("User $userId was unblocked")
                        onMessage(HomeSnackBarMessage.UnblockingUserOperationSuccess(userName))
                    }

                    is UnblockUserResult.Failure -> {
                        appLogger.e("Error while unblocking user $userId ; Error ${result.coreFailure}")
                        onMessage(HomeSnackBarMessage.UnblockingUserOperationError)
                    }
                }
            }
            unblockUserDialogState.dismiss()
        }
    }

    override fun clearConversationContent(conversationId: ConversationId, conversationTypeDetail: ConversationTypeDetail) {
        viewModelScope.launch {
            clearContentDialogState.update { it.copy(loading = true) }
            withContext(dispatchers.io()) {
                clearConversationContent(conversationId)
            }.let { result ->
                if (conversationTypeDetail is ConversationTypeDetail.Connection) {
                    throw IllegalStateException("Unsupported conversation type to clear content, something went wrong?")
                }
                val isGroup = conversationTypeDetail is ConversationTypeDetail.Group
                when (result) {
                    is ClearConversationContentUseCase.Result.Failure ->
                        onMessage(HomeSnackBarMessage.ClearConversationContentFailure(isGroup))

                    ClearConversationContentUseCase.Result.Success ->
                        onMessage(HomeSnackBarMessage.ClearConversationContentSuccess(isGroup))
                }
            }
            clearContentDialogState.dismiss()
        }
    }

    private fun observeIsDeletingConversationLocally(conversationId: ConversationId): Flow<Boolean> {
        return workManager.observeConversationDeletionStatusLocally(conversationId)
            .map { status -> status == ConversationDeletionLocallyStatus.RUNNING }
            .distinctUntilChanged()
    }

    private fun onMessage(message: SnackBarMessage) = sendAction(ConversationOptionsMenuViewAction.Message(message))
}

sealed interface ConversationOptionsMenuState {
    data object Loading : ConversationOptionsMenuState
    data object NotAvailable : ConversationOptionsMenuState
    data class Conversation(val conversation: ConversationOptionsData) : ConversationOptionsMenuState
}

sealed interface ConversationOptionsMenuViewAction {
    data class Message(val message: SnackBarMessage) : ConversationOptionsMenuViewAction
    data class Left(val conversationId: ConversationId, val conversationName: String) : ConversationOptionsMenuViewAction
    data class Deleted(val conversationId: ConversationId, val conversationName: String) : ConversationOptionsMenuViewAction
    data class DeletedLocally(val conversationId: ConversationId, val conversationName: String) : ConversationOptionsMenuViewAction
}
