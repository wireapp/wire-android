package com.wire.android.ui.home.settings.backup.dialog.restore

import android.net.Uri
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
import com.wire.android.ui.home.messagecomposer.attachment.FileBrowserFlow
import com.wire.android.ui.theme.wireTypography


@Composable
fun PickRestoreFileDialog(onChooseBackupFile: (Uri) -> Unit) {
    val fileFlow = FileBrowserFlow(onChooseBackupFile)

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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EnterRestorePasswordDialog(
    isWrongPassword: Boolean,
    onRestoreBackupFile: (TextFieldValue) -> Unit,
    onAcknowledgeWrongPassword: () -> Unit
) {
    var restorePassword by remember { mutableStateOf(TextFieldValue((""))) }

    if (!isWrongPassword) {
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
                onClick = { onRestoreBackupFile(restorePassword) },
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
    onCancelRestore: () -> Unit,
    onOpenConversation: () -> Unit
) {
    WireDialog(
        title = "Restoring Backup...",
        onDismiss = { },
        optionButton1Properties = WireDialogButtonProperties(
            onClick = { if (isRestoreCompleted) onOpenConversation() else onCancelRestore() },
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
            LinearProgressIndicator(progress = restoreProgress)
            VerticalSpace.x16()
        }
    }
}

