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

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wire.android.R
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.collectAsStateLifecycleAware
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.ArchiveConversationDialog
import com.wire.android.ui.common.dialogs.BlockUserDialogContent
import com.wire.android.ui.common.dialogs.UnblockUserDialogContent
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.home.conversations.details.dialog.ClearConversationContentDialog
import com.wire.android.ui.home.conversations.details.menu.DeleteConversationGroupDialog
import com.wire.android.ui.home.conversations.details.menu.DeleteConversationGroupLocallyDialog
import com.wire.android.ui.home.conversations.details.menu.LeaveConversationGroupDialog
import com.wire.android.ui.home.conversations.folder.ConversationFoldersNavArgs
import com.wire.kalium.logic.data.id.ConversationId

@SuppressLint("ComposeModifierMissing")
@Composable
fun ConversationOptionsModalSheetLayout(
    sheetState: WireModalSheetState<ConversationSheetState>,
    openConversationFolders: (ConversationFoldersNavArgs) -> Unit,
    onLeftConversation: () -> Unit = {},
    onDeletedConversation: () -> Unit = {},
    onDeletedConversationLocally: () -> Unit = {},
    openConversationDebugMenu: (ConversationId) -> Unit = {},
    viewModel: ConversationOptionsMenuViewModel =
        hiltViewModelScoped<ConversationOptionsMenuViewModelImpl, ConversationOptionsMenuViewModel>()
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    LeaveConversationGroupDialog(
        dialogState = viewModel.leaveGroupDialogState,
        onLeaveGroup = { viewModel.leaveGroup(it.conversationId, it.conversationName, it.shouldDelete) }
    )
    DeleteConversationGroupDialog(
        dialogState = viewModel.deleteGroupDialogState,
        onDeleteGroup = { viewModel.deleteGroup(it.conversationId, it.conversationName) }
    )
    DeleteConversationGroupLocallyDialog(
        dialogState = viewModel.deleteGroupLocallyDialogState,
        onDeleteGroupLocally = { viewModel.deleteGroupLocally(it.conversationId, it.conversationName) }
    )
    BlockUserDialogContent(
        dialogState = viewModel.blockUserDialogState,
        onBlock = { viewModel.blockUser(it.userId, it.userName) }
    )
    UnblockUserDialogContent(
        dialogState = viewModel.unblockUserDialogState,
        onUnblock = { viewModel.unblockUser(it.userId, it.userName) },
    )
    ClearConversationContentDialog(
        dialogState = viewModel.clearContentDialogState,
        onClearConversationContent = { viewModel.clearConversationContent(it.conversationId, it.conversationTypeDetail) }
    )
    ArchiveConversationDialog(
        dialogState = viewModel.archiveConversationDialogState,
        onArchiveButtonClicked = { viewModel.moveToArchive(it.conversationId, !it.isArchived, !it.isMember) }
    )
    HandleActions(viewModel.actions) { action ->
        when (action) {
            is ConversationOptionsMenuViewAction.Deleted -> onDeletedConversation()
            is ConversationOptionsMenuViewAction.DeletedLocally -> onDeletedConversationLocally()
            is ConversationOptionsMenuViewAction.Left -> onLeftConversation()
            is ConversationOptionsMenuViewAction.Message -> sheetState.hide {
                snackbarHostState.showSnackbar(action.message.uiText.asString(context.resources))
            }
        }
    }

    WireModalSheetLayout(
        sheetState = sheetState,
        sheetContent = { conversationSheetState ->
            val state = viewModel.observeConversationStateFlow(conversationSheetState.conversationId).collectAsStateLifecycleAware().value
            when (state) {
                is ConversationOptionsMenuState.Conversation -> ConversationSheetContent(
                    // show the sheet with proper content
                    conversationOptionsData = state.conversation,
                    conversationSheetState = conversationSheetState,
                    changeFavoriteState = {
                        sheetState.hide { viewModel.changeFavoriteState(it.conversationId, it.conversationName, it.addToFavorite) }
                    },
                    moveConversationToFolder = {
                        sheetState.hide { openConversationFolders(it) }
                    },
                    removeFromFolder = {
                        sheetState.hide { viewModel.removeFromFolder(it.conversationId, it.conversationName, it.folder) }
                    },
                    updateConversationArchiveStatus = {
                        sheetState.hide {
                            when {
                                it.isArchived -> viewModel.moveToArchive(it.conversationId, false, !it.isMember)
                                else -> viewModel.archiveConversationDialogState.show(it)
                            }
                        }
                    },
                    clearConversationContent = {
                        sheetState.hide { viewModel.clearContentDialogState.show(it) }
                    },
                    blockUser = {
                        sheetState.hide { viewModel.blockUserDialogState.show(it) }
                    },
                    unblockUser = {
                        sheetState.hide { viewModel.unblockUserDialogState.show(it) }
                    },
                    leaveGroup = {
                        sheetState.hide { viewModel.leaveGroupDialogState.show(it) }
                    },
                    deleteGroup = {
                        sheetState.hide { viewModel.deleteGroupDialogState.show(it) }
                    },
                    deleteGroupLocally = {
                        sheetState.hide { viewModel.deleteGroupLocallyDialogState.show(it) }
                    },
                    updateMutedConversationStatus = viewModel::changeMutedState,
                    openDebugMenu = { conversationId ->
                        sheetState.hide { openConversationDebugMenu(conversationId) }
                    }
                )

                ConversationOptionsMenuState.Loading -> WireCircularProgressIndicator( // loading state - show a progress indicator
                    progressColor = colorsScheme().onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                ConversationOptionsMenuState.NotAvailable -> sheetState.hide { // conversation not found - hide the sheet and show info
                    snackbarHostState.showSnackbar(context.getString(R.string.deleted_conversation_options_closed))
                }
            }
        },
    )
}
