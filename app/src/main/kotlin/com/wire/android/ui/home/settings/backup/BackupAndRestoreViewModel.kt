package com.wire.android.ui.home.settings.backup

import android.content.Context
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
import com.wire.kalium.logic.feature.backup.ImportBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BackupAndRestoreViewModel
@Inject constructor(
    private val navigationManager: NavigationManager,
    private val importBackup: ImportBackupUseCase,
    private val context: Context
) : ViewModel() {

    var state by mutableStateOf(BackupAndRestoreState.INITIAL_STATE)

    // TODO: the requirement of the validation could be changed
    // for now we do not validate the password
    fun validateBackupPassword(backupPassword: TextFieldValue) {
        viewModelScope.launch {
            state = state.copy(
                backupPasswordValidation = PasswordValidation.Valid
            )
        }
    }

    @Suppress("EmptyFunctionBlock")
    //TODO: save a back up file
    fun saveBackup() {

    }

    @Suppress("MagicNumber")
    //TODO: create a back up
    fun createBackup() {
        // TEST purpose remove once we are restoring the backup
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
        val tempDbFile = File(context.cacheDir, "tempDb")

        context.contentResolver.openInputStream(uri)!!.copyTo(tempDbFile.outputStream())

        viewModelScope.launch {
            importBackup(tempDbFile.absolutePath)
        }
    }

    private fun validateBackupFile(uri: Uri) {
        state = state.copy(restoreFileValidation = RestoreFileValidation.PasswordRequired)
    }

    fun restoreBackup(restorePassword: TextFieldValue) {
        viewModelScope.launch {
            //TODO: restore the back up file
            checkRestorePassword(restorePassword)
            //TODO: restore the file
            restoreBackupFile(restorePassword)
        }
    }
    @Suppress("MagicNumber")
    private suspend fun restoreBackupFile(restorePassword: TextFieldValue) {
        // Test purpose remove once we are able to restore file
        state = state.copy(restoreProgress = RestoreProgress.InProgress(0.25f))
        delay(250)
        state = state.copy(restoreProgress = RestoreProgress.InProgress(0.50f))
        delay(250)
        state = state.copy(restoreProgress = RestoreProgress.InProgress(0.75f))
        delay(250)
        state = state.copy(restoreProgress = RestoreProgress.InProgress(0.99f))
        delay(250)
        state = state.copy(restoreProgress = RestoreProgress.Finished)
    }

    //TODO: check the password
    private fun checkRestorePassword(restorePassword: TextFieldValue) {
        state = state.copy(restorePasswordValidation = PasswordValidation.Valid)
    }

    fun cancelBackupCreation() {
        state = state.copy(
            backupProgress = BackupProgress.Pending,
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
            backupPasswordValidation = PasswordValidation.Valid
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
    data class InProgress(val value: Float = 0f) : BackupProgress
    object Failed : BackupProgress
}

sealed interface RestoreProgress {
    object Pending : RestoreProgress
    object Finished : RestoreProgress
    data class InProgress(val value: Float = 0f) : RestoreProgress
    object Failed : RestoreProgress
}

sealed class RestoreFileValidation {
    object Pending : RestoreFileValidation()
    object IncompatibleBackup : RestoreFileValidation()
    object WrongBackup : RestoreFileValidation()
    object PasswordRequired : RestoreFileValidation()
    object GeneralFailure : RestoreFileValidation()
}
