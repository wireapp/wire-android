package com.wire.android.ui.home.settings.backup

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.WireApplication
import com.wire.android.appLogger
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.FileManager
import com.wire.android.util.copyToTempPath
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.backup.CreateBackupResult
import com.wire.kalium.logic.feature.backup.CreateBackupUseCase
import com.wire.kalium.logic.feature.backup.RestoreBackupResult
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.BackupIOFailure
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.DecryptionFailure
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.IncompatibleBackup
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.InvalidPassword
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.InvalidUserId
import com.wire.kalium.logic.feature.backup.RestoreBackupUseCase
import com.wire.kalium.logic.feature.backup.VerifyBackupResult
import com.wire.kalium.logic.feature.backup.VerifyBackupUseCase
import com.wire.kalium.logic.util.fileExtension
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.Path
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class BackupAndRestoreViewModel
@Inject constructor(
    private val navigationManager: NavigationManager,
    private val importBackup: RestoreBackupUseCase,
    private val createBackupFile: CreateBackupUseCase,
    private val verifyBackup: VerifyBackupUseCase,
    private val fileManager: FileManager,
    private val kaliumFileSystem: KaliumFileSystem,
    wireApplication: WireApplication
) : AndroidViewModel(wireApplication) {

    var state by mutableStateOf(BackupAndRestoreState.INITIAL_STATE)
    private var latestCreatedBackup: BackupAndRestoreState.CreatedBackup? = null
    private lateinit var latestImportedBackupTempPath: Path

    @Suppress("MagicNumber")
    fun createBackup(password: String) {
        viewModelScope.launch {
            // TODO: Find a way to update the create progress more faithfully. For now we will just show this small delays to mimic the
            //  progress also for small backups
            state = state.copy(backupCreationProgress = BackupCreationProgress.InProgress(PROGRESS_25))
            delay(SMALL_DELAY)
            state = state.copy(backupCreationProgress = BackupCreationProgress.InProgress(PROGRESS_50))
            delay(SMALL_DELAY)

            when (val result = createBackupFile(password)) {
                is CreateBackupResult.Success -> {
                    state = state.copy(backupCreationProgress = BackupCreationProgress.Finished)
                    latestCreatedBackup = BackupAndRestoreState.CreatedBackup(
                        result.backupFilePath,
                        result.backupFileName,
                        result.backupFileSize,
                        password.isNotEmpty()
                    )
                }
                is CreateBackupResult.Failure -> {
                    state = state.copy(backupCreationProgress = BackupCreationProgress.Failed)
                    appLogger.e("Failed to create backup: ${result.coreFailure}")
                }
            }
        }
    }

    fun saveBackup() {
        viewModelScope.launch {
            latestCreatedBackup?.let { backupData ->
                fileManager.shareWithExternalApp(backupData.path, backupData.assetName.fileExtension()) {}
            }
            state = BackupAndRestoreState.INITIAL_STATE
        }
    }

    fun chooseBackupFileToRestore(uri: Uri) {
        viewModelScope.launch {
            latestImportedBackupTempPath = kaliumFileSystem.tempFilePath(TEMP_IMPORTED_BACKUP_FILE_NAME)
            uri.copyToTempPath(getApplication<Application>().applicationContext, latestImportedBackupTempPath)
            checkIfBackupEncrypted(latestImportedBackupTempPath)
        }
    }

    private fun showPasswordDialog() {
        state = state.copy(restoreFileValidation = RestoreFileValidation.PasswordRequired)
    }

    private suspend fun checkIfBackupEncrypted(importedBackupPath: Path) {
        when (val result = verifyBackup(importedBackupPath)) {
            is VerifyBackupResult.Success -> {
                when (result) {
                    is VerifyBackupResult.Success.Encrypted -> showPasswordDialog()
                    is VerifyBackupResult.Success.NotEncrypted -> importDatabase(importedBackupPath)
                }
            }
            is VerifyBackupResult.Failure -> {
                state = state.copy(restoreFileValidation = RestoreFileValidation.IncompatibleBackup)
                val errorMessage = when (result) {
                    is VerifyBackupResult.Failure.Generic -> result.error.toString()
                    VerifyBackupResult.Failure.InvalidBackupFile -> "No valid files found in the backup"
                }

                appLogger.e("Failed to extract backup files: $errorMessage")
            }
        }
    }

    private suspend fun importDatabase(importedBackupPath: Path) {
        state = state.copy(
            restoreFileValidation = RestoreFileValidation.ValidNonEncryptedBackup,
            backupRestoreProgress = BackupRestoreProgress.InProgress(PROGRESS_75)
        )
        when (importBackup(importedBackupPath, null)) {
            RestoreBackupResult.Success -> {
                state = state.copy(backupRestoreProgress = BackupRestoreProgress.InProgress(PROGRESS_75))
                delay(SMALL_DELAY)
                state = state.copy(backupRestoreProgress = BackupRestoreProgress.Finished)
            }
            is RestoreBackupResult.Failure -> {
                appLogger.e(
                    "Error when restoring the db file. The format or version of the backup is not compatible with this " +
                            "version of the app"
                )
                state = state.copy(
                    restoreFileValidation = RestoreFileValidation.IncompatibleBackup,
                    backupRestoreProgress = BackupRestoreProgress.Failed
                )
            }
        }
        kaliumFileSystem.delete(importedBackupPath)
    }

    @Suppress("MagicNumber")
    fun restorePasswordProtectedBackup(restorePassword: TextFieldValue) = viewModelScope.launch {
        state = state.copy(
            backupRestoreProgress = BackupRestoreProgress.InProgress(0.50f),
            restorePasswordValidation = PasswordValidation.NotVerified
        )
        delay(SMALL_DELAY)
        val fileValidationState = state.restoreFileValidation
        if (fileValidationState is RestoreFileValidation.PasswordRequired) {
            state = state.copy(restorePasswordValidation = PasswordValidation.Entered)
            when (val result = importBackup(latestImportedBackupTempPath, restorePassword.text)) {
                RestoreBackupResult.Success -> {
                    state = state.copy(
                        backupRestoreProgress = BackupRestoreProgress.Finished,
                        restorePasswordValidation = PasswordValidation.Valid
                    )
                }
                is RestoreBackupResult.Failure -> {
                    mapBackupRestoreFailure(result.failure)
                }
            }
        } else {
            state = state.copy(backupRestoreProgress = BackupRestoreProgress.Failed)
        }
        kaliumFileSystem.delete(latestImportedBackupTempPath)
    }

    private fun mapBackupRestoreFailure(failure: RestoreBackupResult.BackupRestoreFailure) = when (failure) {
        InvalidPassword -> state = state.copy(
            backupRestoreProgress = BackupRestoreProgress.Failed,
            restoreFileValidation = RestoreFileValidation.PasswordRequired,
            restorePasswordValidation = PasswordValidation.NotValid,
        )

        InvalidUserId -> state = state.copy(
            backupRestoreProgress = BackupRestoreProgress.Failed,
            restoreFileValidation = RestoreFileValidation.WrongBackup,
            restorePasswordValidation = PasswordValidation.Valid
        )

        is IncompatibleBackup -> state = state.copy(
            backupRestoreProgress = BackupRestoreProgress.Failed,
            restoreFileValidation = RestoreFileValidation.IncompatibleBackup,
            restorePasswordValidation = PasswordValidation.Valid
        )

        is BackupIOFailure, is DecryptionFailure -> state = state.copy(
            backupRestoreProgress = BackupRestoreProgress.Failed,
            restoreFileValidation = RestoreFileValidation.GeneralFailure,
            restorePasswordValidation = PasswordValidation.Valid
        )
    }

    fun validateBackupCreationPassword(backupPassword: TextFieldValue) {
        // TODO: modify in case the password requirements change
    }

    fun cancelBackupCreation() {
        state = state.copy(
            backupCreationProgress = BackupCreationProgress.InProgress(0f), // reset progress, aka initial state
        )
    }

    fun cancelBackupRestore() {
        state = state.copy(
            restoreFileValidation = RestoreFileValidation.Initial,
            backupRestoreProgress = BackupRestoreProgress.InProgress(),
            restorePasswordValidation = PasswordValidation.NotVerified
        )
    }

    fun navigateToConversations() {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

    private companion object {
        const val TEMP_IMPORTED_BACKUP_FILE_NAME = "tempImportedBackup.zip"
        const val SMALL_DELAY = 300L
        const val PROGRESS_25 = 0.25f
        const val PROGRESS_50 = 0.50f
        const val PROGRESS_75 = 0.75f
    }
}

