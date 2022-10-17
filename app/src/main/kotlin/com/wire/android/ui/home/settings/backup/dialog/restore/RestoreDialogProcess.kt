package com.wire.android.ui.home.settings.backup.dialog.restore

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.settings.backup.BackupAndRestoreState
import com.wire.android.ui.home.settings.backup.RestoreFileValidation
import com.wire.android.ui.home.settings.backup.RestorePasswordValidation
import com.wire.android.ui.home.settings.backup.RestoreProgress

@Composable
fun RestoreDialogProcess(
    backUpAndRestoreState: BackupAndRestoreState,
    onChooseBackupFile: (Uri) -> Unit,
    onRestoreBackup: (TextFieldValue) -> Unit
) {
    val restoreDialogStateHolder = rememberRestoreDialogState()

    with(restoreDialogStateHolder) {
        when (val restoreDialogStep = currentRestoreDialogStep) {
            is RestoreDialogStep.ChooseBackupFile -> {
                LaunchedEffect(backUpAndRestoreState.restoreFileValidation) {
                    when (backUpAndRestoreState.restoreFileValidation) {
                        RestoreFileValidation.GeneralFailure -> restoreDialogStateHolder.toRestoreFailure(RestoreFailure.GeneralFailure)
                        RestoreFileValidation.IncompatibleBackup -> restoreDialogStateHolder.toRestoreFailure(RestoreFailure.IncompatibleBackup)
                        RestoreFileValidation.WrongBackup -> restoreDialogStateHolder.toRestoreFailure(RestoreFailure.WrongBackup)
                        RestoreFileValidation.PasswordRequired -> restoreDialogStateHolder.toEnterPassword()
                    }
                }

                PickRestoreFileDialog(onChooseBackupFile = onChooseBackupFile)
            }
            is RestoreDialogStep.EnterPassword -> {
                var showWrongPassword by remember { mutableStateOf(false) }

                LaunchedEffect(backUpAndRestoreState.restorePasswordValidation) {
                    when (backUpAndRestoreState.restorePasswordValidation) {
                        RestorePasswordValidation.NotValid -> showWrongPassword = true
                        RestorePasswordValidation.NotVerified -> showWrongPassword = false
                        RestorePasswordValidation.Valid -> restoreDialogStateHolder.toRestoreBackup()
                    }
                }

                EnterRestorePasswordDialog(
                    isWrongPassword = showWrongPassword,
                    onRestoreBackupFile = onRestoreBackup,
                    onAcknowledgeWrongPassword = { showWrongPassword = false }
                )
            }
            RestoreDialogStep.RestoreBackup -> {
                LaunchedEffect(backUpAndRestoreState.restoreProgress) {
                    when (val progress = backUpAndRestoreState.restoreProgress) {
                        RestoreProgress.Failed -> restoreDialogStateHolder.toRestoreFailure(RestoreFailure.GeneralFailure)
                        RestoreProgress.Finished -> restoreDialogStateHolder.toFinished()
                        is RestoreProgress.InProgress -> {
                            restoreDialogStateHolder.restoreProgress = progress.progress
                        }
                    }
                }

                RestoreProgressDialog(isRestoreCompleted, restoreProgress)
            }
            is RestoreDialogStep.Failure -> {
                FailureDialog(
                    title = restoreDialogStep.restoreFailure.title,
                    message = restoreDialogStep.restoreFailure.message
                )
            }
        }
    }
}
