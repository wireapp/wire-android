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

    var state by mutableStateOf(BackupAndRestoreState())

    fun setBackupPassword(backupPassword: TextFieldValue) {
        state = state.copy(backupPassword = backupPassword)
    }

    fun validateBackupPassword(backupPassword: TextFieldValue) {
        viewModelScope.launch {
            state = state.copy(isBackupPasswordValid = true)
        }
    }

    fun saveBackup() {

    }

    fun cancelBackup() {
        resetState()
    }

    fun createBackup() {

    }

    fun chooseBackupFile() {

    }

    fun restoreBackup() {

    }

    private fun resetState() {
        state = state.copy(
            isBackupPasswordValid = true,
            backupPassword = TextFieldValue(""),
            backupProgress = 0.0f
        )
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

}

data class BackupAndRestoreState(
    val isBackupPasswordValid: Boolean = false,
    val backupPassword: TextFieldValue = TextFieldValue(""),
    val backupProgress: Float = 0.0f
) {
}
