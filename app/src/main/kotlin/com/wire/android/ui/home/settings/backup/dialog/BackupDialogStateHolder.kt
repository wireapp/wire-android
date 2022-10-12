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
        private const val INITIAL_STEP_INDEX = 0
    }

    private var currentStepIndex = INITIAL_STEP_INDEX

    private val steps: List<BackUpDialogStep> = listOf(
        BackUpDialogStep.Inform,
        BackUpDialogStep.SetPassword,
        BackUpDialogStep.CreatingBackup,
        BackUpDialogStep.Failure
    )

    var currentBackupDialogStep: BackUpDialogStep by mutableStateOf(steps[INITIAL_STEP_INDEX])

    var backupPassword: TextFieldValue by mutableStateOf(TextFieldValue(""))

    var isBackupPasswordValid: Boolean by mutableStateOf(true)

    var backupProgress: Float by mutableStateOf(0.0f)

    fun nextStep() {
        if (currentStepIndex != steps.lastIndex) {
            currentStepIndex += 1
            currentBackupDialogStep = steps[currentStepIndex]
        }
    }

    fun reset() {
        currentStepIndex = INITIAL_STEP_INDEX
        currentBackupDialogStep = steps[INITIAL_STEP_INDEX]
    }

    fun toCreateBackUp() {
        currentBackupDialogStep = steps[2]
    }

    private fun clearPasswordData() {
        backupPassword = TextFieldValue("")
        isBackupPasswordValid = false
    }

}

@Composable
fun rememberBackUpDialogState(): BackupDialogStateHolder {
    val backupDialogStateHolder = remember { BackupDialogStateHolder({}, { TextFieldValue("") }, {}) }

    return backupDialogStateHolder
}

sealed interface BackUpDialogStep {
    object Inform : BackUpDialogStep
    object SetPassword : BackUpDialogStep
    object CreatingBackup : BackUpDialogStep
    object Failure : BackUpDialogStep
}
