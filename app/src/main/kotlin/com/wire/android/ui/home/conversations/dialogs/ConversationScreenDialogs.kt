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
package com.wire.android.ui.home.conversations.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.wire.android.ui.common.dialogs.calling.CallingFeatureUnavailableDialog
import com.wire.android.ui.common.dialogs.calling.JoinAnywayDialog
import com.wire.android.ui.common.dialogs.calling.OngoingActiveCallDialog
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.home.conversations.AssetTooLargeDialog
import com.wire.android.ui.home.conversations.AssetTooLargeDialogState
import com.wire.android.ui.home.conversations.MessageComposerViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewModel
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState.Visible
import com.wire.android.ui.home.conversations.dialogs.ConversationScreenDialogType.ASSET_TOO_LARGE
import com.wire.android.ui.home.conversations.dialogs.ConversationScreenDialogType.CALLING_FEATURE_UNAVAILABLE
import com.wire.android.ui.home.conversations.dialogs.ConversationScreenDialogType.DELETE_MESSAGE
import com.wire.android.ui.home.conversations.dialogs.ConversationScreenDialogType.DOWNLOADED_ASSET
import com.wire.android.ui.home.conversations.dialogs.ConversationScreenDialogType.JOIN_CALL_ANYWAY
import com.wire.android.ui.home.conversations.dialogs.ConversationScreenDialogType.NONE
import com.wire.android.ui.home.conversations.dialogs.ConversationScreenDialogType.NO_CONNECTIVITY
import com.wire.android.ui.home.conversations.dialogs.ConversationScreenDialogType.ONGOING_ACTIVE_CALL
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.DownloadedAssetDialogVisibilityState.Displayed
import com.wire.kalium.logic.NetworkFailure

/**
 * This class is meant to handle the visibility of the different dialogs that can be displayed in the conversation screen. Each dialog can
 * have its own internal state and logic (or not)
 */
@Composable
fun ConversationScreenDialogs(
    conversationCallViewModel: ConversationCallViewModel,
    messageComposerViewModel: MessageComposerViewModel,
    currentConversationScreenDialog: ConversationScreenDialogType,
    conversationMessagesViewModel: ConversationMessagesViewModel,
    updateDialogType: (ConversationScreenDialogType) -> Unit
) {

    LaunchedEffect(conversationMessagesViewModel.conversationViewState.downloadedAssetDialogState) {
        if (conversationMessagesViewModel.conversationViewState.downloadedAssetDialogState is Displayed) {
            updateDialogType(DOWNLOADED_ASSET)
        }
    }

    LaunchedEffect(conversationCallViewModel.conversationCallViewState.shouldShowJoinAnywayDialog) {
        if (conversationCallViewModel.conversationCallViewState.shouldShowJoinAnywayDialog) {
            updateDialogType(JOIN_CALL_ANYWAY)
        }
    }

    LaunchedEffect(messageComposerViewModel.deleteMessageDialogsState) {
        val showDeleteMessageDialog = messageComposerViewModel.deleteMessageDialogsState.forEveryone is Visible
                || messageComposerViewModel.deleteMessageDialogsState.forYourself is Visible
        if (showDeleteMessageDialog) {
            updateDialogType(DELETE_MESSAGE)
        }
    }

    LaunchedEffect(messageComposerViewModel.messageComposerViewState) {
        if (messageComposerViewModel.messageComposerViewState.assetTooLargeDialogState is AssetTooLargeDialogState.Visible) {
            updateDialogType(ASSET_TOO_LARGE)
        }
    }


    when (currentConversationScreenDialog) {
        ONGOING_ACTIVE_CALL -> {
            OngoingActiveCallDialog(onJoinAnyways = {
                conversationCallViewModel.navigateToInitiatingCallScreen()
                updateDialogType(NONE)
            }, onDialogDismiss = {
                updateDialogType(NONE)
            })
        }

        NO_CONNECTIVITY -> {
            CoreFailureErrorDialog(coreFailure = NetworkFailure.NoNetworkConnection(null)) {
                updateDialogType(NONE)
            }
        }

        CALLING_FEATURE_UNAVAILABLE -> {
            CallingFeatureUnavailableDialog(onDialogDismiss = {
                updateDialogType(NONE)
            })
        }

        JOIN_CALL_ANYWAY -> {
            JoinAnywayDialog(
                onDismiss = {
                    conversationCallViewModel.dismissJoinCallAnywayDialog()
                    updateDialogType(NONE)
                },
                onConfirm = conversationCallViewModel::joinAnyway
            )
        }

        DELETE_MESSAGE -> {
            DeleteMessageDialog(
                state = messageComposerViewModel.deleteMessageDialogsState,
                actions = messageComposerViewModel.deleteMessageHelper,
                extraOnDismissActions = { updateDialogType(NONE) }
            )
        }

        DOWNLOADED_ASSET -> {
            DownloadedAssetDialog(
                downloadedAssetDialogState = conversationMessagesViewModel.conversationViewState.downloadedAssetDialogState,
                onSaveFileToExternalStorage = conversationMessagesViewModel::downloadAssetExternally,
                onOpenFileWithExternalApp = conversationMessagesViewModel::downloadAndOpenAsset,
                hideOnAssetDownloadedDialog = {
                    conversationMessagesViewModel.hideOnAssetDownloadedDialog()
                    updateDialogType(NONE)
                }
            )
        }

        ASSET_TOO_LARGE -> {
            AssetTooLargeDialog(
                dialogState = messageComposerViewModel.messageComposerViewState.assetTooLargeDialogState,
                hideDialog = {
                    messageComposerViewModel.hideAssetTooLargeError()
                    updateDialogType(NONE)
                }
            )
        }

        NONE -> {}
    }
}
