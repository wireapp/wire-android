package com.wire.android.ui.home.settings.backup.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

class RestoreDialogStateHolder {
    companion object {
        val INITIAL_STEP = RestoreDialogStep.ChooseBackupFile
    }

    var currentBackupDialogStep: RestoreDialogStep by mutableStateOf(INITIAL_STEP)

    var backupPassword: TextFieldValue by mutableStateOf(TextFieldValue(""))

    fun toEnterPassword() {
        currentBackupDialogStep = RestoreDialogStep.EnterPassword
    }

    fun toBackupFailure() {
        currentBackupDialogStep = RestoreDialogStep.Failure
    }

    fun reset() {
        currentBackupDialogStep = INITIAL_STEP
    }

}

@Composable
fun rememberRestoreDialogState(): RestoreDialogStateHolder {
    return remember { RestoreDialogStateHolder() }
}

sealed interface RestoreDialogStep {
    object ChooseBackupFile : RestoreDialogStep
    object EnterPassword : RestoreDialogStep
    object RestoreBackup : RestoreDialogStep
    object Failure : RestoreDialogStep, RestoreFailures
}

sealed interface RestoreFailures {
    object IncompatibleBackup : RestoreFailures
    object WrongBackup : RestoreFailures
    object SomethingWentWrong : RestoreFailures
    object WrongPassword : RestoreFailures
}
