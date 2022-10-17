package com.wire.android.ui.home.settings.backup.dialog.create

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
import com.wire.android.ui.common.WireCheckIcon
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.home.settings.backup.dialog.create.BackUpDialogStep
import com.wire.android.ui.home.settings.backup.dialog.create.CreateBackupDialogStateHolder
import com.wire.android.ui.theme.wireTypography

@Composable
fun InformBackupDialog(
    onAcknowledgeBackup: () -> Unit,
    onDismissDialog: () -> Unit
) {
    WireDialog(
        title = "Set an email and password",
        text = "You need an email and a password in order to back up your conversation history. You can do it from the account page in Settings.",
        onDismiss = onDismissDialog,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onAcknowledgeBackup,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SetBackupPasswordDialog(
    isBackupPasswordValid: Boolean,
    onBackupPasswordChanged: (TextFieldValue) -> Unit,
    onCreateBackup: () -> Unit,
    onDismissDialog: () -> Unit
) {
    var backupPassword by remember { mutableStateOf(TextFieldValue("")) }

    WireDialog(
        title = "Set password",
        text = "The backup will be compressed and encrypted with a password. Make sure to store it in a secure place.",
        onDismiss = onDismissDialog,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDismissDialog,
            text = stringResource(id = R.string.label_cancel),
            state = WireButtonState.Default
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onCreateBackup,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
            state = if (!isBackupPasswordValid) WireButtonState.Disabled else WireButtonState.Default
        )
    ) {
        WirePasswordTextField(
            labelText = "PASSWORD (OPTIONAL)",
            state = if (!isBackupPasswordValid) WireTextFieldState.Error("some error") else WireTextFieldState.Default,
            value = backupPassword,
            onValueChange = {
                backupPassword = it
                onBackupPasswordChanged(it)
            }
        )
    }
}

@Composable
fun CreateBackupDialog(
    isBackupCreationCompleted: Boolean,
    createBackupProgress: Float,
    onSaveBackup: () -> Unit,
    onDismissDialog: () -> Unit
) {
    WireDialog(
        title = "Creating Backup",
        onDismiss = onDismissDialog,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onSaveBackup,
            text = "Save backup",
            type = WireDialogButtonType.Primary,
            state = if (isBackupCreationCompleted) WireButtonState.Default else WireButtonState.Disabled
        ),
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDismissDialog,
            text = stringResource(id = R.string.label_cancel),
            state = WireButtonState.Default
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isBackupCreationCompleted) {
                Row {
                    Text("Conversations successfully saved", modifier = Modifier.weight(1f))
                    WireCheckIcon()
                }
            } else {
                Row {
                    Text("Saving conversations", modifier = Modifier.weight(1f))
                    Text("25 %", style = MaterialTheme.wireTypography.body02)
                }
            }
            VerticalSpace.x16()
            LinearProgressIndicator(progress = 1f)
            VerticalSpace.x16()
        }
    }
}
