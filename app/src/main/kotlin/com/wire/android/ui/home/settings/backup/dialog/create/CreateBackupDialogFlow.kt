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

package com.wire.android.ui.home.settings.backup.dialog.create

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.R
import com.wire.android.ui.home.settings.backup.BackupAndRestoreState
import com.wire.android.ui.home.settings.backup.BackupCreationProgress
import com.wire.android.ui.home.settings.backup.dialog.common.FailureDialog
import com.wire.android.util.permission.PermissionDenialType

@Composable
fun CreateBackupDialogFlow(
    backUpAndRestoreState: BackupAndRestoreState,
    onValidateBackupPassword: (TextFieldValue) -> Unit,
    onCreateBackup: (String) -> Unit,
    onSaveBackup: (Uri) -> Unit,
    onShareBackup: () -> Unit,
    onCancelCreateBackup: () -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit
) {
    val backupDialogStateHolder = rememberBackUpDialogState()

    with(backupDialogStateHolder) {
        when (currentBackupDialogStep) {
            BackUpDialogStep.SetPassword -> {
                SetBackupPasswordDialog(
                    passwordValidation = backUpAndRestoreState.passwordValidation,
                    onBackupPasswordChanged = onValidateBackupPassword,
                    onCreateBackup = { password ->
                        toCreatingBackup()
                        onCreateBackup(password)
                    },
                    onDismissDialog = onCancelCreateBackup
                )
            }

            is BackUpDialogStep.CreatingBackup,
            is BackUpDialogStep.Finished -> {
                CreateBackupStep(
                    backUpAndRestoreState = backUpAndRestoreState,
                    backupDialogStateHolder = backupDialogStateHolder,
                    onSaveBackup = onSaveBackup,
                    onShareBackup = onShareBackup,
                    onCancelCreateBackup = onCancelCreateBackup,
                    onPermissionPermanentlyDenied = onPermissionPermanentlyDenied
                )
            }

            BackUpDialogStep.Failure -> {
                FailureDialog(
                    title = stringResource(R.string.backup_dialog_create_error_title),
                    message = stringResource(R.string.backup_dialog_create_error_subtitle),
                    onDismiss = onCancelCreateBackup
                )
            }
        }
    }

    BackHandler(backupDialogStateHolder.currentBackupDialogStep !is BackUpDialogStep.CreatingBackup) {
        onCancelCreateBackup()
    }
}

@Composable
private fun CreateBackupStep(
    backUpAndRestoreState: BackupAndRestoreState,
    backupDialogStateHolder: CreateBackupDialogStateHolder,
    onSaveBackup: (Uri) -> Unit,
    onShareBackup: () -> Unit,
    onCancelCreateBackup: () -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit,
) {
    with(backupDialogStateHolder) {
        LaunchedEffect(backUpAndRestoreState.backupCreationProgress) {
            when (val progress = backUpAndRestoreState.backupCreationProgress) {
                BackupCreationProgress.Failed -> toBackupFailure()
                is BackupCreationProgress.Finished -> toFinished(progress.fileName)
                is BackupCreationProgress.InProgress -> toCreatingBackup(progress.value)
            }
        }

        CreateBackupDialog(
            isBackupCreationCompleted = isBackupFinished,
            createBackupProgress = backupProgress,
            onSaveBackup = onSaveBackup,
            onShareBackup = onShareBackup,
            backupFileName = backupFileName,
            onDismissDialog = onCancelCreateBackup,
            onPermissionPermanentlyDenied = onPermissionPermanentlyDenied
        )
    }
}
