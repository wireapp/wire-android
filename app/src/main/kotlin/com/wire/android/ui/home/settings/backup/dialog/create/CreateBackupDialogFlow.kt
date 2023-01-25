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
 *
 *
 */

package com.wire.android.ui.home.settings.backup.dialog.create

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.R
import com.wire.android.ui.home.settings.backup.BackupAndRestoreState
import com.wire.android.ui.home.settings.backup.BackupCreationProgress
import com.wire.android.ui.home.settings.backup.PasswordValidation
import com.wire.android.ui.home.settings.backup.dialog.common.FailureDialog

@Composable
fun CreateBackupDialogFlow(
    backUpAndRestoreState: BackupAndRestoreState,
    onValidateBackupPassword: (TextFieldValue) -> Unit,
    onCreateBackup: (String) -> Unit,
    onSaveBackup: () -> Unit,
    onCancelCreateBackup: () -> Unit
) {
    val backupDialogStateHolder = rememberBackUpDialogState()

    with(backupDialogStateHolder) {
        when (currentBackupDialogStep) {
            BackUpDialogStep.SetPassword -> {
                SetBackupPasswordDialog(
                    isBackupPasswordValid = backUpAndRestoreState.backupCreationPasswordValidation is PasswordValidation.Valid,
                    onBackupPasswordChanged = onValidateBackupPassword,
                    onCreateBackup = { password ->
                        toCreateBackup()
                        onCreateBackup(password)
                    },
                    onDismissDialog = onCancelCreateBackup
                )
            }

            BackUpDialogStep.CreatingBackup -> {
                CreateBackupStep(
                    backUpAndRestoreState = backUpAndRestoreState,
                    backupDialogStateHolder = backupDialogStateHolder,
                    onSaveBackup = onSaveBackup,
                    onCancelCreateBackup = onCancelCreateBackup
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

    BackHandler(backupDialogStateHolder.currentBackupDialogStep != BackUpDialogStep.CreatingBackup) {
        onCancelCreateBackup()
    }
}

@Composable
private fun CreateBackupStep(
    backUpAndRestoreState: BackupAndRestoreState,
    backupDialogStateHolder: CreateBackupDialogStateHolder,
    onSaveBackup: () -> Unit,
    onCancelCreateBackup: () -> Unit
) {
    with(backupDialogStateHolder) {
        LaunchedEffect(backUpAndRestoreState.backupCreationProgress) {
            when (val progress = backUpAndRestoreState.backupCreationProgress) {
                BackupCreationProgress.Failed -> toBackupFailure()
                BackupCreationProgress.Finished -> toFinished()
                is BackupCreationProgress.InProgress -> {
                    backupProgress = progress.value
                }
            }
        }

        CreateBackupDialog(
            isBackupCreationCompleted = isBackupFinished,
            createBackupProgress = backupProgress,
            onSaveBackup = onSaveBackup,
            onDismissDialog = onCancelCreateBackup
        )
    }
}
