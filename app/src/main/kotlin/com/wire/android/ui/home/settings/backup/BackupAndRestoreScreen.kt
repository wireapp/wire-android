package com.wire.android.ui.home.settings.backup

import android.net.Uri
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.settings.backup.dialog.common.FailureDialog
import com.wire.android.ui.home.settings.backup.dialog.create.BackUpDialogStep
import com.wire.android.ui.home.settings.backup.dialog.create.BackupDialog
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
import kotlinx.coroutines.InternalCoroutinesApi

@Composable
fun BackupAndRestoreScreen(viewModel: BackupAndRestoreViewModel = hiltViewModel()) {
    BackupAndRestoreContent(
        backUpAndRestoreState = viewModel.state,
        onValidateBackupPassword = viewModel::validateBackupPassword,
        onCreateBackup = viewModel::createBackup,
        onSaveBackup = viewModel::saveBackup,
        onChooseBackupFile = viewModel::chooseBackupFileToRestore,
        onRestoreBackup = viewModel::restoreBackup,
        onCancelBackupRestore = viewModel::cancelBackupRestore,
        onCancelBackupCreation = viewModel::cancelBackupCreation,
        onOpenConversations = viewModel::navigateToConversations,
        onBackPressed = viewModel::navigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class, InternalCoroutinesApi::class, ExperimentalComposeUiApi::class)
@Composable
fun BackupAndRestoreContent(
    backUpAndRestoreState: BackupAndRestoreState,
    onValidateBackupPassword: (TextFieldValue) -> Unit,
    onCreateBackup: () -> Unit,
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
            is BackupAndRestoreDialog.Backup -> {
                CreateBackupDialogFlow(
                    backUpAndRestoreState = backUpAndRestoreState,
                    onValidateBackupPassword = onValidateBackupPassword
                )
            }
            is BackupAndRestoreDialog.Restore -> {
                RestoreDialogFlow(
                    backUpAndRestoreState = backUpAndRestoreState,
                    onChooseBackupFile = onChooseBackupFile,
                    onRestoreBackup = onRestoreBackup,
                    onCancelBackupRestore = onCancelBackupRestore,
                    onOpenConversations = onOpenConversations
                )
            }
        }
    }
}

@Composable
fun CreateBackupDialogFlow(
    backUpAndRestoreState: BackupAndRestoreState,
    onValidateBackupPassword: (TextFieldValue) -> Unit,
) {
    val backupDialogStateHolder = rememberBackUpDialogState()

    with(backupDialogStateHolder) {
        when (backupDialogStateHolder.currentBackupDialogStep) {
            BackUpDialogStep.Inform -> {
                InformBackupDialog(
                    onAcknowledgeBackup = ::toBackupPassword,
                    onDismissDialog = { }
                )
            }
            BackUpDialogStep.SetPassword -> {
                SetBackupPasswordDialog(
                    isBackupPasswordValid = backUpAndRestoreState.isBackupPasswordValid,
                    onBackupPasswordChanged = onValidateBackupPassword,
                    onCreateBackup = ::toCreateBackUp,
                    onDismissDialog = { }
                )
            }
            BackUpDialogStep.CreatingBackup -> {
                CreateBackupDialog(
                    isBackupCreationCompleted = false,
                    createBackupProgress = 1f,
                    onDismissDialog = { }
                )
            }
            BackUpDialogStep.Failure -> {
                FailureDialog(
                    title = "test",
                    message = "restoreDialogStep.restoreFailure.messag"
                )
            }
        }
    }
}

@Composable
fun RestoreDialogFlow(
    backUpAndRestoreState: BackupAndRestoreState,
    onChooseBackupFile: (Uri) -> Unit,
    onRestoreBackup: (TextFieldValue) -> Unit,
    onCancelBackupRestore: () -> Unit,
    onOpenConversations: () -> Unit
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

                RestoreProgressDialog(
                    isRestoreCompleted = isRestoreCompleted,
                    restoreProgress = restoreProgress,
                    onOpenConversation = onOpenConversations,
                    onCancelRestore = onCancelBackupRestore
                )
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
//
//@Preview
//@Composable
//fun BackupAndRestoreScreenPreview() {
//    BackupAndRestoreContent({}, {}, {}, {}, {}, {})
//}
