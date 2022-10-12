package com.wire.android.ui.home.settings.backup.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

class BackupDialogStateHolder(
    val onDismiss: () -> Unit,
    val onStartBackup: (TextFieldValue) -> Unit,
    val onSaveBackup: () -> Unit
) {
    companion object {
        val INITIAL_STEP = BackUpDialogStep.Inform
    }

    var currentBackupDialogStep: BackUpDialogStep by mutableStateOf(INITIAL_STEP)

    var backupPassword: TextFieldValue by mutableStateOf(TextFieldValue(""))

    var isBackupPasswordValid: Boolean by mutableStateOf(false)

    var backupProgress: Float by mutableStateOf(0.0f)

    fun reset() {
        currentBackupDialogStep = INITIAL_STEP
    }

    fun toCreateBackUp() {
        currentBackupDialogStep = BackUpDialogStep.CreatingBackup
    }

    fun toBackupPassword() {
        currentBackupDialogStep = BackUpDialogStep.SetPassword
    }

}

@Composable
fun rememberBackUpDialogState(): BackupDialogStateHolder {
    val backupDialogStateHolder =

    return remember { BackupDialogStateHolder({}, { TextFieldValue("") }, {}) }
}

sealed interface BackUpDialogStep {
    object Inform : BackUpDialogStep
    object SetPassword : BackUpDialogStep
    object CreatingBackup : BackUpDialogStep
    object Failure : BackUpDialogStep
}
