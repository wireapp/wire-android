package com.wire.android.ui.home.settings.backup

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupAndRestoreViewModel
@Inject constructor(
    private val navigationManager: NavigationManager
) : ViewModel() {

    var state by mutableStateOf(BackupAndRestoreState.INITIAL_STATE)

    // TODO: the requirement of the validation could be changed
    // for now we do not validate the password
    fun validateBackupPassword(backupPassword: TextFieldValue) {
        viewModelScope.launch {
            state = state.copy(
                isBackupPasswordValid = true
            )
        }
    }

    fun saveBackup() {

    }

    fun createBackup() {

    }

    fun chooseBackupFileToRestore(uri: Uri) {
        state = state.copy(restoreFileValidation = RestoreFileValidation.RequiresPassword)
    }

    fun restoreBackup(backupPassword: TextFieldValue) {

    }

    fun cancelBackupCreation() {
        state = state.copy(
            isBackupPasswordValid = false,
            backupProgress = 0.0f
        )
    }

    fun cancelBackupRestore() {
        state = state.copy(
            isRestorePasswordValid = false,
            restoreProgress = 0.0f,
            restoreFileValidation = RestoreFileValidation.None
        )
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

}

data class BackupAndRestoreState(
    val isRestorePasswordValid: Boolean,
    val restoreProgress: Float,
    val isRestoreSuccessFull: Boolean,
    val restoreFileValidation: RestoreFileValidation,
    val isBackupPasswordValid: Boolean,
    val backupProgress: Float,
    val isBackupSuccessFull: Boolean,
) {
    companion object {
        val INITIAL_STATE = BackupAndRestoreState(
            isRestorePasswordValid = false,
            restoreProgress = 0.0f,
            isRestoreSuccessFull = true,
            restoreFileValidation = RestoreFileValidation.None,
            isBackupPasswordValid = true,
            backupProgress = 0.0f,
            isBackupSuccessFull = true
        )
    }
}

sealed class RestoreFileValidation {
    object None : RestoreFileValidation()
    object IncompatibleBackup : RestoreFileValidation()
    object WrongBackup : RestoreFileValidation()
    object RequiresPassword : RestoreFileValidation()
    object GeneralFailure : RestoreFileValidation()
    object SuccessFull : RestoreFileValidation()
    object WrongPassword : RestoreFileValidation()
}
