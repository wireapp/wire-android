package com.wire.android.ui.home.settings.backup

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
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun BackupAndRestoreScreen(viewModel: BackupAndRestoreViewModel = hiltViewModel()) {
    BackupAndRestoreContent(
        onSaveBackup = viewModel::saveBackup,
        onCancelBackup = viewModel::cancelBackup,
        onChooseBackupFile = viewModel::chooseBackupFile,
        onRestoreBackup = viewModel::restoreBackup,
        onCreateBackup = viewModel::createBackup,
        onBackPressed = viewModel::navigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndRestoreContent(
    onCreateBackup: (TextFieldValue) -> Unit,
    onSaveBackup: () -> Unit,
    onCancelBackup: () -> Unit,
    onChooseBackupFile: () -> Unit,
    onRestoreBackup: () -> Unit,
    onBackPressed: () -> Unit
) {
    val backupAndRestoreState = rememberBackUpAndRestoreState()

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
                        onClick = backupAndRestoreState::showBackupDialog,
                        modifier = Modifier.fillMaxWidth()
                    )
                    VerticalSpace.x8()
                    WirePrimaryButton(
                        text = stringResource(id = R.string.settings_backup_restore),
                        onClick = backupAndRestoreState::showRestoreDialog,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    BackupAndRestoreDialogs(
        backupAndRestoreDialog = backupAndRestoreState.dialogState,
        onStartBackup = onCreateBackup,
        onSaveBackup = {},
        onCancelBackup = {},
        onChooseBackupFile = {},
        onRestoreBackup = {},
        onDismissDialog = backupAndRestoreState::dismissDialog
    )
}

@Composable
fun BackupAndRestoreDialogs(
    backupAndRestoreDialog: BackupAndRestoreDialog,
    onStartBackup: (TextFieldValue) -> Unit,
    onSaveBackup: () -> Unit,
    onCancelBackup: () -> Unit,
    onChooseBackupFile: () -> Unit,
    onRestoreBackup: () -> Unit,
    onDismissDialog: () -> Unit
) {
    if (backupAndRestoreDialog !is BackupAndRestoreDialog.None) {
        when (backupAndRestoreDialog) {
            is BackupAndRestoreDialog.Backup -> BackupDialog(
                backupDialogState = backupAndRestoreDialog.backUpDialogState,
                onCreateBackup = onStartBackup,
                onSaveBackup = onSaveBackup,
                onCancelBackup = onCancelBackup,
                onDismissDialog = onDismissDialog
            )
            is BackupAndRestoreDialog.Restore -> {
                RestoreDialog(
                    restoreDialogState = backupAndRestoreDialog.restoreDialogState,
                    onChooseBackupFile = onChooseBackupFile,
                    onRestoreBackup = onRestoreBackup
                )
            }
        }
    }
}

@Composable
fun BackupDialog(
    backupDialogState: BackupDialogState,
    onCreateBackup: (TextFieldValue) -> Unit,
    onSaveBackup: () -> Unit,
    onCancelBackup: () -> Unit,
    onDismissDialog: () -> Unit
) {
    when (val backupDialogStep = backupDialogState.currentBackupDialogStep) {
        BackUpDialogStep.Inform -> {
            WireDialog(
                title = "Set an email and password",
                text = "test",
                onDismiss = onDismissDialog,
                optionButton1Properties = WireDialogButtonProperties(
                    onClick = backupDialogState::nextStep,
                    text = stringResource(id = R.string.label_ok),
                    type = WireDialogButtonType.Primary,
                ),
            ) {

            }
        }
        is BackUpDialogStep.SetPassword -> {
            WireDialog(
                title = "Set password",
                text = "test",
                onDismiss = onDismissDialog,
                dismissButtonProperties = WireDialogButtonProperties(
                    onClick = onDismissDialog,
                    text = stringResource(id = R.string.label_cancel),
                    state = WireButtonState.Default
                ),
                optionButton1Properties = WireDialogButtonProperties(
                    onClick = { backupDialogState.startBackup(backupDialogStep.password) },
                    text = stringResource(id = R.string.label_ok),
                    type = WireDialogButtonType.Primary,
                ),
            ) {
                WirePasswordTextField(
                    value = backupDialogStep.password,
                    onValueChange = { backupDialogStep.password = it }
                )
            }
        }
        BackUpDialogStep.CreatingBackup -> {
            WireDialog(
                title = "Creating Backup",
                text = "test",
                onDismiss = onDismissDialog,
                optionButton1Properties = WireDialogButtonProperties(
                    onClick = backupDialogState::nextStep,
                    text = stringResource(id = R.string.label_ok),
                    type = WireDialogButtonType.Primary,
                ),
            ) {

            }
        }
        BackUpDialogStep.Failure -> {
            WireDialog(
                title = "Something went wrong",
                text = "test",
                onDismiss = onDismissDialog,
                optionButton1Properties = WireDialogButtonProperties(
                    onClick = backupDialogState::nextStep,
                    text = stringResource(id = R.string.label_ok),
                    type = WireDialogButtonType.Primary,
                ),
            ) {

            }
        }
    }
}

@Composable
fun RestoreDialog(
    restoreDialogState: RestoreDialogState,
    onChooseBackupFile: Any,
    onRestoreBackup: Any
) {
    when (restoreDialogState) {

    }
}

class BackUpAndRestoreState(
    private val onDismiss: () -> Unit,
    private val onStartBackup: (TextFieldValue) -> Unit,
    private val onSaveBackup: () -> Unit
) {

    var dialogState: BackupAndRestoreDialog by mutableStateOf(
        BackupAndRestoreDialog.Backup(
            BackupDialogState(
                onDismiss = onDismiss,
                onStartBackup = onStartBackup,
                onSaveBackup = onSaveBackup
            )
        )
    )

    fun showBackupDialog() {
        dialogState = BackupAndRestoreDialog.Backup(
            BackupDialogState(
                onDismiss = onDismiss,
                onStartBackup = onStartBackup,
                onSaveBackup = onSaveBackup
            )
        )
    }

    fun showRestoreDialog() {
        dialogState = BackupAndRestoreDialog.Restore(
            RestoreDialogState(

            )
        )
    }

    fun dismissDialog() {
        dialogState = BackupAndRestoreDialog.None
    }

}

sealed class BackupAndRestoreDialog {
    object None : BackupAndRestoreDialog()
    data class Backup(val backUpDialogState: BackupDialogState) : BackupAndRestoreDialog()
    data class Restore(val restoreDialogState: RestoreDialogState) : BackupAndRestoreDialog()
}

@Composable
fun rememberBackUpAndRestoreState(): BackUpAndRestoreState {
    return remember {
        BackUpAndRestoreState(
            onDismiss = { },
            onStartBackup = { },
            onSaveBackup = { }
        )
    }
}

class BackupDialogState(
    val onDismiss: () -> Unit,
    val onStartBackup: (TextFieldValue) -> Unit,
    val onSaveBackup: () -> Unit
) {
    companion object {
        private const val INITIAL_STEP_INDEX = 1
    }

    private var currentStepIndex = INITIAL_STEP_INDEX

    private val steps: List<BackUpDialogStep> = listOf(
        BackUpDialogStep.Inform,
        BackUpDialogStep.SetPassword,
        BackUpDialogStep.CreatingBackup,
        BackUpDialogStep.Failure
    )

    var currentBackupDialogStep: BackUpDialogStep by mutableStateOf(steps[INITIAL_STEP_INDEX])

    fun nextStep() {
        if (currentStepIndex != steps.lastIndex) {
            currentStepIndex += 1
            currentBackupDialogStep = steps[currentStepIndex]
        }
    }

    fun startBackup(password: TextFieldValue) {
        onStartBackup(password)
        nextStep()
    }

    fun reset() {
        currentStepIndex = 1
        currentBackupDialogStep = steps[currentStepIndex]
    }

}

sealed interface BackUpDialogStep {
    object Inform : BackUpDialogStep
    object SetPassword : BackUpDialogStep {
        var password by mutableStateOf(TextFieldValue(""))
    }

    object CreatingBackup : BackUpDialogStep
    object Failure : BackUpDialogStep
}

sealed interface RestoreDialogStep {
    object Inform : RestoreDialogStep
    object Failure : RestoreDialogStep, RestoreFailures
    object Restore : RestoreDialogStep
}

sealed interface RestoreFailures {
    object IncompatibleBackup : RestoreFailures
    object WrongBackup : RestoreFailures
    object SomethingWentWrong : RestoreFailures
    object WrongPassword : RestoreFailures
}

class RestoreDialogState() {

}

@Preview
@Composable
fun BackupAndRestoreScreenPreview() {
    BackupAndRestoreContent({}, {}, {}, {}, {}, {})
}
