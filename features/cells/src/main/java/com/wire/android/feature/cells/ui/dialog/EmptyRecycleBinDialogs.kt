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
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.theme.WireTheme

/** The dialog of emptying the recycle bin, if any is shown. */
internal sealed interface EmptyRecycleBinDialog {

    data class Confirmation(val isInProgress: Boolean = false) : EmptyRecycleBinDialog

    /**
     * Emptying the recycle bin failed. Without rollback, some of its items may still have been deleted.
     * @param isNoNetworkConnection the device was offline, as opposed to a server side error.
     */
    data class Failure(val isNoNetworkConnection: Boolean) : EmptyRecycleBinDialog
}

@Composable
internal fun EmptyRecycleBinDialogs(
    dialog: EmptyRecycleBinDialog,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        is EmptyRecycleBinDialog.Confirmation -> EmptyRecycleBinConfirmationDialog(dialog, onConfirm, onDismiss)
        is EmptyRecycleBinDialog.Failure -> EmptyRecycleBinFailureDialog(dialog, onDismiss)
    }
}

@Composable
private fun EmptyRecycleBinConfirmationDialog(
    confirmation: EmptyRecycleBinDialog.Confirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    WireDialog(
        title = stringResource(R.string.cells_empty_recycle_bin_confirmation_title),
        text = stringResource(R.string.cells_empty_recycle_bin_confirmation_message),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onConfirm,
            text = stringResource(R.string.cells_empty_recycle_bin_confirm),
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
private fun EmptyRecycleBinFailureDialog(
    failure: EmptyRecycleBinDialog.Failure,
    onDismiss: () -> Unit,
) {
    WireDialog(
        title = stringResource(R.string.cells_empty_recycle_bin_failure_title),
        text = stringResource(
            if (failure.isNoNetworkConnection) {
                R.string.cells_empty_recycle_bin_failure_message_no_network
            } else {
                R.string.cells_empty_recycle_bin_failure_message
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
private fun PreviewEmptyRecycleBinConfirmationDialog() {
    WireTheme {
        EmptyRecycleBinDialogs(dialog = EmptyRecycleBinDialog.Confirmation(), onConfirm = {}, onDismiss = {})
    }
}

@MultipleThemePreviews
@Composable
private fun PreviewEmptyRecycleBinFailureDialog() {
    WireTheme {
        EmptyRecycleBinDialogs(dialog = EmptyRecycleBinDialog.Failure(isNoNetworkConnection = false), onConfirm = { }, onDismiss = { })
    }
}
