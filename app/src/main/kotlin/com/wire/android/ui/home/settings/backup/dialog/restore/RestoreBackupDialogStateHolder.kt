package com.wire.android.ui.home.settings.backup.dialog.restore

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.R

@Stable
class RestoreDialogStateHolder {
    companion object {
        val INITIAL_STEP = RestoreDialogStep.ChooseBackupFile
    }

    var currentRestoreDialogStep: RestoreDialogStep by mutableStateOf(INITIAL_STEP)

    var restoreProgress: Float by mutableStateOf(0.0f)

    var isRestoreCompleted: Boolean by mutableStateOf(false)

    fun toEnterPassword() {
        currentRestoreDialogStep = RestoreDialogStep.EnterPassword
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

enum class RestoreFailure(@StringRes val title: Int, @StringRes val message: Int) {
    IncompatibleBackup(
        R.string.backup_dialog_restore_incompatible_version_error_title,
        R.string.backup_dialog_restore_incompatible_version_error_message
    ),
    WrongBackup(
        R.string.backup_dialog_restore_wrong_user_error_title,
        R.string.backup_dialog_restore_wrong_user_error_message
    ),
    GeneralFailure(
        R.string.backup_dialog_restore_general_error_title,
        R.string.backup_dialog_restore_general_error_message
    )
}
