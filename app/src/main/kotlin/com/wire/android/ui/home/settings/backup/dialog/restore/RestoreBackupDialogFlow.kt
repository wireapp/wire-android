package com.wire.android.ui.home.settings.backup.dialog.restore

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.appLogger
import com.wire.android.ui.home.settings.backup.BackupAndRestoreState
import com.wire.android.ui.home.settings.backup.BackupRestoreProgress
import com.wire.android.ui.home.settings.backup.PasswordValidation
import com.wire.android.ui.home.settings.backup.RestoreFileValidation
import com.wire.android.ui.home.settings.backup.dialog.common.FailureDialog

@Composable
fun RestoreBackupDialogFlow(
    backUpAndRestoreState: BackupAndRestoreState,
    onChooseBackupFile: (Uri) -> Unit,
    onRestoreBackup: (TextFieldValue) -> Unit,
    onOpenConversations: () -> Unit,
    onCancelBackupRestore: () -> Unit
) {
    val restoreDialogStateHolder = rememberRestoreDialogState()

    with(restoreDialogStateHolder) {
        when (val restoreDialogStep = currentRestoreDialogStep) {
            is RestoreDialogStep.ChooseBackupFile -> {
                ChooseBackupFileStep(
                    backUpAndRestoreState = backUpAndRestoreState,
                    restoreDialogStateHolder = restoreDialogStateHolder,
                    onChooseBackupFile = onChooseBackupFile,
                    onCancelBackupRestore = onCancelBackupRestore
                )
            }

            is RestoreDialogStep.EnterPassword -> {
                EnterPasswordStep(
                    backUpAndRestoreState = backUpAndRestoreState,
                    restoreDialogStateHolder = restoreDialogStateHolder,
                    onRestoreBackup = onRestoreBackup,
                    onCancelBackupRestore = onCancelBackupRestore
                )
            }

            RestoreDialogStep.RestoreBackup -> {
                RestoreBackupStep(
                    backUpAndRestoreState = backUpAndRestoreState,
                    restoreDialogStateHolder = restoreDialogStateHolder,
                    onOpenConversations = onOpenConversations,
                    onCancelBackupRestore = onCancelBackupRestore
                )
            }

            is RestoreDialogStep.Failure -> {
                FailureDialog(
                    title = stringResource(id = restoreDialogStep.restoreFailure.title),
                    message = stringResource(id = restoreDialogStep.restoreFailure.message),
                    onDismiss = onCancelBackupRestore
                )
            }
        }
    }

    BackHandler(restoreDialogStateHolder.currentRestoreDialogStep != RestoreDialogStep.RestoreBackup) {
        onCancelBackupRestore()
    }
}

@Composable
private fun ChooseBackupFileStep(
    backUpAndRestoreState: BackupAndRestoreState,
    restoreDialogStateHolder: RestoreDialogStateHolder,
    onChooseBackupFile: (Uri) -> Unit,
    onCancelBackupRestore: () -> Unit
) {
    LaunchedEffect(backUpAndRestoreState.restoreFileValidation) {
        when (backUpAndRestoreState.restoreFileValidation) {
            RestoreFileValidation.Pending -> {}
            RestoreFileValidation.ValidNonEncryptedBackup -> {
                restoreDialogStateHolder.toRestoreBackup()
            }
            RestoreFileValidation.GeneralFailure -> {
                restoreDialogStateHolder.toRestoreFailure(RestoreFailure.GeneralFailure)
            }

            RestoreFileValidation.IncompatibleBackup -> {
                restoreDialogStateHolder.toRestoreFailure(RestoreFailure.IncompatibleBackup)
            }

            RestoreFileValidation.WrongBackup -> {
                restoreDialogStateHolder.toRestoreFailure(RestoreFailure.WrongBackup)
            }

            is RestoreFileValidation.PasswordRequired -> {
                restoreDialogStateHolder.toEnterPassword()
            }
        }
    }

    PickRestoreFileDialog(
        onChooseBackupFile = onChooseBackupFile,
        onCancelBackupRestore = onCancelBackupRestore
    )
}

@Composable
fun EnterPasswordStep(
    backUpAndRestoreState: BackupAndRestoreState,
    restoreDialogStateHolder: RestoreDialogStateHolder,
    onRestoreBackup: (TextFieldValue) -> Unit,
    onCancelBackupRestore: () -> Unit
) {
    var showWrongPassword by remember { mutableStateOf(false) }

    LaunchedEffect(backUpAndRestoreState.restorePasswordValidation) {
        appLogger.d("Password status changed: -> ${backUpAndRestoreState.restorePasswordValidation}")
        when (backUpAndRestoreState.restorePasswordValidation) {
            PasswordValidation.NotValid -> showWrongPassword = true
            PasswordValidation.NotVerified -> showWrongPassword = false
            PasswordValidation.Entered -> restoreDialogStateHolder.toRestoreBackup()
            PasswordValidation.Valid -> restoreDialogStateHolder.toRestoreBackup()
        }
    }

    EnterRestorePasswordDialog(
        isWrongPassword = showWrongPassword,
        onRestoreBackupFile = { password ->
            showWrongPassword = false
            onRestoreBackup(password)
        },
        onAcknowledgeWrongPassword = { showWrongPassword = false },
        onCancelBackupRestore = onCancelBackupRestore
    )
}

@Composable
fun RestoreBackupStep(
    backUpAndRestoreState: BackupAndRestoreState,
    restoreDialogStateHolder: RestoreDialogStateHolder,
    onOpenConversations: () -> Unit,
    onCancelBackupRestore: () -> Unit
) {
    with(restoreDialogStateHolder) {
        LaunchedEffect(backUpAndRestoreState.backupRestoreProgress) {
            when (val progress = backUpAndRestoreState.backupRestoreProgress) {
                BackupRestoreProgress.Failed -> {
                    if (backUpAndRestoreState.restorePasswordValidation == PasswordValidation.NotValid) {
                        restoreDialogStateHolder.toEnterPassword()
                    } else {
                        val failureType = when (backUpAndRestoreState.restoreFileValidation) {
                            RestoreFileValidation.IncompatibleBackup -> RestoreFailure.IncompatibleBackup
                            RestoreFileValidation.WrongBackup -> RestoreFailure.WrongBackup
                            else -> RestoreFailure.GeneralFailure
                        }
                        restoreDialogStateHolder.toRestoreFailure(failureType)
                    }
                }
                BackupRestoreProgress.Finished -> restoreDialogStateHolder.toFinished()
                is BackupRestoreProgress.InProgress -> {
                    restoreDialogStateHolder.restoreProgress = progress.value
                }
            }
        }

        RestoreProgressDialog(
            isRestoreCompleted = isRestoreCompleted,
            restoreProgress = restoreProgress,
            onOpenConversation = onOpenConversations,
            onCancelBackupRestore = onCancelBackupRestore
        )
    }
}

