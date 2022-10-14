package com.wire.android.ui.home.settings.backup.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

@Stable
class BackupDialogStateHolder {
    companion object {
        val INITIAL_STEP = BackUpDialogStep.Inform
    }

    var currentBackupDialogStep: BackUpDialogStep by mutableStateOf(INITIAL_STEP)

    var backupPassword: TextFieldValue by mutableStateOf(TextFieldValue(""))

    var isBackupPasswordValid: Boolean by mutableStateOf(false)

    var isBackupSuccessFull: Boolean by mutableStateOf(false)

    var backupProgress: Float by mutableStateOf(0.0f)

    fun toCreateBackUp() {
        currentBackupDialogStep = BackUpDialogStep.CreatingBackup
    }

    fun toBackupPassword() {
        currentBackupDialogStep = BackUpDialogStep.SetPassword
    }

    fun toBackupFailure() {
        currentBackupDialogStep = BackUpDialogStep.Failure
    }

}

@Composable
fun rememberBackUpDialogState(): BackupDialogStateHolder {
    return remember { BackupDialogStateHolder() }
}

sealed interface BackUpDialogStep {
    object Inform : BackUpDialogStep
    object SetPassword : BackUpDialogStep
    object CreatingBackup : BackUpDialogStep
    object Failure : BackUpDialogStep
}
