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

    var currentRestoreDialogStep: RestoreDialogStep by mutableStateOf(INITIAL_STEP)

    var backupPassword: TextFieldValue by mutableStateOf(TextFieldValue(""))

    fun toEnterPassword() {
        currentRestoreDialogStep = RestoreDialogStep.EnterPassword
    }

    fun toRestoreFailure(restoreFailure: RestoreFailure) {
        currentRestoreDialogStep = RestoreDialogStep.Failure(restoreFailure)
    }

    fun toRestoreBackup() {
        currentRestoreDialogStep = RestoreDialogStep.RestoreBackup
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
    data class Failure(val restoreFailure: RestoreFailure) : RestoreDialogStep
}

enum class RestoreFailure(val title: String, val message: String) {
    IncompatibleBackup(
        "Incompatible Backup",
        "This backup was created by a newer or outdated version of Wire and cannot be restored here."
    ),
    WrongBackup("Wrong Backup", "You cannot restore history from a different account."),
    GeneralFailure("Something Went Wrong", "Your history could not be restored. Please try again or contact the Wire customer support.")
}
