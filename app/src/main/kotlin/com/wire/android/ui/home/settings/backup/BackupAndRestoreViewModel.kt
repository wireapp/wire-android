package com.wire.android.ui.home.settings.backup

import android.app.backup.BackupManager
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
    private val navigationManager: NavigationManager,
    private val wireBackUpManager: WireBackupManager
) : ViewModel() {

    fun createBackup(backupPassword: TextFieldValue) {
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
