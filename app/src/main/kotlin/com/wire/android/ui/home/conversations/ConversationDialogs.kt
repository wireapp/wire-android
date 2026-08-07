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
import com.wire.android.R
import com.wire.android.feature.cells.ui.dialog.IncompatibleFileNameDialog
import com.wire.android.ui.common.dialogs.InvalidLinkDialog
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dialogs.SureAboutMessagingInDegradedConversationDialog
import com.wire.android.ui.common.dialogs.VisitLinkDialog
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.home.conversations.attachment.IncompatibleFileNameDialogState
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentsViewModel
import com.wire.android.ui.home.conversations.composer.MessageComposerViewModel
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModel
import com.wire.android.ui.legalhold.dialog.subject.LegalHoldSubjectMessageDialog
import com.wire.android.ui.common.R as commonR

@Composable
internal fun ConversationDialogs(
    conversationMessagesViewModel: ConversationMessagesViewModel,
    sendMessageViewModel: SendMessageViewModel,
    messageComposerViewModel: MessageComposerViewModel,
    messageAttachmentsViewModel: MessageAttachmentsViewModel,
    permissionPermanentlyDeniedDialogState: VisibilityState<PermissionPermanentlyDeniedDialogState>,
) {
    DeleteMessageDialog(
        dialogState = conversationMessagesViewModel.deleteMessageDialogState,
        deleteMessage = conversationMessagesViewModel::deleteMessage,
    )
    DownloadedAssetDialog(
        downloadedAssetDialogState = conversationMessagesViewModel.conversationViewState.downloadedAssetDialogState,
        onSaveFileToExternalStorage = conversationMessagesViewModel::downloadAssetExternally,
        onOpenFileWithExternalApp = conversationMessagesViewModel::downloadAndOpenAsset,
        hideOnAssetDownloadedDialog = conversationMessagesViewModel::hideOnAssetDownloadedDialog,
        onPermissionPermanentlyDenied = {
            permissionPermanentlyDeniedDialogState.show(
                PermissionPermanentlyDeniedDialogState.Visible(
                    title = commonR.string.app_permission_dialog_title,
                    description = R.string.save_permission_dialog_description,
                )
            )
        },
    )
    AssetTooLargeDialog(
        dialogState = sendMessageViewModel.assetTooLargeDialogState,
        hideDialog = sendMessageViewModel::hideAssetTooLargeError,
    )
    VisitLinkDialog(
        dialogState = messageComposerViewModel.visitLinkDialogState,
        hideDialog = messageComposerViewModel::hideVisitLinkDialog,
    )
    InvalidLinkDialog(
        dialogState = messageComposerViewModel.invalidLinkDialogState,
        hideDialog = messageComposerViewModel::hideInvalidLinkError,
    )
    PermissionPermanentlyDeniedDialog(
        dialogState = permissionPermanentlyDeniedDialogState,
        hideDialog = permissionPermanentlyDeniedDialogState::dismiss,
    )
    SureAboutMessagingInDegradedConversationDialog(
        dialogState = sendMessageViewModel.sureAboutMessagingDialogState,
        sendAnyway = sendMessageViewModel::acceptSureAboutSendingMessage,
        hideDialog = sendMessageViewModel::dismissSureAboutSendingMessage,
    )
    FailedAttachmentDialog(
        state = messageAttachmentsViewModel.failedAttachmentDialogState,
        onRetryUpload = messageAttachmentsViewModel::retryUpload,
        onRemoveAttachment = messageAttachmentsViewModel::remove,
        onDismiss = messageAttachmentsViewModel::onFailedAttachmentDialogDismissed,
    )

    if (messageAttachmentsViewModel.incompatibleFileNameDialogState is IncompatibleFileNameDialogState.Visible) {
        IncompatibleFileNameDialog(
            onReplaceAutomatically = messageAttachmentsViewModel::onReplaceFileNameAutomatically,
            onDismiss = messageAttachmentsViewModel::onDismissIncompatibleFileNameDialog,
        )
    }

    (sendMessageViewModel.sureAboutMessagingDialogState as?
        SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold)?.let {
        LegalHoldSubjectMessageDialog(
            dialogDismissed = sendMessageViewModel::dismissSureAboutSendingMessage,
            sendAnywayClicked = sendMessageViewModel::acceptSureAboutSendingMessage,
        )
    }
}
