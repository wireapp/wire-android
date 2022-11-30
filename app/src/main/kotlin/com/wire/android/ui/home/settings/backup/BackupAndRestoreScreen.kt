package com.wire.android.ui.home.settings.backup

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.settings.backup.RestoreFileValidation.GeneralFailure
import com.wire.android.ui.home.settings.backup.RestoreFileValidation.IncompatibleBackup
import com.wire.android.ui.home.settings.backup.RestoreFileValidation.PasswordRequired
import com.wire.android.ui.home.settings.backup.RestoreFileValidation.Pending
import com.wire.android.ui.home.settings.backup.RestoreFileValidation.WrongBackup
import com.wire.android.ui.home.settings.backup.dialog.common.FailureDialog
import com.wire.android.ui.home.settings.backup.dialog.create.BackUpDialogStep
import com.wire.android.ui.home.settings.backup.dialog.create.CreateBackupDialog
import com.wire.android.ui.home.settings.backup.dialog.create.InformBackupDialog
import com.wire.android.ui.home.settings.backup.dialog.create.SetBackupPasswordDialog
import com.wire.android.ui.home.settings.backup.dialog.create.rememberBackUpDialogState
import com.wire.android.ui.home.settings.backup.dialog.restore.EnterRestorePasswordDialog
import com.wire.android.ui.home.settings.backup.dialog.restore.PickRestoreFileDialog
import com.wire.android.ui.home.settings.backup.dialog.restore.RestoreDialogStep
import com.wire.android.ui.home.settings.backup.dialog.restore.RestoreFailure
import com.wire.android.ui.home.settings.backup.dialog.restore.RestoreProgressDialog
import com.wire.android.ui.home.settings.backup.dialog.restore.rememberRestoreDialogState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun BackupAndRestoreScreen(viewModel: BackupAndRestoreViewModel = hiltViewModel()) {
    BackupAndRestoreContent(
        backUpAndRestoreState = viewModel.state,
        onValidateBackupPassword = viewModel::validateBackupCreationPassword,
        onCreateBackup = viewModel::createBackup,
        onSaveBackup = viewModel::saveBackup,
        onChooseBackupFile = viewModel::chooseBackupFileToRestore,
        onRestoreBackup = viewModel::restorePasswordProtectedBackup,
        onCancelBackupRestore = viewModel::cancelBackupRestore,
        onCancelBackupCreation = viewModel::cancelBackupCreation,
        onOpenConversations = viewModel::navigateToConversations,
        onBackPressed = viewModel::navigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndRestoreContent(
    backUpAndRestoreState: BackupAndRestoreState,
    onValidateBackupPassword: (TextFieldValue) -> Unit,
    onCreateBackup: (String) -> Unit,
    onSaveBackup: () -> Unit,
    onCancelBackupCreation: () -> Unit,
    onCancelBackupRestore: () -> Unit,
    onChooseBackupFile: (Uri) -> Unit,
    onRestoreBackup: (TextFieldValue) -> Unit,
    onOpenConversations: () -> Unit,
    onBackPressed: () -> Unit
) {
    val backupAndRestoreStateHolder = rememberBackUpAndRestoreStateHolder()

    Scaffold(topBar = {
        WireCenterAlignedTopAppBar(
            elevation = 0.dp,
            title = stringResource(id = R.string.backup_and_restore_screen_title),
            onNavigationPressed = onBackPressed
        )
    }) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxHeight()
                .padding(internalPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.settings_backup_info),
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)
                )
            }
            Surface(
                color = MaterialTheme.wireColorScheme.background,
                shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation
            ) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)
                ) {
                    WirePrimaryButton(
                        text = stringResource(id = R.string.settings_backup_create),
                        onClick = backupAndRestoreStateHolder::showBackupDialog,
                        modifier = Modifier.fillMaxWidth()
                    )
                    VerticalSpace.x8()
                    WirePrimaryButton(
                        text = stringResource(id = R.string.settings_backup_restore),
                        onClick = backupAndRestoreStateHolder::showRestoreDialog,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (backupAndRestoreStateHolder.dialogState !is BackupAndRestoreDialog.None) {
        when (backupAndRestoreStateHolder.dialogState) {
            is BackupAndRestoreDialog.CreateBackup -> {
                CreateBackupDialogFlow(
                    backUpAndRestoreState = backUpAndRestoreState,
                    onValidateBackupPassword = onValidateBackupPassword,
                    onCreateBackup = onCreateBackup,
                    onSaveBackup = onSaveBackup,
                    onCancelCreateBackup = {
                        backupAndRestoreStateHolder.dismissDialog()
                        onCancelBackupCreation()
                    }
                )
            }

            is BackupAndRestoreDialog.RestoreBackup -> {
                RestoreBackupDialogFlow(
                    backUpAndRestoreState = backUpAndRestoreState,
                    onChooseBackupFile = onChooseBackupFile,
                    onRestoreBackup = onRestoreBackup,
                    onCancelBackupRestore = {
                        backupAndRestoreStateHolder.dismissDialog()
                        onCancelBackupRestore()
                    },
                    onOpenConversations = onOpenConversations
                )
            }

            BackupAndRestoreDialog.None -> {}
        }
    }
}

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
        when (backupDialogStateHolder.currentBackupDialogStep) {
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
                LaunchedEffect(backUpAndRestoreState.backupCreationProgress) {
                    when (val progress = backUpAndRestoreState.backupCreationProgress) {
                        BackupCreationProgress.Pending -> {}
                        BackupCreationProgress.Failed -> backupDialogStateHolder.toBackupFailure()
                        BackupCreationProgress.Finished -> backupDialogStateHolder.toFinished()
                        is BackupCreationProgress.InProgress -> {
                            backupDialogStateHolder.backupProgress = progress.value
                        }
                    }
                }

                CreateBackupDialog(
                    isBackupCreationCompleted = backupDialogStateHolder.isBackupFinished,
                    createBackupProgress = backupDialogStateHolder.backupProgress,
                    onSaveBackup = onSaveBackup,
                    onDismissDialog = onCancelCreateBackup
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
                        Pending -> {}
                        GeneralFailure -> {
                            restoreDialogStateHolder.toRestoreFailure(RestoreFailure.GeneralFailure)
                        }

                        IncompatibleBackup -> {
                            restoreDialogStateHolder.toRestoreFailure(RestoreFailure.IncompatibleBackup)
                        }

                        WrongBackup -> {
                            restoreDialogStateHolder.toRestoreFailure(RestoreFailure.WrongBackup)
                        }

                        is PasswordRequired -> {
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
                        BackupRestoreProgress.Pending -> {}
                        BackupRestoreProgress.Failed -> {
                            val failureType = when (backUpAndRestoreState.restoreFileValidation) {
                                is PasswordRequired, GeneralFailure, Pending -> RestoreFailure.GeneralFailure
                                IncompatibleBackup -> RestoreFailure.IncompatibleBackup
                                WrongBackup -> RestoreFailure.WrongBackup
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

@Preview
@Composable
fun BackupAndRestoreScreenPreview() {
    BackupAndRestoreContent(
        backUpAndRestoreState = BackupAndRestoreState.INITIAL_STATE,
        onValidateBackupPassword = {},
        onCreateBackup = {},
        onSaveBackup = {},
        onCancelBackupCreation = {},
        onCancelBackupRestore = {},
        onChooseBackupFile = {},
        onRestoreBackup = {},
        onOpenConversations = {},
        onBackPressed = {}
    )
}
