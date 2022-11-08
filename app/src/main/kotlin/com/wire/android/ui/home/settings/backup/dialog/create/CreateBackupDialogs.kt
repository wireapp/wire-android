package com.wire.android.ui.home.settings.backup.dialog.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.wire.android.ui.theme.wireTypography
import kotlin.math.roundToInt

@Composable
fun InformBackupDialog(
    onAcknowledgeBackup: () -> Unit,
    onDismissDialog: () -> Unit
) {
    WireDialog(
        title = stringResource(R.string.dialog_create_backup_inform_title),
        text = stringResource(R.string.backup_dialog_create_backup_inform_message),
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
        title = stringResource(R.string.backup_dialog_create_backup_set_password_title),
        text = stringResource(R.string.backup_dialog_create_backup_set_password_message),
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
            labelText = stringResource(R.string.label_textfield_optional_password),
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
        title = stringResource(R.string.backup_dialog_create_backup_title),
        onDismiss = onDismissDialog,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onSaveBackup,
            text = stringResource(R.string.backup_dialog_create_backup_save),
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
                    Text(stringResource(R.string.backup_label_conversation_successfully_saved), modifier = Modifier.weight(1f))
                    WireCheckIcon()
                }
            } else {
                Row {
                    Text("Test", modifier = Modifier.weight(1f))
                    Text("${createBackupProgress.times(100).roundToInt()} %", style = MaterialTheme.wireTypography.body02)
                }
            }
            VerticalSpace.x16()
            LinearProgressIndicator(progress = createBackupProgress)
            VerticalSpace.x16()
        }
    }
}
