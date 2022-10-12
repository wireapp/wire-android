package com.wire.android.ui.home.settings.backup.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState

@Composable
fun BackupDialog(
    backupDialogStateHolder: BackupDialogStateHolder,
    onValidateBackupPassword: (TextFieldValue) -> Unit,
    onCreateBackup: () -> Unit,
    onSaveBackup: () -> Unit,
    onCancelBackup: () -> Unit,
    onDismissDialog: () -> Unit
) {
    with(backupDialogStateHolder) {
        when (currentBackupDialogStep) {
            BackUpDialogStep.Inform -> {
                WireDialog(
                    title = "Set an email and password",
                    text = "You need an email and a password in order to back up your conversation history. You can do it from the account page in Settings.",
                    onDismiss = onDismissDialog,
                    optionButton1Properties = WireDialogButtonProperties(
                        onClick = ::nextStep,
                        text = stringResource(id = R.string.label_ok),
                        type = WireDialogButtonType.Primary,
                    )
                )
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
                        onClick = {
                            if (isBackupPasswordValid) {
                                toCreateBackUp()
                                onCreateBackup()
                            }
                        },
                        text = stringResource(id = R.string.label_ok),
                        type = WireDialogButtonType.Primary,
                        state = if (!isBackupPasswordValid) WireButtonState.Disabled else WireButtonState.Default
                    )
                ) {
                    WirePasswordTextField(
                        state = if (!isBackupPasswordValid) WireTextFieldState.Error("some error") else WireTextFieldState.Default,
                        value = backupPassword,
                        onValueChange = {
                            backupPassword = it
                            onValidateBackupPassword(it)
                        }
                    )
                }
            }
            BackUpDialogStep.CreatingBackup -> {
                WireDialog(
                    title = "Creating Backup",
                    text = "test",
                    onDismiss = onDismissDialog,
                    optionButton1Properties = WireDialogButtonProperties(
                        onClick = ::nextStep,
                        text = stringResource(id = R.string.label_ok),
                        type = WireDialogButtonType.Primary,
                    )
                ) {

                }
            }
            BackUpDialogStep.Failure -> {
                WireDialog(
                    title = "Something went wrong",
                    text = "test",
                    onDismiss = onDismissDialog,
                    optionButton1Properties = WireDialogButtonProperties(
                        onClick = ::nextStep,
                        text = stringResource(id = R.string.label_ok),
                        type = WireDialogButtonType.Primary,
                    )
                ) {

                }
            }
        }
    }
}
