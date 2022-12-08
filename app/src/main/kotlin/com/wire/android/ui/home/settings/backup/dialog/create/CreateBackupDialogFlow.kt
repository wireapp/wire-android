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
            BackUpDialogStep.Inform -> {
                InformBackupDialog(
                    onAcknowledgeBackup = ::toBackupPassword,
                    onDismissDialog = onCancelCreateBackup
                )
            }

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
