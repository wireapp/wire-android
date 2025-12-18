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
package com.wire.android.feature.cells.ui.versioning.restore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.WireCheckIcon
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import kotlin.math.roundToInt

@Composable
fun RestoreNodeVersionConfirmationDialog(
    onGoToFileClicked: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    restoreVersionState: RestoreVersionState = RestoreVersionState.Idle,
    restoreProgress: Float = .40F,
) {
    WireDialog(
        title = when (restoreVersionState) {
            RestoreVersionState.Idle -> stringResource(R.string.dialog_restore_cell_version_confirmation_title)
            RestoreVersionState.Failed -> stringResource(R.string.dialog_restore_cell_version_failed_title)
            RestoreVersionState.Completed -> stringResource(R.string.dialog_restore_cell_version_successfully_completed_title)
            RestoreVersionState.Restoring -> stringResource(R.string.dialog_restore_cell_version_restoring_title)
        },
        text = when (restoreVersionState) {
            RestoreVersionState.Idle -> stringResource(R.string.dialog_restore_cell_version_confirmation_description)
            RestoreVersionState.Failed -> stringResource(R.string.dialog_restore_cell_version_failed_description)
            RestoreVersionState.Completed, RestoreVersionState.Restoring -> null
        },
        onDismiss = onDismiss,
        optionButton1Properties =
            if (restoreVersionState == RestoreVersionState.Idle) {
                WireDialogButtonProperties(
                    onClick = onConfirm,
                    text = stringResource(id = R.string.dialog_restore_cell_version_confirmation_button_label),
                    type = WireDialogButtonType.Primary,
                )
            } else {
                null
            },
        dismissButtonProperties =
            if (restoreVersionState == RestoreVersionState.Idle) {
                WireDialogButtonProperties(
                    text = stringResource(id = R.string.cancel),
                    onClick = onDismiss
                )
            } else {
                null
            },
        buttonsHorizontalAlignment = false,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = restoreVersionState == RestoreVersionState.Completed
        )
    ) {
        when (restoreVersionState) {
            RestoreVersionState.Completed, RestoreVersionState.Restoring -> GoToFileContent(
                restoreVersionState,
                restoreProgress,
                onGoToFileClicked
            )

            RestoreVersionState.Failed -> RestoreFailedContent(
                onRetry = onConfirm,
                onCancel = onDismiss
            )

            else -> null
        }
    }
}

@Composable
private fun GoToFileContent(
    restoreVersionState: RestoreVersionState,
    restoreProgress: Float = .40F,
    onGoToFileClicked: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth()
    ) {
        if (restoreVersionState == RestoreVersionState.Restoring) {
            Row {
                Text(
                    stringResource(id = R.string.dialog_restore_cell_version_loading),
                    modifier = Modifier.weight(1f)
                )
                Text("${restoreProgress.times(100).roundToInt()} %", style = MaterialTheme.wireTypography.body02)
            }
        } else {
            Row {
                Text(
                    text = stringResource(R.string.dialog_restore_cell_version_successfully_completed),
                    modifier = Modifier.weight(1f)
                )
                WireCheckIcon()
            }
        }
        Spacer(Modifier.height(dimensions().spacing16x))
        LinearProgressIndicator(
            progress = { restoreProgress },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(dimensions().spacing24x))
        WirePrimaryButton(
            text = stringResource(id = R.string.dialog_restore_cell_version_go_to_file_button_label),
            onClick = onGoToFileClicked
        )
    }
}

@Composable
private fun RestoreFailedContent(
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
    ) {
        WirePrimaryButton(
            text = stringResource(id = R.string.retry),
            onClick = onRetry
        )
        Spacer(Modifier.height(dimensions().spacing8x))
        WireSecondaryButton(
            text = stringResource(id = R.string.cancel),
            onClick = onCancel
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewRestoreNodeVersionConfirmationDialog() {
    WireTheme {
        RestoreNodeVersionConfirmationDialog(
            restoreProgress = 0.34f,
            onGoToFileClicked = {},
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewRestoreNodeVersionConfirmationDialogWithRestoreCompleted() {
    WireTheme {
        RestoreNodeVersionConfirmationDialog(
            restoreProgress = 0.34f,
            restoreVersionState = RestoreVersionState.Completed,
            onGoToFileClicked = {},
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewRestoreNodeVersionConfirmationDialogWithRestoreFailed() {
    WireTheme {
        RestoreNodeVersionConfirmationDialog(
            restoreVersionState = RestoreVersionState.Failed,
            onGoToFileClicked = {},
            onConfirm = {},
            onDismiss = {}
        )
    }
}
