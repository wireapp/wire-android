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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.recyclebin.RestoreConfirmationDialog
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.theme.WireTheme

/** The dialog of a bulk restore from the recycle bin, if any is shown. */
internal sealed interface BulkRestoreDialog {

    /** @param nodes the nodes picked when the restore was requested. */
    data class Confirmation(
        val nodes: List<CellNodeUi>,
        val isInProgress: Boolean = false,
    ) : BulkRestoreDialog

    /**
     * The restore of [totalCount] nodes failed. Without rollback, some of them may still have been restored.
     * @param isNoNetworkConnection the device was offline, as opposed to a server side error.
     */
    data class Failure(
        val totalCount: Int,
        val isNoNetworkConnection: Boolean,
    ) : BulkRestoreDialog
}

@Composable
internal fun BulkRestoreDialogs(
    dialog: BulkRestoreDialog,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        is BulkRestoreDialog.Confirmation -> BulkRestoreConfirmationDialog(dialog, onConfirm, onDismiss)
        is BulkRestoreDialog.Failure -> BulkRestoreFailureDialog(dialog, onDismiss)
    }
}

@Composable
private fun BulkRestoreConfirmationDialog(
    confirmation: BulkRestoreDialog.Confirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val singleNode = confirmation.nodes.singleOrNull()
    if (singleNode != null) {
        // Same wording as restoring it from its menu.
        RestoreConfirmationDialog(
            itemName = singleNode.name ?: "",
            isFolder = singleNode is CellNodeUi.Folder,
            isRestoreInProgress = confirmation.isInProgress,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        return
    }

    val count = confirmation.nodes.size
    WireDialog(
        title = pluralStringResource(R.plurals.cells_bulk_restore_confirmation_title, count, count),
        text = pluralStringResource(R.plurals.cells_bulk_restore_confirmation_message, count, count),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onConfirm,
            text = stringResource(R.string.dialog_restore_button_label),
            type = WireDialogButtonType.Primary,
            loading = confirmation.isInProgress,
            state = if (confirmation.isInProgress) WireButtonState.Disabled else WireButtonState.Default,
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
private fun BulkRestoreFailureDialog(
    failure: BulkRestoreDialog.Failure,
    onDismiss: () -> Unit,
) {
    val isSingleItem = failure.totalCount == 1
    WireDialog(
        title = stringResource(
            if (isSingleItem) R.string.cells_bulk_restore_failure_title_one else R.string.cells_bulk_restore_failure_title_many
        ),
        text = stringResource(
            when {
                isSingleItem && failure.isNoNetworkConnection -> R.string.cells_bulk_restore_failure_message_one_no_network
                isSingleItem -> R.string.cells_bulk_restore_failure_message_one
                failure.isNoNetworkConnection -> R.string.cells_bulk_restore_failure_message_many_no_network
                else -> R.string.cells_bulk_restore_failure_message_many
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
private fun PreviewBulkRestoreConfirmationDialog() {
    WireTheme {
        BulkRestoreDialogs(
            dialog = BulkRestoreDialog.Confirmation(nodes = List(3) { previewFolder("Folder $it") }),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@MultipleThemePreviews
@Composable
private fun PreviewBulkRestoreFailureDialog() {
    WireTheme {
        BulkRestoreDialogs(
            dialog = BulkRestoreDialog.Failure(totalCount = 3, isNoNetworkConnection = false),
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
