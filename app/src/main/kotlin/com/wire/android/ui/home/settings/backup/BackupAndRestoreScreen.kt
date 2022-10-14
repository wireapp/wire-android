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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.settings.backup.dialog.BackupAndRestoreDialog
import com.wire.android.ui.home.settings.backup.dialog.BackupDialog
import com.wire.android.ui.home.settings.backup.dialog.RestoreDialog
import com.wire.android.ui.home.settings.backup.dialog.RestoreFailure
import com.wire.android.ui.home.settings.backup.dialog.rememberBackUpAndRestoreStateHolder
import com.wire.android.ui.home.settings.backup.dialog.rememberBackUpDialogState
import com.wire.android.ui.home.settings.backup.dialog.rememberRestoreDialogState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

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
        onBackPressed = viewModel::navigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
                val backupDialogStateHolder = rememberBackUpDialogState()

                LaunchedEffect(backUpAndRestoreState.isBackupPasswordValid) {
                    backupDialogStateHolder.isBackupPasswordValid = backUpAndRestoreState.isBackupPasswordValid
                }

                LaunchedEffect(backUpAndRestoreState.backupProgress) {
                    backupDialogStateHolder.backupProgress = backUpAndRestoreState.backupProgress
                }

                LaunchedEffect(backUpAndRestoreState.isBackupSuccessFull) {
                    backupDialogStateHolder.isBackupSuccessFull = backUpAndRestoreState.isBackupSuccessFull
                }

                BackupDialog(
                    backupDialogStateHolder = backupDialogStateHolder,
                    onValidateBackupPassword = onValidateBackupPassword,
                    onCreateBackup = onCreateBackup,
                    onSaveBackup = onSaveBackup,
                    onCancelBackup = onCancelBackupCreation,
                    onDismissDialog = {
                        onCancelBackupCreation()
                        backupAndRestoreStateHolder.dismissDialog()
                    }
                )
            }
            is BackupAndRestoreDialog.Restore -> {
                val restoreDialogStateHolder = rememberRestoreDialogState()

                LaunchedEffect(backUpAndRestoreState.restoreFileValidation) {
                    when (backUpAndRestoreState.restoreFileValidation) {
                        RestoreFileValidation.GeneralFailure -> restoreDialogStateHolder.toRestoreFailure(RestoreFailure.GeneralFailure)
                        RestoreFileValidation.IncompatibleBackup -> restoreDialogStateHolder.toRestoreFailure(RestoreFailure.IncompatibleBackup)
                        RestoreFileValidation.WrongBackup -> restoreDialogStateHolder.toRestoreFailure(RestoreFailure.WrongBackup)
                        RestoreFileValidation.RequiresPassword -> restoreDialogStateHolder.toEnterPassword()
                        RestoreFileValidation.WrongPassword -> restoreDialogStateHolder.toRestoreFailure(RestoreFailure.WrongPassword)
                        RestoreFileValidation.SuccessFull -> restoreDialogStateHolder.toRestoreBackup()
                    }
                }

                RestoreDialog(
                    restoreDialogStateHolder = restoreDialogStateHolder,
                    onBackupFileChosen = onChooseBackupFile,
                    onRestoreBackup = onRestoreBackup,
                    onDismissDialog = {
                        onCancelBackupRestore()
                        backupAndRestoreStateHolder.dismissDialog()
                    }
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
