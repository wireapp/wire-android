/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home.settings.backup

import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class BackupAndRestoreViewModel
@Inject constructor(
    private val importBackup: RestoreBackupUseCase,
    private val createBackupFile: CreateBackupUseCase,
    private val verifyBackup: VerifyBackupUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val kaliumFileSystem: KaliumFileSystem,
    private val fileManager: FileManager,
    private val dispatcher: DispatcherProvider,
) : ViewModel() {

    var state by mutableStateOf(BackupAndRestoreState.INITIAL_STATE)

    @VisibleForTesting
    internal var latestCreatedBackup: BackupAndRestoreState.CreatedBackup? = null

    @VisibleForTesting
    internal lateinit var latestImportedBackupTempPath: Path

    fun createBackup(password: String) = viewModelScope.launch(dispatcher.main()) {
        // TODO: Find a way to update the creation progress more faithfully. For now we will just show this small delays to mimic the
        //  progress also for small backups
        updateCreationProgress(PROGRESS_25)
        delay(SMALL_DELAY)
        updateCreationProgress(PROGRESS_50)
        delay(SMALL_DELAY)

        when (val result = createBackupFile(password)) {
            is CreateBackupResult.Success -> {
                state = state.copy(backupCreationProgress = BackupCreationProgress.Finished(result.backupFileName))
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

    fun shareBackup() = viewModelScope.launch(dispatcher.main()) {
        latestCreatedBackup?.let { backupData ->
            withContext(dispatcher.io()) {
                fileManager.shareWithExternalApp(backupData.path, backupData.assetName) {}
            }
        }
        state = BackupAndRestoreState.INITIAL_STATE
    }

    fun saveBackup(uri: Uri) = viewModelScope.launch(dispatcher.main()) {
        latestCreatedBackup?.let { backupData ->
            fileManager.copyToUri(backupData.path, uri, dispatcher)
        }
        state = BackupAndRestoreState.INITIAL_STATE
    }

    fun chooseBackupFileToRestore(uri: Uri) = viewModelScope.launch {
        latestImportedBackupTempPath = kaliumFileSystem.tempFilePath(TEMP_IMPORTED_BACKUP_FILE_NAME)
        fileManager.copyToPath(uri, latestImportedBackupTempPath)
        checkIfBackupEncrypted(latestImportedBackupTempPath)
    }

    private fun showPasswordDialog() {
        state = state.copy(restoreFileValidation = RestoreFileValidation.PasswordRequired)
    }

    private suspend fun checkIfBackupEncrypted(importedBackupPath: Path) = withContext(dispatcher.main()) {
        when (val result = verifyBackup(importedBackupPath)) {
            is VerifyBackupResult.Success -> {
                when (result) {
                    is VerifyBackupResult.Success.Encrypted -> showPasswordDialog()
                    is VerifyBackupResult.Success.NotEncrypted -> importDatabase(importedBackupPath)
                    VerifyBackupResult.Success.Web -> {
                        if (BuildConfig.DEVELOPER_FEATURES_ENABLED) {
                            importDatabase(importedBackupPath)
                        } else {
                            state = state.copy(restoreFileValidation = RestoreFileValidation.IncompatibleBackup)
                        }
                    }
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
                updateCreationProgress(PROGRESS_75)
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
    }

    fun restorePasswordProtectedBackup(restorePassword: String) = viewModelScope.launch(dispatcher.main()) {
        state = state.copy(
            backupRestoreProgress = BackupRestoreProgress.InProgress(PROGRESS_50),
            restorePasswordValidation = PasswordValidation.NotVerified
        )
        delay(SMALL_DELAY)
        val fileValidationState = state.restoreFileValidation
        if (fileValidationState is RestoreFileValidation.PasswordRequired) {
            state = state.copy(restorePasswordValidation = PasswordValidation.Entered)
            when (val result = importBackup(latestImportedBackupTempPath, restorePassword)) {
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
        state = state.copy(
            passwordValidation = if (backupPassword.text.isEmpty()) {
                ValidatePasswordResult.Valid
            } else {
                validatePassword(backupPassword.text)
            }
        )
    }

    fun cancelBackupCreation() = viewModelScope.launch(dispatcher.main()) {
        updateCreationProgress(0f)
    }

    fun cancelBackupRestore() = viewModelScope.launch {
        state = state.copy(
            restoreFileValidation = RestoreFileValidation.Initial,
            backupRestoreProgress = BackupRestoreProgress.InProgress(),
            restorePasswordValidation = PasswordValidation.NotVerified
        )
        withContext(dispatcher.io()) {
            if (this@BackupAndRestoreViewModel::latestImportedBackupTempPath.isInitialized && kaliumFileSystem.exists(
                    latestImportedBackupTempPath
                )
            ) kaliumFileSystem.delete(latestImportedBackupTempPath)
        }
    }

    private suspend fun updateCreationProgress(progress: Float) = withContext(dispatcher.main()) {
        state = state.copy(backupCreationProgress = BackupCreationProgress.InProgress(progress))
    }

    internal companion object {
        const val TEMP_IMPORTED_BACKUP_FILE_NAME = "tempImportedBackup.zip"
        const val SMALL_DELAY = 300L
        const val PROGRESS_25 = 0.25f
        const val PROGRESS_50 = 0.50f
        const val PROGRESS_75 = 0.75f
    }
}
