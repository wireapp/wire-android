package com.wire.android.ui.home.settings.backup.dialog

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RestoreDialog(
    restoreDialogStateHolder: RestoreDialogStateHolder,
    onBackupFileChosen: (Uri) -> Unit,
    onRestoreBackup: (TextFieldValue) -> Unit,
    onOpenConversations: () -> Unit,
    onDismissDialog: () -> Unit
) {
    with(restoreDialogStateHolder) {
        when (val restoreDialogStep = currentRestoreDialogStep) {
            is RestoreDialogStep.ChooseBackupFile -> {
                val fileFlow = FileBrowserFlow(onBackupFileChosen)

                WireDialog(
                    title = "Restore a Backup",
                    text = "The backup contents will replace the conversation history on this device. You can only restore history from a backup of the same platform.",
                    onDismiss = onDismissDialog,
                    optionButton1Properties = WireDialogButtonProperties(
                        onClick = { fileFlow.launch() },
                        text = "Choose Backup File",
                        type = WireDialogButtonType.Primary,
                    )
                )
            }
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
            RestoreDialogStep.RestoreBackup -> {
                val isCompleted = restoreProgress == 1.0f

                WireDialog(
                    title = "Restoring Backup...",
                    onDismiss = onDismissDialog,
                    optionButton1Properties = WireDialogButtonProperties(
                        onClick = { if (isCompleted) onOpenConversations() else onDismissDialog() },
                        text = if (isCompleted) "Ok" else stringResource(id = R.string.label_cancel),
                        type = WireDialogButtonType.Primary,
                        state = if (isCompleted) WireButtonState.Default else WireButtonState.Disabled
                    ),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (isCompleted) {
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
            is RestoreDialogStep.WrongPassword -> {
                WireDialog(
                    title = "Wrong password",
                    text = "Pleasy verify your input and try again",
                    onDismiss = onDismissDialog,
                    optionButton1Properties = WireDialogButtonProperties(
                        onClick = { toEnterPassword() },
                        text = stringResource(id = R.string.label_ok),
                        type = WireDialogButtonType.Primary,
                    )
                )
            }
            is RestoreDialogStep.Failure -> {
                WireDialog(
                    title = restoreDialogStep.restoreFailure.title,
                    text = restoreDialogStep.restoreFailure.message,
                    onDismiss = onDismissDialog,
                    optionButton1Properties = WireDialogButtonProperties(
                        onClick = onDismissDialog,
                        text = stringResource(id = R.string.label_ok),
                        type = WireDialogButtonType.Primary,
                    )
                )
            }
        }
    }
}
