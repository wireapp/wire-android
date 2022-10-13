package com.wire.android.ui.home.settings.backup.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePasswordTextField

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RestoreDialog(
    restoreDialogStateHolder: RestoreDialogStateHolder,
    onChooseBackupFile: () -> Unit,
    onRestoreBackup: (TextFieldValue) -> Unit,
    onDismissDialog: () -> Unit
) {
    with(restoreDialogStateHolder) {
        when (currentBackupDialogStep) {
            is RestoreDialogStep.ChooseBackupFile -> WireDialog(
                title = "Restore a Backup",
                text = "The backup contents will replace the conversation history on this device. You can only restore history from a backup of the same platform.",
                onDismiss = onDismissDialog,
                optionButton1Properties = WireDialogButtonProperties(
                    onClick = onChooseBackupFile,
                    text = "Choose Backup File",
                    type = WireDialogButtonType.Primary,
                )
            )
            is RestoreDialogStep.EnterPassword ->
                WireDialog(
                    title = "Enter password",
                    text = "This backup is password protected.",
                    onDismiss = onDismissDialog,
                    dismissButtonProperties = WireDialogButtonProperties(
                        onClick = onDismissDialog,
                        text = stringResource(id = R.string.label_cancel),
                        state = WireButtonState.Default
                    ),
                    optionButton1Properties = WireDialogButtonProperties(
                        onClick = { onRestoreBackup(backupPassword) },
                        text = "Continue",
                        type = WireDialogButtonType.Primary,
                        state = if (backupPassword.text.isEmpty()) WireButtonState.Disabled else WireButtonState.Default
                    )
                ) {
                    WirePasswordTextField(
                        value = backupPassword,
                        onValueChange = {
                            backupPassword = it
                        }
                    )
                }
        }
    }
}
