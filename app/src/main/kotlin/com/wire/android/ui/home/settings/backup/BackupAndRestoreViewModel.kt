package com.wire.android.ui.home.settings.backup

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
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
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.InvalidPassword
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.InvalidUserId
import com.wire.kalium.logic.feature.backup.RestoreBackupUseCase
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.util.extractCompressedFile
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
    private val fileManager: FileManager,
    private val kaliumFileSystem: KaliumFileSystem,
    private val context: Context
) : ViewModel() {

    var state by mutableStateOf(BackupAndRestoreState.INITIAL_STATE)

    @Suppress("MagicNumber")
    fun createBackup(password: String) {
        viewModelScope.launch {
            state = state.copy(backupCreationProgress = BackupCreationProgress.InProgress(0.25f))
            delay(300)
            state = state.copy(backupCreationProgress = BackupCreationProgress.InProgress(0.50f))
            delay(300)
            val result = createBackupUseCase.invoke(password)

            state = if (result is CreateBackupResult.Success) {
                state.copy(
                    createdBackup = BackupAndRestoreState.CreatedBackup(
                        result.backupFilePath,
                        result.backupFileName,
                        result.backupFileSize,
                        password.isNotEmpty()
                    ),
                    backupCreationProgress = BackupCreationProgress.Finished
                )
            } else state.copy(backupCreationProgress = BackupCreationProgress.Failed)
        }
    }

    fun saveBackup() = viewModelScope.launch {
        state.createdBackup?.let { backupData ->
            fileManager.saveToExternalStorage(backupData.assetName, backupData.path, backupData.assetSize) {
                Toast.makeText(context, context.getString(R.string.backup_label_conversation_successfully_saved), Toast.LENGTH_SHORT).show()
            }
        }
        state = BackupAndRestoreState.INITIAL_STATE
    }

    fun chooseBackupFileToRestore(uri: Uri) = viewModelScope.launch {
        val importedBackupPath = kaliumFileSystem.tempFilePath("tempImportedBackup.zip")
        uri.copyToTempPath(context, importedBackupPath)
        val tempCompressedBackupFileSource = kaliumFileSystem.source(importedBackupPath)
        val extractedBackupFilesRootPath = kaliumFileSystem.tempFilePath("extractedBackup")

        // Delete any previously existing files in the extractedBackupRootFilesPath
        if (kaliumFileSystem.exists(extractedBackupFilesRootPath)) {
            kaliumFileSystem.deleteContents(extractedBackupFilesRootPath)
        }
        kaliumFileSystem.createDirectory(extractedBackupFilesRootPath)

        extractCompressedFile(tempCompressedBackupFileSource, extractedBackupFilesRootPath, kaliumFileSystem).fold({
            onBackupRestoreError("Error extracting backup file")
        }, {
            kaliumFileSystem.delete(importedBackupPath)
            val encryptedFilePath = kaliumFileSystem.listDirectories(extractedBackupFilesRootPath).firstOrNull { it.name.contains(".cc20") }
            val isPasswordProtected = encryptedFilePath != null

            if (isPasswordProtected) {
                // If it's password protected, we need to ask the user for the password
                state = state.copy(restoreFileValidation = RestoreFileValidation.PasswordRequired(extractedBackupFilesRootPath))
            } else {
                // If it's not password protected, we can restore the backup
                val result = importBackup(extractedBackupFilesRootPath, null)
                if (result is RestoreBackupResult.Success) {
                    Toast.makeText(context, context.getString(R.string.backup_label_conversation_successfully_saved), Toast.LENGTH_LONG)
                        .show()
                } else {
                    onBackupRestoreError("Error when restoring the db file", RestoreFileValidation.IncompatibleBackup)
                }
            }
        })
    }

    private fun onBackupRestoreError(errorMessage: String, errorType: RestoreFileValidation = RestoreFileValidation.GeneralFailure) {
        appLogger.e(errorMessage)
        state = state.copy(restoreFileValidation = errorType)
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
            when (val result = importBackup(fileValidationState.extractedBackupFilesRootPath, restorePassword.text)) {
                RestoreBackupResult.Success -> {
                    state = state.copy(
                        backupRestoreProgress = BackupRestoreProgress.Finished,
                        backupPasswordValidation = PasswordValidation.Valid
                    )
                    Toast.makeText(context, context.getString(R.string.backup_label_conversation_successfully_saved), Toast.LENGTH_LONG)
                        .show()
                }
                is RestoreBackupResult.Failure -> {
                    when (result.failure) {
                        InvalidPassword -> state = state.copy(
                            backupRestoreProgress = BackupRestoreProgress.Failed,
                            restorePasswordValidation = PasswordValidation.NotValid,
                        )

                        InvalidUserId -> state = state.copy(
                            backupRestoreProgress = BackupRestoreProgress.Failed,
                            restoreFileValidation = RestoreFileValidation.WrongBackup
                        )

                        is BackupIOFailure, is DecryptionFailure -> state = state.copy(
                            backupRestoreProgress = BackupRestoreProgress.Failed,
                            restoreFileValidation = RestoreFileValidation.GeneralFailure
                        )
                    }
                }
            }
        } else {
            state = state.copy(backupRestoreProgress = BackupRestoreProgress.Failed)
        }
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
}

data class BackupAndRestoreState(
    val backupRestoreProgress: BackupRestoreProgress,
    val restoreFileValidation: RestoreFileValidation,
    val restorePasswordValidation: PasswordValidation,
    val backupCreationProgress: BackupCreationProgress,
    val backupPasswordValidation: PasswordValidation,
    val createdBackup: CreatedBackup?
) {

    data class CreatedBackup(val path: Path, val assetName: String, val assetSize: Long, val isEncrypted: Boolean)
    companion object {
        val INITIAL_STATE = BackupAndRestoreState(
            backupRestoreProgress = BackupRestoreProgress.Pending,
            restoreFileValidation = RestoreFileValidation.Pending,
            backupCreationProgress = BackupCreationProgress.Pending,
            restorePasswordValidation = PasswordValidation.NotVerified,
            backupPasswordValidation = PasswordValidation.Valid,
            createdBackup = null
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
    object IncompatibleBackup : RestoreFileValidation()
    object WrongBackup : RestoreFileValidation()
    data class PasswordRequired(val extractedBackupFilesRootPath: Path) : RestoreFileValidation()
    object GeneralFailure : RestoreFileValidation()
}
