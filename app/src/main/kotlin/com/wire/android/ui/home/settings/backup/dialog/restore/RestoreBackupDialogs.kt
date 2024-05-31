/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.home.settings.backup.dialog.restore

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireCheckIcon
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.home.messagecomposer.FileBrowserFlow
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.permission.PermissionDenialType
import kotlin.math.roundToInt

@Composable
fun PickRestoreFileDialog(
    onChooseBackupFile: (Uri) -> Unit,
    onCancelBackupRestore: () -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fileFlow = FileBrowserFlow(onChooseBackupFile, onPermissionPermanentlyDenied)

    WireDialog(
        modifier = modifier,
        title = stringResource(R.string.backup_dialog_restore_backup_title),
        text = stringResource(R.string.backup_dialog_restore_backup_message),
        onDismiss = onCancelBackupRestore,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = { fileFlow.launch() },
            text = stringResource(R.string.backup_dialog_choose_backup_file_option),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
fun EnterRestorePasswordDialog(
    isWrongPassword: Boolean,
    backupPasswordTextState: TextFieldState,
    onRestoreBackupFile: () -> Unit,
    onAcknowledgeWrongPassword: () -> Unit,
    onCancelBackupRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isWrongPassword) {
        WireDialog(
            modifier = modifier,
            title = stringResource(R.string.backup_label_enter_password),
            text = stringResource(R.string.backup_dialog_restore_backup_password_message),
            onDismiss = onCancelBackupRestore,
            properties = wireDialogPropertiesBuilder(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = onCancelBackupRestore,
                text = stringResource(id = R.string.label_cancel),
                state = WireButtonState.Default
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = onRestoreBackupFile,
                text = stringResource(id = R.string.label_continue),
                type = WireDialogButtonType.Primary,
                state = if (backupPasswordTextState.text.isEmpty()) WireButtonState.Disabled else WireButtonState.Default
            )
        ) {
            WirePasswordTextField(
                textState = backupPasswordTextState,
                autoFill = false
            )
        }
    } else {
        WireDialog(
            modifier = modifier,
            title = stringResource(R.string.backup_label_wrong_password),
            text = stringResource(R.string.backup_label_verify_input),
            onDismiss = onCancelBackupRestore,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = onAcknowledgeWrongPassword,
                text = stringResource(id = R.string.label_ok),
                type = WireDialogButtonType.Primary,
            )
        )
    }
}

@Composable
fun RestoreProgressDialog(
    isRestoreCompleted: Boolean,
    restoreProgress: Float,
    onOpenConversation: () -> Unit,
    onCancelBackupRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(targetValue = restoreProgress)
    WireDialog(
        modifier = modifier,
        title = stringResource(R.string.backup_dialog_restoring_backup_title),
        onDismiss = {
            // User is not able to dismiss the dialog
        },
        optionButton1Properties = WireDialogButtonProperties(
            onClick = {
                if (isRestoreCompleted) onOpenConversation() else onCancelBackupRestore()
            },
            text = if (isRestoreCompleted) stringResource(R.string.label_ok) else stringResource(id = R.string.label_cancel),
            type = WireDialogButtonType.Primary,
            state = if (isRestoreCompleted) WireButtonState.Default else WireButtonState.Disabled
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isRestoreCompleted) {
                Row {
                    Text(stringResource(R.string.backup_label_conversation_restored), modifier = Modifier.weight(1f))
                    WireCheckIcon()
                }
            } else {
                Row {
                    Text(stringResource(R.string.backup_label_loading_conversations), modifier = Modifier.weight(1f))
                    Text("${restoreProgress.times(100).roundToInt()} %", style = MaterialTheme.wireTypography.body02)
                }
            }
            VerticalSpace.x16()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpace.x16()
        }
    }
}
