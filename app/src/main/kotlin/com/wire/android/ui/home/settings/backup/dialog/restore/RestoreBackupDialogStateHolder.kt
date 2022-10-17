package com.wire.android.ui.home.settings.backup.dialog.restore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class RestoreDialogStateHolder {
    companion object {
        val INITIAL_STEP = RestoreDialogStep.ChooseBackupFile
    }

    var currentRestoreDialogStep: RestoreDialogStep by mutableStateOf(INITIAL_STEP)

    var restoreProgress: Float by mutableStateOf(0.0f)

    var isRestoreCompleted: Boolean by mutableStateOf(false)

    var isRestorePasswordValid: Boolean by mutableStateOf(false)

    fun toEnterPassword() {
        currentRestoreDialogStep = RestoreDialogStep.EnterPassword
    }

    fun test() {
        isRestorePasswordValid = true
    }

    fun toRestoreFailure(restoreFailure: RestoreFailure) {
        currentRestoreDialogStep = RestoreDialogStep.Failure(restoreFailure)
    }

    fun toRestoreBackup() {
        currentRestoreDialogStep = RestoreDialogStep.RestoreBackup
    }

    fun toFinished() {
        restoreProgress = 1.0f
        isRestoreCompleted = true
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
