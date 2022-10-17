package com.wire.android.ui.home.settings.backup

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
        //TODO: validate the file
        state = state.copy(restoreFileValidation = RestoreFileValidation.PasswordRequired)
    }

    fun restoreBackup(restorePassword: TextFieldValue) {
        //TODO: restore the back up file
        viewModelScope.launch {
            delay(1000)
            state = state.copy(restorePasswordValidation = RestorePasswordValidation.NotVerified)
            delay(1000)
            state = state.copy(restorePasswordValidation = RestorePasswordValidation.NotValid)
            delay(1000)
            state = state.copy(
                restorePasswordValidation = RestorePasswordValidation.Valid,
                restoreProgress = RestoreProgress.InProgress(0.3f)
            )
        }
    }

    fun cancelBackupCreation() {
        state = state.copy(
            isBackupPasswordValid = false,
        )
    }

    fun cancelBackupRestore() {
        state = state.copy(
            restoreFileValidation = RestoreFileValidation.Pending,
            restoreProgress = RestoreProgress.Pending,
            restorePasswordValidation = RestorePasswordValidation.NotVerified
        )
    }

    fun navigateToConversations() {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
        }
    }

    fun retryRestorePassword() {

    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

}

data class BackupAndRestoreState(
    val restoreProgress: RestoreProgress,
    val restoreFileValidation: RestoreFileValidation,
    val restorePasswordValidation: RestorePasswordValidation,
    val backupProgress: BackupProgress,
    val isBackupPasswordValid: Boolean,
) {
    companion object {
        val INITIAL_STATE = BackupAndRestoreState(
            restoreProgress = RestoreProgress.Pending,
            restoreFileValidation = RestoreFileValidation.Pending,
            restorePasswordValidation = RestorePasswordValidation.NotVerified,
            backupProgress = BackupProgress.Pending,
            isBackupPasswordValid = true,
        )
    }
}

sealed interface RestorePasswordValidation {
    object NotVerified : RestorePasswordValidation
    object NotValid : RestorePasswordValidation
    object Valid : RestorePasswordValidation
}

sealed interface BackupProgress {
    object Pending : BackupProgress
    object Finished : BackupProgress
    data class InProgress(val progress: Float = 0f) : BackupProgress
    object Failed : BackupProgress
}

sealed interface RestoreProgress {
    object Pending : RestoreProgress
    object Finished : RestoreProgress
    data class InProgress(val progress: Float = 0f) : RestoreProgress
    object Failed : RestoreProgress
}

sealed class RestoreFileValidation {
    object Pending : RestoreFileValidation()
    object IncompatibleBackup : RestoreFileValidation()
    object WrongBackup : RestoreFileValidation()
    object PasswordRequired : RestoreFileValidation()
    object GeneralFailure : RestoreFileValidation()
}
