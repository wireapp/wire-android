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
 */
package com.wire.android.ui.home.conversationslist.bottomsheet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.HomeSnackbarState
import com.wire.android.ui.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.AssetId
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserResult
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// TODO: cover with unit test
@HiltViewModel
class ConversationBottomSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val leaveConversation: LeaveConversationUseCase,
    private val deleteTeamConversation: DeleteTeamConversationUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val unblockUserUseCase: UnblockUserUseCase,
    private val dispatcher: DispatcherProvider,
    private val navigationManager: NavigationManager,
    private val clearConversationContent: ClearConversationContentUseCase,
) : ViewModel() {
    var dialogBottomSheetState: DialogBottomSheetState by mutableStateOf(DialogBottomSheetState())
        private set

    val homeSnackBarState = MutableSharedFlow<HomeSnackbarState>()

    val navArg: ConversationBottomSheetNavArg = savedStateHandle.navArgs()

    fun showDeleteConversationDialog() {
        dialogBottomSheetState = dialogBottomSheetState.copy(showDeleteConversationDialog = true)
    }

    fun dismissDeleteConversationDialog() {
        dialogBottomSheetState = dialogBottomSheetState.copy(showDeleteConversationDialog = false)
    }

    fun showLeaveConversationDialog() {
        dialogBottomSheetState = dialogBottomSheetState.copy(showLeaveConversationDialog = true)
    }

    fun dismissLeaveConversationDialog() {
        dialogBottomSheetState = dialogBottomSheetState.copy(showLeaveConversationDialog = false)
    }

    fun showBlockUserDialog() {
        dialogBottomSheetState = dialogBottomSheetState.copy(showBlockUserDialog = true)
    }

    fun dismissBlockUserDialog() {
        dialogBottomSheetState = dialogBottomSheetState.copy(showBlockUserDialog = false)
    }

    fun showUnBlockUserDialog() {
        dialogBottomSheetState = dialogBottomSheetState.copy(showUnblockUserDialog = true)
    }

    fun dismissUnBlockUserDialog() {
        dialogBottomSheetState = dialogBottomSheetState.copy(showUnblockUserDialog = false)
    }

    fun showClearContentDialog() {
        dialogBottomSheetState = dialogBottomSheetState.copy(showClearContentDialog = true)
    }

    fun dismissClearContentDialog() {
        dialogBottomSheetState = dialogBottomSheetState.copy(showClearContentDialog = false)
    }

    fun loadAvatar(assetId: AssetId) = ImageAsset.UserAvatarAsset(wireSessionImageLoader, assetId)

    fun blockUser(onSuccess: () -> Unit) {
        viewModelScope.launch {
            dialogBottomSheetState.copy(isLoading = true)
            navArg.userId?.let {
                val result = withContext(dispatcher.io()) { blockUserUseCase(it) }
                when (result) {
                    BlockUserResult.Success -> {
                        onSuccess()
//                    HomeSnackbarState.BlockingUserOperationSuccess(blockUserState.userName)
                    }

                    is BlockUserResult.Failure -> {
                        appLogger.e("Error while blocking user ${navArg.userId} ; Error ${result.coreFailure}")
                        HomeSnackbarState.BlockingUserOperationError
                    }
                }
            }

//            homeSnackBarState.emit(state)
            dialogBottomSheetState.copy(isLoading = false)
        }
    }

    fun unblockUser(onSuccess: () -> Unit) {
        viewModelScope.launch {
            dialogBottomSheetState.copy(isLoading = true)
            navArg.userId?.let {
                val result = withContext(dispatcher.io()) { unblockUserUseCase(it) }
                when (result) {
                    UnblockUserResult.Success -> {
//                    closeBottomSheet.emit(Unit)
                        onSuccess()
                    }

                    is UnblockUserResult.Failure -> {
                        appLogger.e("Error while unblocking user ${navArg.userId} ; Error ${result.coreFailure}")
                        homeSnackBarState.emit(HomeSnackbarState.UnblockingUserOperationError)
                    }
                }
            }
            dialogBottomSheetState.copy(isLoading = false)
        }
    }

    fun clearConversationContent(onSuccess: () -> Unit) {
        viewModelScope.launch {
            dialogBottomSheetState.copy(isLoading = true)
            val result = withContext(dispatcher.io()) { clearConversationContent(navArg.conversationId) }
            when (result) {
                is ClearConversationContentUseCase.Result.Failure ->
                    homeSnackBarState.emit(HomeSnackbarState.LeaveConversationError)

                ClearConversationContentUseCase.Result.Success -> {
                    homeSnackBarState.emit(HomeSnackbarState.LeftConversationSuccess)
                    onSuccess()
                }
            }
            dialogBottomSheetState.copy(isLoading = false)
        }
    }

    fun leaveGroup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            dialogBottomSheetState.copy(isLoading = true)
            val response = withContext(dispatcher.io()) {
                leaveConversation(navArg.conversationId)
            }

            when (response) {
                is RemoveMemberFromConversationUseCase.Result.Failure ->
                    homeSnackBarState.emit(HomeSnackbarState.LeaveConversationError)

                RemoveMemberFromConversationUseCase.Result.Success -> {
                    homeSnackBarState.emit(HomeSnackbarState.LeftConversationSuccess)
                    onSuccess()
                }
            }
            dialogBottomSheetState.copy(isLoading = false)
        }
    }

    fun deleteGroup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            dialogBottomSheetState.copy(isLoading = true)
            when (withContext(dispatcher.io()) { deleteTeamConversation(navArg.conversationId) }) {
                is DeleteTeamConversationUseCase.Result.Failure.GenericFailure -> homeSnackBarState.emit(HomeSnackbarState.DeleteConversationGroupError)
                DeleteTeamConversationUseCase.Result.Failure.NoTeamFailure -> homeSnackBarState.emit(HomeSnackbarState.DeleteConversationGroupError)
                DeleteTeamConversationUseCase.Result.Success -> {
                    homeSnackBarState.emit(
                        HomeSnackbarState.DeletedConversationGroupSuccess(navArg.conversationName)
                    )
                    onSuccess()
                }

                else -> {}
            }
            dialogBottomSheetState.copy(isLoading = false)
        }
    }
}
