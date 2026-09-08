/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.conversations

import androidx.compose.runtime.Composable
import com.wire.android.feature.cells.ui.dialog.IncompatibleFileNameDialog
import com.wire.android.ui.common.dialogs.InvalidLinkDialog
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dialogs.SureAboutMessagingInDegradedConversationDialog
import com.wire.android.ui.common.dialogs.VisitLinkDialog
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.home.conversations.attachment.FailedAttachmentDialogState
import com.wire.android.ui.home.conversations.attachment.IncompatibleFileNameDialogState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogState
import com.wire.android.ui.home.conversations.messages.DownloadedAssetDialogVisibilityState
import com.wire.android.ui.legalhold.dialog.subject.LegalHoldSubjectMessageDialog

internal data class ConversationDialogsState(
    val deleteMessage: VisibilityState<DeleteMessageDialogState>,
    val downloadedAsset: DownloadedAssetDialogVisibilityState,
    val assetTooLarge: AssetTooLargeDialogState,
    val visitLink: VisitLinkDialogState,
    val invalidLink: InvalidLinkDialogState,
    val permissionPermanentlyDenied: VisibilityState<PermissionPermanentlyDeniedDialogState>,
    val sureAboutMessaging: SureAboutMessagingDialogState,
    val failedAttachment: FailedAttachmentDialogState,
    val incompatibleFileName: IncompatibleFileNameDialogState,
)

@Suppress("LongParameterList")
internal class ConversationDialogActions(
    val deleteMessage: (messageId: String, deleteForEveryone: Boolean) -> Unit,
    val saveFileToExternalStorage: (String) -> Unit,
    val openFileWithExternalApp: (String) -> Unit,
    val hideDownloadedAsset: () -> Unit,
    val onAssetPermissionPermanentlyDenied: () -> Unit,
    val hideAssetTooLarge: () -> Unit,
    val hideVisitLink: () -> Unit,
    val hideInvalidLink: () -> Unit,
    val hidePermissionPermanentlyDenied: () -> Unit,
    val acceptSureAboutMessaging: () -> Unit,
    val dismissSureAboutMessaging: () -> Unit,
    val retryAttachmentUpload: () -> Unit,
    val removeAttachment: () -> Unit,
    val dismissFailedAttachment: () -> Unit,
    val replaceFileNameAutomatically: () -> Unit,
    val dismissIncompatibleFileName: () -> Unit,
)

@Composable
internal fun ConversationDialogs(
    state: ConversationDialogsState,
    actions: ConversationDialogActions,
) {
    DeleteMessageDialog(
        dialogState = state.deleteMessage,
        deleteMessage = actions.deleteMessage,
    )
    DownloadedAssetDialog(
        downloadedAssetDialogState = state.downloadedAsset,
        onSaveFileToExternalStorage = actions.saveFileToExternalStorage,
        onOpenFileWithExternalApp = actions.openFileWithExternalApp,
        hideOnAssetDownloadedDialog = actions.hideDownloadedAsset,
        onPermissionPermanentlyDenied = actions.onAssetPermissionPermanentlyDenied,
    )
    AssetTooLargeDialog(
        dialogState = state.assetTooLarge,
        hideDialog = actions.hideAssetTooLarge,
    )
    VisitLinkDialog(
        dialogState = state.visitLink,
        hideDialog = actions.hideVisitLink,
    )
    InvalidLinkDialog(
        dialogState = state.invalidLink,
        hideDialog = actions.hideInvalidLink,
    )
    PermissionPermanentlyDeniedDialog(
        dialogState = state.permissionPermanentlyDenied,
        hideDialog = actions.hidePermissionPermanentlyDenied,
    )
    SureAboutMessagingInDegradedConversationDialog(
        dialogState = state.sureAboutMessaging,
        sendAnyway = actions.acceptSureAboutMessaging,
        hideDialog = actions.dismissSureAboutMessaging,
    )
    FailedAttachmentDialog(
        state = state.failedAttachment,
        onRetryUpload = actions.retryAttachmentUpload,
        onRemoveAttachment = actions.removeAttachment,
        onDismiss = actions.dismissFailedAttachment,
    )

    if (state.incompatibleFileName is IncompatibleFileNameDialogState.Visible) {
        IncompatibleFileNameDialog(
            onReplaceAutomatically = actions.replaceFileNameAutomatically,
            onDismiss = actions.dismissIncompatibleFileName,
        )
    }

    (state.sureAboutMessaging as? SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold)?.let {
        LegalHoldSubjectMessageDialog(
            dialogDismissed = actions.dismissSureAboutMessaging,
            sendAnywayClicked = actions.acceptSureAboutMessaging,
        )
    }
}
