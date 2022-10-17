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
                backupPasswordValidation = if (backupPassword.text.isEmpty()) PasswordValidation.Valid else PasswordValidation.NotValid
            )
        }
    }

    fun saveBackup() {

    }

    fun createBackup() {
        viewModelScope.launch {
            state = state.copy(backupProgress = BackupProgress.InProgress(0.25f))
            delay(250)
            state = state.copy(backupProgress = BackupProgress.InProgress(0.50f))
            delay(250)
            state = state.copy(backupProgress = BackupProgress.InProgress(0.75f))
            delay(250)
            state = state.copy(backupProgress = BackupProgress.InProgress(0.99f))
            delay(250)
            state = state.copy(backupProgress = BackupProgress.Finished)
        }
    }

    fun chooseBackupFileToRestore(uri: Uri) {
        //TODO: validate the file
        state = state.copy(restoreFileValidation = RestoreFileValidation.PasswordRequired)
    }

    fun restoreBackup(restorePassword: TextFieldValue) {
        //TODO: restore the back up file
        viewModelScope.launch {
            delay(1000)
            state = state.copy(restorePasswordValidation = PasswordValidation.NotVerified)
            delay(1000)
            state = state.copy(restorePasswordValidation = PasswordValidation.NotValid)
        }
    }

    fun cancelBackupCreation() {
        state = state.copy(
            backupPasswordValidation = PasswordValidation.NotVerified,
        )
    }

    fun cancelBackupRestore() {
        state = state.copy(
            restoreFileValidation = RestoreFileValidation.Pending,
            restoreProgress = RestoreProgress.Pending,
            restorePasswordValidation = PasswordValidation.NotVerified
        )
    }

    fun navigateToConversations() {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

}

data class BackupAndRestoreState(
    val restoreProgress: RestoreProgress,
    val restoreFileValidation: RestoreFileValidation,
    val restorePasswordValidation: PasswordValidation,
    val backupProgress: BackupProgress,
    val backupPasswordValidation: PasswordValidation
) {
    companion object {
        val INITIAL_STATE = BackupAndRestoreState(
            restoreProgress = RestoreProgress.Pending,
            restoreFileValidation = RestoreFileValidation.Pending,
            backupProgress = BackupProgress.Pending,
            restorePasswordValidation = PasswordValidation.NotVerified,
            backupPasswordValidation = PasswordValidation.NotVerified
        )
    }
}

sealed interface PasswordValidation {
    object NotVerified : PasswordValidation
    object NotValid : PasswordValidation
    object Valid : PasswordValidation
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
