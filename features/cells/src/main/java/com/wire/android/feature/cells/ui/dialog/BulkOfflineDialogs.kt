/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
@file:Suppress("MatchingDeclarationName")

package com.wire.android.feature.cells.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.FileSizeFormatter

/** The dialog of a bulk "make available offline", if any is shown. */
internal sealed interface BulkOfflineDialog {

    /**
     * Asks before downloading.
     * @param files the selected files that will be downloaded.
     * @param skippedCount the selected items left out: folders, files already saved or downloading, unsupported files.
     */
    data class Confirmation(
        val files: List<CellNodeUi.File>,
        val skippedCount: Int,
    ) : BulkOfflineDialog {
        /** Sum of the known sizes, null when no file has one. */
        val totalSize: Long? = files.mapNotNull { it.size }.takeIf { it.isNotEmpty() }?.sum()
    }

    /** Checked before downloading: the files need [requiredBytes] but the device only has [availableBytes] left. */
    data class NotEnoughStorage(
        val requiredBytes: Long,
        val availableBytes: Long,
    ) : BulkOfflineDialog

    /** The device ran out of storage while downloading: only [savedCount] of [totalCount] files were saved. */
    data class RanOutOfStorage(
        val savedCount: Int,
        val totalCount: Int,
    ) : BulkOfflineDialog
}

@Composable
internal fun BulkOfflineDialogs(
    dialog: BulkOfflineDialog,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        is BulkOfflineDialog.Confirmation -> BulkOfflineConfirmationDialog(dialog, onConfirm, onDismiss)
        is BulkOfflineDialog.NotEnoughStorage -> NotEnoughStorageDialog(dialog, onDismiss)
        is BulkOfflineDialog.RanOutOfStorage -> RanOutOfStorageDialog(dialog, onDismiss)
    }
}

@Composable
private fun BulkOfflineConfirmationDialog(
    confirmation: BulkOfflineDialog.Confirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val count = confirmation.files.size
    val formattedSize = confirmation.totalSize?.let { rememberFormattedSize(it) }

    val downloadMessage = if (formattedSize != null) {
        pluralStringResource(R.plurals.cells_bulk_offline_confirmation_message_with_size, count, formattedSize)
    } else {
        pluralStringResource(R.plurals.cells_bulk_offline_confirmation_message, count)
    }
    val skippedMessage = confirmation.skippedCount.takeIf { it > 0 }?.let {
        pluralStringResource(R.plurals.cells_bulk_offline_confirmation_skipped, it, it)
    }

    WireDialog(
        title = pluralStringResource(R.plurals.cells_bulk_offline_confirmation_title, count, count),
        text = listOfNotNull(downloadMessage, skippedMessage).joinToString(separator = "\n\n"),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onConfirm,
            text = stringResource(R.string.cells_bulk_offline_confirmation_button),
            type = WireDialogButtonType.Primary,
        ),
        dismissButtonProperties = WireDialogButtonProperties(
            text = stringResource(R.string.cancel),
            onClick = onDismiss,
        ),
    )
}

@Composable
private fun NotEnoughStorageDialog(dialog: BulkOfflineDialog.NotEnoughStorage, onDismiss: () -> Unit) {
    WireDialog(
        title = stringResource(R.string.cells_bulk_offline_not_enough_storage_title),
        text = stringResource(
            R.string.cells_bulk_offline_not_enough_storage_message,
            rememberFormattedSize(dialog.requiredBytes),
            rememberFormattedSize(dialog.availableBytes),
        ),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(R.string.ok_label),
            type = WireDialogButtonType.Primary,
        ),
    )
}

@Composable
private fun RanOutOfStorageDialog(dialog: BulkOfflineDialog.RanOutOfStorage, onDismiss: () -> Unit) {
    WireDialog(
        title = stringResource(R.string.cells_bulk_offline_not_enough_storage_title),
        text = if (dialog.savedCount > 0) {
            pluralStringResource(
                R.plurals.cells_bulk_offline_ran_out_of_storage_message,
                dialog.totalCount,
                dialog.savedCount,
                dialog.totalCount,
            )
        } else {
            stringResource(R.string.cells_bulk_offline_ran_out_of_storage_message_none)
        },
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(R.string.ok_label),
            type = WireDialogButtonType.Primary,
        ),
    )
}

@Composable
private fun rememberFormattedSize(bytes: Long): String {
    val context = LocalContext.current
    return remember(bytes) { FileSizeFormatter(context).formatSize(bytes) }
}

@MultipleThemePreviews
@Composable
private fun PreviewBulkOfflineConfirmationDialog() {
    WireTheme {
        BulkOfflineDialogs(
            dialog = BulkOfflineDialog.Confirmation(files = List(3) { previewFile("File $it.pdf") }, skippedCount = 2),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@MultipleThemePreviews
@Composable
private fun PreviewBulkOfflineNotEnoughStorageDialog() {
    WireTheme {
        BulkOfflineDialogs(
            dialog = BulkOfflineDialog.NotEnoughStorage(requiredBytes = 1_200_000_000, availableBytes = 300_000_000),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@MultipleThemePreviews
@Composable
private fun PreviewBulkOfflineRanOutOfStorageDialog() {
    WireTheme {
        BulkOfflineDialogs(
            dialog = BulkOfflineDialog.RanOutOfStorage(savedCount = 4, totalCount = 10),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

private fun previewFile(name: String) = CellNodeUi.File(
    uuid = name,
    name = name,
    mimeType = "application/pdf",
    assetType = AttachmentFileType.PDF,
    size = 12_345_678,
    localPath = null,
    userName = null,
    userHandle = null,
    ownerUserId = null,
    conversationName = null,
    conversationId = null,
    modifiedTime = null,
)
