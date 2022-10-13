package com.wire.android.ui.home.settings.backup

import android.app.backup.BackupManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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

    fun chooseBackupFile() {

    }

    fun restoreBackup() {

    }

    fun cancelBackup() {
        resetState()
    }

    private fun resetState() {
        state = BackupAndRestoreState.INITIAL_STATE
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

}

data class BackupAndRestoreState(
    val isBackupPasswordValid: Boolean = true,
    val backupPassword: TextFieldValue = TextFieldValue(""),
    val backupProgress: Float = 0.0f
) {
    companion object {
        val INITIAL_STATE = BackupAndRestoreState(
            isBackupPasswordValid = true,
            backupPassword = TextFieldValue(""),
            backupProgress = 0.0f
        )
    }
}
