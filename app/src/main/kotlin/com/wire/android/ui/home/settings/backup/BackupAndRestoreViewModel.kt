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
    private val navigationManager: NavigationManager,
    private val wireBackUpManager: WireBackupManager
) : ViewModel() {

    var state by mutableStateOf(BackupAndRestoreState())

    fun setBackupPassword(backupPassword: TextFieldValue) {
        state = state.copy(backupPassword = backupPassword)
    }

    fun createBackup() {
        viewModelScope.launch {
            delay(2000)
            state = state.copy(isBackupPasswordValid = true)
        }
//        wireBackUpManager.createBackUp(backupPassword)
    }

    fun saveBackup() {
//        wireBackUpManager.createBackUp(backupPassword)
    }

    fun cancelBackup() {
//        wireBackUpManager.createBackUp(backupPassword)
    }

    fun chooseBackupFile() {

    }

    fun restoreBackup() {
//        wireBackUpManager.restoreBackUp(file)
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

}

data class BackupAndRestoreState(val isBackupPasswordValid: Boolean = false, val backupPassword: TextFieldValue = TextFieldValue(""))
