package com.wire.android.ui.home.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupAndRestoreViewModel
@Inject constructor(private val navigationManager: NavigationManager) : ViewModel() {

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }
    fun backupCreate() { /* TODO */ }
    fun backupRestore() { /* TODO */ }
}
