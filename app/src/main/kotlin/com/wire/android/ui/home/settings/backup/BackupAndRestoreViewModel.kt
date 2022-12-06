package com.wire.android.ui.home.settings.backup

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.wire.kalium.logic.feature.backup.ExtractCompressedBackupFileResult
import com.wire.kalium.logic.feature.backup.ExtractCompressedFileUseCase
import com.wire.kalium.logic.feature.backup.RestoreBackupResult
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.BackupIOFailure
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.DecryptionFailure
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.IncompatibleBackup
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.InvalidPassword
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.InvalidUserId
import com.wire.kalium.logic.feature.backup.RestoreBackupUseCase
import com.wire.kalium.logic.util.fileExtension
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.Path
import javax.inject.Inject

@HiltViewModel
class BackupAndRestoreViewModel
@Inject constructor(
    private val navigationManager: NavigationManager,
    private val importBackup: RestoreBackupUseCase,
    private val createBackupUseCase: CreateBackupUseCase,
    private val extractCompressedFileUseCase: ExtractCompressedFileUseCase,
    private val fileManager: FileManager,
    private val kaliumFileSystem: KaliumFileSystem,
    private val context: Context
) : ViewModel() {

    var state by mutableStateOf(BackupAndRestoreState.INITIAL_STATE)
    private var latestCreatedBackup: BackupAndRestoreState.CreatedBackup? = null
    private lateinit var latestExtractedBackupRootPath: Path

    @Suppress("MagicNumber")
    fun createBackup(password: String) {
        viewModelScope.launch {
            // TODO: Find a way to update the create progress more faithfully. For now we will just show this small delays to mimic the
            //  progress also for small backups
            state = state.copy(backupCreationProgress = BackupCreationProgress.InProgress(0.25f))
            delay(300)
            state = state.copy(backupCreationProgress = BackupCreationProgress.InProgress(0.50f))
            delay(300)

            when (val result = createBackupUseCase.invoke(password)) {
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

    fun saveBackup() = viewModelScope.launch {
        latestCreatedBackup?.let { backupData ->
            fileManager.shareWithExternalApp(backupData.path, backupData.assetName.fileExtension()) {}
        }
        state = BackupAndRestoreState.INITIAL_STATE
    }

    fun chooseBackupFileToRestore(uri: Uri) = viewModelScope.launch {
        val importedBackupPath = kaliumFileSystem.tempFilePath(TEMP_IMPORTED_BACKUP_FILE_NAME)
        uri.copyToTempPath(context, importedBackupPath)

        extractBackupFiles(importedBackupPath)

        // Delete the imported backup file
        kaliumFileSystem.delete(importedBackupPath)
    }

    private fun showPasswordDialog() {
        state = state.copy(restoreFileValidation = RestoreFileValidation.PasswordRequired)
    }

    @Suppress("MagicNumber")
    private suspend fun extractBackupFiles(importedBackupPath: Path) {
        when (val result = extractCompressedFileUseCase.invoke(importedBackupPath)) {
            is ExtractCompressedBackupFileResult.Success -> {
                if (result.isEncrypted) {
                    latestExtractedBackupRootPath = result.extractedFilesRootPath
                    showPasswordDialog()
                } else {
                    importDatabase(result.extractedFilesRootPath)
                }
            }
            is ExtractCompressedBackupFileResult.Failure -> {
                state = state.copy(restoreFileValidation = RestoreFileValidation.IncompatibleBackup)
                appLogger.e("Failed to extract backup files: ${result.error}")
            }
        }
    }

    private suspend fun importDatabase(extractedBackupRootPath: Path) {
        state = state.copy(
            restoreFileValidation = RestoreFileValidation.ValidNonEncryptedBackup,
            backupRestoreProgress = BackupRestoreProgress.InProgress(0.75f)
        )
        when (importBackup(extractedBackupRootPath, null)) {
            RestoreBackupResult.Success -> {
                state = state.copy(backupRestoreProgress = BackupRestoreProgress.InProgress(0.75f))
                delay(300)
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
    }

    @Suppress("MagicNumber")
    fun restorePasswordProtectedBackup(restorePassword: TextFieldValue) = viewModelScope.launch {
        state = state.copy(
            backupRestoreProgress = BackupRestoreProgress.InProgress(0.50f),
            restorePasswordValidation = PasswordValidation.NotVerified
        )
        delay(250)
        val fileValidationState = state.restoreFileValidation
        if (fileValidationState is RestoreFileValidation.PasswordRequired) {
            when (val result = importBackup(latestExtractedBackupRootPath, restorePassword.text)) {
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
    }

    private fun mapBackupRestoreFailure(failure: RestoreBackupResult.BackupRestoreFailure) = when (failure) {
        InvalidPassword -> state = state.copy(
            backupRestoreProgress = BackupRestoreProgress.Failed,
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
            backupCreationProgress = BackupCreationProgress.Pending,
        )
    }

    fun cancelBackupRestore() {
        state = state.copy(
            restoreFileValidation = RestoreFileValidation.Pending,
            backupRestoreProgress = BackupRestoreProgress.Pending,
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
    }
}

data class BackupAndRestoreState(
    val backupRestoreProgress: BackupRestoreProgress,
    val restoreFileValidation: RestoreFileValidation,
    val restorePasswordValidation: PasswordValidation,
    val backupCreationProgress: BackupCreationProgress,
    val backupCreationPasswordValidation: PasswordValidation
) {

    data class CreatedBackup(val path: Path, val assetName: String, val assetSize: Long, val isEncrypted: Boolean)
    companion object {
        val INITIAL_STATE = BackupAndRestoreState(
            backupRestoreProgress = BackupRestoreProgress.Pending,
            restoreFileValidation = RestoreFileValidation.Pending,
            backupCreationProgress = BackupCreationProgress.Pending,
            restorePasswordValidation = PasswordValidation.NotVerified,
            backupCreationPasswordValidation = PasswordValidation.Valid,
        )
    }
}

sealed interface PasswordValidation {
    object NotVerified : PasswordValidation
    object NotValid : PasswordValidation
    object Valid : PasswordValidation
}

sealed interface BackupCreationProgress {
    object Pending : BackupCreationProgress
    object Finished : BackupCreationProgress
    data class InProgress(val value: Float = 0f) : BackupCreationProgress
    object Failed : BackupCreationProgress
}

sealed interface BackupRestoreProgress {
    object Pending : BackupRestoreProgress
    object Finished : BackupRestoreProgress
    data class InProgress(val value: Float = 0f) : BackupRestoreProgress
    object Failed : BackupRestoreProgress
}

sealed class RestoreFileValidation {
    object Pending : RestoreFileValidation()
    object ValidNonEncryptedBackup : RestoreFileValidation()
    object IncompatibleBackup : RestoreFileValidation()
    object WrongBackup : RestoreFileValidation()
    object GeneralFailure : RestoreFileValidation()
    object PasswordRequired : RestoreFileValidation()
}
