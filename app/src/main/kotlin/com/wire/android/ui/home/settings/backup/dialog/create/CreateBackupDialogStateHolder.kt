package com.wire.android.ui.home.settings.backup.dialog.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

@Stable
class CreateBackupDialogStateHolder {
    companion object {
        val INITIAL_STEP = BackUpDialogStep.SetPassword
    }

    var currentBackupDialogStep: BackUpDialogStep by mutableStateOf(INITIAL_STEP)

    var isBackupFinished: Boolean by mutableStateOf(false)

    var backupProgress: Float by mutableStateOf(0.0f)

    fun toCreateBackup() {
        currentBackupDialogStep = BackUpDialogStep.CreatingBackup
    }

    fun toBackupFailure() {
        currentBackupDialogStep = BackUpDialogStep.Failure
    }

    fun toFinished() {
        isBackupFinished = true
        backupProgress = 1.0f
    }
}

@Composable
fun rememberBackUpDialogState(): CreateBackupDialogStateHolder {
    return remember("someData") { CreateBackupDialogStateHolder() }
}

sealed interface BackUpDialogStep {
    object SetPassword : BackUpDialogStep
    object CreatingBackup : BackUpDialogStep
    object Failure : BackUpDialogStep
}
