package com.wire.android.ui.home.settings.backup.dialog.restore

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.WireCheckIcon
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.home.messagecomposer.attachment.FileBrowserFlow
import com.wire.android.ui.home.settings.backup.BackupAndRestoreState
import com.wire.android.ui.home.settings.backup.BackupRestoreProgress
import com.wire.android.ui.home.settings.backup.PasswordValidation
import com.wire.android.ui.home.settings.backup.RestoreFileValidation
import com.wire.android.ui.home.settings.backup.dialog.common.FailureDialog
import com.wire.android.ui.theme.wireTypography
import kotlin.math.roundToInt

@Suppress("ComplexMethod")
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

            is RestoreDialogStep.EnterPassword -> {
                var showWrongPassword by remember { mutableStateOf(false) }

                LaunchedEffect(backUpAndRestoreState.restorePasswordValidation) {
                    appLogger.d("Password status changed: -> ${backUpAndRestoreState.restorePasswordValidation}")
                    when (backUpAndRestoreState.restorePasswordValidation) {
                        PasswordValidation.NotValid -> showWrongPassword = true
                        PasswordValidation.NotVerified -> showWrongPassword = false
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

            RestoreDialogStep.RestoreBackup -> {
                LaunchedEffect(backUpAndRestoreState.backupRestoreProgress) {
                    when (val progress = backUpAndRestoreState.backupRestoreProgress) {
                        BackupRestoreProgress.Failed -> {
                            val failureType = when (backUpAndRestoreState.restoreFileValidation) {
                                is RestoreFileValidation.PasswordRequired, RestoreFileValidation.GeneralFailure, RestoreFileValidation.Pending, RestoreFileValidation.ValidNonEncryptedBackup -> RestoreFailure.GeneralFailure
                                RestoreFileValidation.IncompatibleBackup -> RestoreFailure.IncompatibleBackup
                                RestoreFileValidation.WrongBackup -> RestoreFailure.WrongBackup
                            }
                            restoreDialogStateHolder.toRestoreFailure(failureType)
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
fun PickRestoreFileDialog(
    onChooseBackupFile: (Uri) -> Unit,
    onCancelBackupRestore: () -> Unit
) {
    val fileFlow = FileBrowserFlow(onChooseBackupFile)

    WireDialog(
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EnterRestorePasswordDialog(
    isWrongPassword: Boolean,
    onRestoreBackupFile: (TextFieldValue) -> Unit,
    onAcknowledgeWrongPassword: () -> Unit,
    onCancelBackupRestore: () -> Unit
) {
    var restorePassword by remember { mutableStateOf(TextFieldValue((""))) }

    if (!isWrongPassword) {
        WireDialog(
            title = stringResource(R.string.backup_label_enter_password),
            text = stringResource(R.string.backup_dialog_restore_backup_password_message),
            onDismiss = onCancelBackupRestore,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = onCancelBackupRestore,
                text = stringResource(id = R.string.label_cancel),
                state = WireButtonState.Default
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { onRestoreBackupFile(restorePassword) },
                text = stringResource(id = R.string.label_continue),
                type = WireDialogButtonType.Primary,
                state = if (restorePassword.text.isEmpty()) WireButtonState.Disabled else WireButtonState.Default
            )
        ) {
            WirePasswordTextField(
                value = restorePassword,
                onValueChange = { restorePassword = it }
            )
        }
    } else {
        WireDialog(
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
    onCancelBackupRestore: () -> Unit
) {
    WireDialog(
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
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = restoreProgress)
            VerticalSpace.x16()
        }
    }
}

