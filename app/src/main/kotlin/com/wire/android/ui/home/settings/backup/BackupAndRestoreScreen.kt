package com.wire.android.ui.home.settings.backup

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import com.wire.android.ui.common.WireCheckIcon
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.messagecomposer.attachment.FileBrowserFlow
import com.wire.android.ui.home.settings.backup.dialog.BackupAndRestoreDialog
import com.wire.android.ui.home.settings.backup.dialog.BackupDialog
import com.wire.android.ui.home.settings.backup.dialog.RestoreDialog
import com.wire.android.ui.home.settings.backup.dialog.RestoreDialogStep
import com.wire.android.ui.home.settings.backup.dialog.RestoreFailure
import com.wire.android.ui.home.settings.backup.dialog.rememberBackUpAndRestoreStateHolder
import com.wire.android.ui.home.settings.backup.dialog.rememberBackUpDialogState
import com.wire.android.ui.home.settings.backup.dialog.rememberRestoreDialogState
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
        onRetryRestorePassword = viewModel::retryRestorePassword,
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
    onRetryRestorePassword: () -> Unit,
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
                    when (val progress = backUpAndRestoreState.backupProgress) {
                        BackupProgress.Failed -> backupDialogStateHolder.toBackupFailure()
                        BackupProgress.Finished -> backupDialogStateHolder.toFinished()
                        is BackupProgress.InProgress -> {
                            backupDialogStateHolder.backupProgress = progress.progress
                        }
                    }
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

                with(restoreDialogStateHolder) {
                    when (val restoreDialogStep = currentRestoreDialogStep) {
                        is RestoreDialogStep.ChooseBackupFile -> {
                            val fileFlow = FileBrowserFlow(onChooseBackupFile)

                            LaunchedEffect(backUpAndRestoreState.restoreFileValidation) {
                                when (backUpAndRestoreState.restoreFileValidation) {
                                    RestoreFileValidation.GeneralFailure -> restoreDialogStateHolder.toRestoreFailure(RestoreFailure.GeneralFailure)
                                    RestoreFileValidation.IncompatibleBackup -> restoreDialogStateHolder.toRestoreFailure(RestoreFailure.IncompatibleBackup)
                                    RestoreFileValidation.WrongBackup -> restoreDialogStateHolder.toRestoreFailure(RestoreFailure.WrongBackup)
                                    RestoreFileValidation.PasswordRequired -> restoreDialogStateHolder.toEnterPassword()
                                }
                            }

                            WireDialog(
                                title = "Restore a Backup",
                                text = "The backup contents will replace the conversation history on this device. You can only restore history from a backup of the same platform.",
                                onDismiss = {},
                                optionButton1Properties = WireDialogButtonProperties(
                                    onClick = { fileFlow.launch() },
                                    text = "Choose Backup File",
                                    type = WireDialogButtonType.Primary,
                                )
                            )
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

                            if (!showWrongPassword) {
                                WireDialog(
                                    title = "Enter password",
                                    text = "This backup is password protected.",
                                    onDismiss = { },
                                    dismissButtonProperties = WireDialogButtonProperties(
                                        onClick = { },
                                        text = stringResource(id = R.string.label_cancel),
                                        state = WireButtonState.Default
                                    ),
                                    optionButton1Properties = WireDialogButtonProperties(
                                        onClick = { onRestoreBackup(restorePassword) },
                                        text = "Continue",
                                        type = WireDialogButtonType.Primary,
                                        state = if (restorePassword.text.isEmpty()) WireButtonState.Disabled else WireButtonState.Default
                                    )
                                ) {
                                    WirePasswordTextField(
                                        value = restorePassword,
                                        onValueChange = {
                                            restorePassword = it
                                        }
                                    )
                                }
                            } else {
                                WireDialog(
                                    title = "Wrong password",
                                    text = "Please verify your input and try again",
                                    onDismiss = { },
                                    optionButton1Properties = WireDialogButtonProperties(
                                        onClick = { showWrongPassword = false },
                                        text = stringResource(id = R.string.label_ok),
                                        type = WireDialogButtonType.Primary,
                                    )
                                )
                            }
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

                            WireDialog(
                                title = "Restoring Backup...",
                                onDismiss = { },
                                optionButton1Properties = WireDialogButtonProperties(
                                    onClick = { },
                                    text = if (isRestoreCompleted) "Ok" else stringResource(id = R.string.label_cancel),
                                    type = WireDialogButtonType.Primary,
                                    state = if (isRestoreCompleted) WireButtonState.Default else WireButtonState.Disabled
                                ),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (isRestoreCompleted) {
                                        Row {
                                            Text("Conversations have been restored", modifier = Modifier.weight(1f))
                                            WireCheckIcon()
                                        }
                                    } else {
                                        Row {
                                            Text("Loading conversations", modifier = Modifier.weight(1f))
                                            Text("25 %", style = MaterialTheme.wireTypography.body02)
                                        }
                                    }
                                    VerticalSpace.x16()
                                    LinearProgressIndicator(progress = 1f)
                                    VerticalSpace.x16()
                                }
                            }
                        }
                        is RestoreDialogStep.Failure -> {
                            WireDialog(
                                title = restoreDialogStep.restoreFailure.title,
                                text = restoreDialogStep.restoreFailure.message,
                                onDismiss = { },
                                optionButton1Properties = WireDialogButtonProperties(
                                    onClick = { },
                                    text = stringResource(id = R.string.label_ok),
                                    type = WireDialogButtonType.Primary,
                                )
                            )
                        }
                    }
                }
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
