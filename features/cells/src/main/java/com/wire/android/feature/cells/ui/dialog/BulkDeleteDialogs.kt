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
package com.wire.android.feature.cells.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.theme.WireTheme

/** The dialog of a bulk move to the recycle bin, if any is shown. */
internal sealed interface BulkDeleteDialog {

    /** @param nodes the nodes picked when the delete was requested. */
    data class Confirmation(
        val nodes: List<CellNodeUi>,
        val isInProgress: Boolean = false,
    ) : BulkDeleteDialog

    /**
     * The delete of [totalCount] nodes failed. Without rollback, some of them may still have been deleted.
     * @param isNoNetworkConnection the device was offline, as opposed to a server side error.
     */
    data class Failure(
        val totalCount: Int,
        val isNoNetworkConnection: Boolean,
    ) : BulkDeleteDialog
}

@Composable
internal fun BulkDeleteDialogs(
    dialog: BulkDeleteDialog,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        is BulkDeleteDialog.Confirmation -> BulkDeleteConfirmationDialog(dialog, onConfirm, onDismiss)
        is BulkDeleteDialog.Failure -> BulkDeleteFailureDialog(dialog, onDismiss)
    }
}

@Composable
private fun BulkDeleteConfirmationDialog(
    confirmation: BulkDeleteDialog.Confirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val singleNode = confirmation.nodes.singleOrNull()
    if (singleNode != null) {
        // Same wording as deleting it from its menu.
        DeleteConfirmationDialog(
            itemName = singleNode.name ?: "",
            isFolder = singleNode is CellNodeUi.Folder,
            isPermanentDelete = false,
            isDeleteInProgress = confirmation.isInProgress,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        return
    }

    val count = confirmation.nodes.size
    val hasFolders = confirmation.nodes.any { it is CellNodeUi.Folder }
    WireDialog(
        title = pluralStringResource(R.plurals.cells_bulk_delete_confirmation_title, count, count),
        text = pluralStringResource(
            if (hasFolders) R.plurals.cells_bulk_delete_confirmation_message_with_folders else R.plurals.cells_bulk_delete_confirmation_message,
            count,
            count,
        ),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onConfirm,
            text = stringResource(R.string.delete_label),
            type = WireDialogButtonType.Primary,
            loading = confirmation.isInProgress,
            state = if (confirmation.isInProgress) WireButtonState.Disabled else WireButtonState.Error,
        ),
        dismissButtonProperties = WireDialogButtonProperties(
            text = stringResource(R.string.cancel),
            state = if (confirmation.isInProgress) WireButtonState.Disabled else WireButtonState.Default,
            onClick = onDismiss,
        ),
        properties = wireDialogPropertiesBuilder(
            dismissOnBackPress = !confirmation.isInProgress,
            dismissOnClickOutside = !confirmation.isInProgress,
        ),
    )
}

@Composable
private fun BulkDeleteFailureDialog(
    failure: BulkDeleteDialog.Failure,
    onDismiss: () -> Unit,
) {
    val isSingleItem = failure.totalCount == 1
    WireDialog(
        title = stringResource(
            if (isSingleItem) R.string.cells_bulk_delete_failure_title_one else R.string.cells_bulk_delete_failure_title_many
        ),
        text = stringResource(
            when {
                isSingleItem && failure.isNoNetworkConnection -> R.string.cells_bulk_delete_failure_message_one_no_network
                isSingleItem -> R.string.cells_bulk_delete_failure_message_one
                failure.isNoNetworkConnection -> R.string.cells_bulk_delete_failure_message_many_no_network
                else -> R.string.cells_bulk_delete_failure_message_many
            }
        ),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(R.string.ok_label),
            type = WireDialogButtonType.Primary,
        ),
    )
}

@MultipleThemePreviews
@Composable
private fun PreviewBulkDeleteConfirmationDialog() {
    WireTheme {
        BulkDeleteDialogs(
            dialog = BulkDeleteDialog.Confirmation(nodes = List(3) { previewFolder("Folder $it") }),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@MultipleThemePreviews
@Composable
private fun PreviewBulkDeleteFailureDialog() {
    WireTheme {
        BulkDeleteDialogs(
            dialog = BulkDeleteDialog.Failure(totalCount = 3, isNoNetworkConnection = false),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

private fun previewFolder(name: String) = CellNodeUi.Folder(
    uuid = name,
    name = name,
    userName = null,
    userHandle = null,
    ownerUserId = null,
    conversationName = null,
    modifiedTime = null,
    size = null,
)
