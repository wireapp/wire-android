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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStore
import com.wire.android.feature.analytics.AnonymousAnalyticsManagerImpl
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.backup.BackupFileFormat
import com.wire.kalium.logic.feature.backup.CreateBackupResult
import com.wire.kalium.logic.feature.backup.CreateBackupUseCase
import com.wire.kalium.logic.feature.backup.CreateMPBackupUseCase
import com.wire.kalium.logic.feature.backup.RestoreBackupResult
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.BackupIOFailure
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.DecryptionFailure
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.IncompatibleBackup
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.InvalidPassword
import com.wire.kalium.logic.feature.backup.RestoreBackupResult.BackupRestoreFailure.InvalidUserId
import com.wire.kalium.logic.feature.backup.RestoreBackupUseCase
import com.wire.kalium.logic.feature.backup.RestoreMPBackupUseCase
import com.wire.kalium.logic.feature.backup.VerifyBackupResult
import com.wire.kalium.logic.feature.backup.VerifyBackupUseCase
import com.wire.kalium.util.DateTimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class BackupAndRestoreViewModel @Inject constructor(
    private val importBackup: RestoreBackupUseCase,
    private val importMpBackup: RestoreMPBackupUseCase,
    private val createBackupFile: CreateBackupUseCase,
    private val createMpBackupFile: CreateMPBackupUseCase,
    private val verifyBackup: VerifyBackupUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val kaliumFileSystem: KaliumFileSystem,
    private val fileManager: FileManager,
    private val userDataStore: UserDataStore,
    private val dispatcher: DispatcherProvider,
    private val mpBackupSettings: MPBackupSettings,
) : ViewModel() {

    val createBackupPasswordState: TextFieldState = TextFieldState()
    val restoreBackupPasswordState: TextFieldState = TextFieldState()
    var state by mutableStateOf(BackupAndRestoreState.INITIAL_STATE)

    @VisibleForTesting
    internal var latestCreatedBackup: BackupAndRestoreState.CreatedBackup? = null

    @VisibleForTesting
    internal lateinit var latestImportedBackupTempPath: Path

    init {
        observeLastBackupDate()
        observeCreateBackupPasswordChanges()
    }

    private fun observeCreateBackupPasswordChanges() {
        viewModelScope.launch {
            createBackupPasswordState.textAsFlow().collectLatest {
                validateBackupCreationPassword(it.toString())
            }
        }
    }

    private fun observeLastBackupDate() {
        viewModelScope.launch {
            userDataStore.lastBackupDateSeconds().collect {
                state = state.copy(lastBackupData = it)
            }
        }
    }

    fun createBackup() = viewModelScope.launch {

        val password = createBackupPasswordState.text.toString()

        val result = if (mpBackupSettings is MPBackupSettings.Enabled) {
            createMpBackupFile(password) { progress ->
                updateCreationProgress(progress)
            }
        } else {
            createBackupFile(password)
        }

        when (result) {
            is CreateBackupResult.Success -> {
                state = state.copy(backupCreationProgress = BackupCreationProgress.Finished(result.backupFileName))
                latestCreatedBackup = BackupAndRestoreState.CreatedBackup(
                    result.backupFilePath,
                    result.backupFileName,
                    createBackupPasswordState.text.isNotEmpty()
                )
                createBackupPasswordState.clearText()
            }

            is CreateBackupResult.Failure -> {
                state = state.copy(backupCreationProgress = BackupCreationProgress.Failed)
                appLogger.e("Failed to create backup: ${result.coreFailure}")
                AnonymousAnalyticsManagerImpl.sendEvent(event = AnalyticsEvent.BackupExportFailed)
            }
        }
    }

    private suspend fun updateLastBackupDate() {
        DateTimeUtil.currentInstant().epochSeconds.also { currentTime ->
            userDataStore.setLastBackupDateSeconds(currentTime)
        }
    }

    fun shareBackup() = viewModelScope.launch {
        updateLastBackupDate()
        latestCreatedBackup?.let { backupData ->
            withContext(dispatcher.io()) {
                fileManager.shareWithExternalApp(backupData.path, backupData.assetName) {}
            }
        }
        state = state.copy(
            backupRestoreProgress = BackupRestoreProgress.InProgress(),
            restoreFileValidation = RestoreFileValidation.Initial,
            backupCreationProgress = BackupCreationProgress.InProgress(),
            restorePasswordValidation = PasswordValidation.NotVerified,
            passwordValidation = ValidatePasswordResult.Valid,
        )
    }

    fun saveBackup(uri: Uri) = viewModelScope.launch {
        updateLastBackupDate()
        latestCreatedBackup?.let { backupData ->
            fileManager.copyToUri(backupData.path, uri, dispatcher)
        }
        state = state.copy(
            backupRestoreProgress = BackupRestoreProgress.InProgress(),
            restoreFileValidation = RestoreFileValidation.Initial,
            backupCreationProgress = BackupCreationProgress.InProgress(),
            restorePasswordValidation = PasswordValidation.NotVerified,
            passwordValidation = ValidatePasswordResult.Valid,
        )
    }

    fun chooseBackupFileToRestore(uri: Uri) = viewModelScope.launch {
        latestImportedBackupTempPath = kaliumFileSystem.tempFilePath(TEMP_IMPORTED_BACKUP_FILE_NAME)
        fileManager.copyToPath(uri, latestImportedBackupTempPath)
        verifyBackupFile(latestImportedBackupTempPath)
    }

    private fun showPasswordDialog() {
        state = state.copy(restoreFileValidation = RestoreFileValidation.PasswordRequired)
    }

    private suspend fun verifyBackupFile(importedBackupPath: Path) = withContext(dispatcher.main()) {
        when (val result = verifyBackup(importedBackupPath)) {
            is VerifyBackupResult.Success -> {
                state = state.copy(backupFileFormat = result.format)
                if (result.isEncrypted) {
                    showPasswordDialog()
                } else {
                    state = state.copy(
                        restoreFileValidation = RestoreFileValidation.ValidNonEncryptedBackup,
                    )
                    restoreBackup(importedBackupPath, null)
                }
            }

            is VerifyBackupResult.Failure -> {
                state = state.copy(restoreFileValidation = RestoreFileValidation.IncompatibleBackup)
                val errorMessage = when (result) {
                    is VerifyBackupResult.Failure.Generic -> result.error.toString()
                    VerifyBackupResult.Failure.InvalidBackupFile -> "No valid files found in the backup"
                    is VerifyBackupResult.Failure.UnsupportedVersion -> "Unsupported backup version: ${result.version}"
                    VerifyBackupResult.Failure.InvalidUserId -> {
                        state = state.copy(
                            backupRestoreProgress = BackupRestoreProgress.Failed,
                            restoreFileValidation = RestoreFileValidation.WrongBackup,
                            restorePasswordValidation = PasswordValidation.Valid
                        )
                        "Invalid user ID"
                    }
                }

                AnonymousAnalyticsManagerImpl.sendEvent(event = AnalyticsEvent.BackupRestoreFailed)
                appLogger.e("Failed to extract backup files: $errorMessage")
            }
        }
    }

    fun restorePasswordProtectedBackup() = viewModelScope.launch(dispatcher.main()) {
        state = state.copy(
            restorePasswordValidation = PasswordValidation.NotVerified
        )
        delay(SMALL_DELAY)
        val fileValidationState = state.restoreFileValidation
        if (fileValidationState is RestoreFileValidation.PasswordRequired) {
            state = state.copy(restorePasswordValidation = PasswordValidation.Entered)

            restoreBackup(latestImportedBackupTempPath, restoreBackupPasswordState.text.toString())
        } else {
            state = state.copy(backupRestoreProgress = BackupRestoreProgress.Failed)
            AnonymousAnalyticsManagerImpl.sendEvent(event = AnalyticsEvent.BackupRestoreFailed)
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

    fun validateBackupCreationPassword(backupPassword: String) {
        state = state.copy(
            passwordValidation = if (backupPassword.isEmpty()) {
                ValidatePasswordResult.Valid
            } else {
                validatePassword(backupPassword)
            }
        )
    }

    fun cancelBackupCreation() = viewModelScope.launch(dispatcher.main()) {
        createBackupPasswordState.clearText()
        updateCreationProgress(0f)
    }

    fun cancelBackupRestore() = viewModelScope.launch {
        restoreBackupPasswordState.clearText()
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

    private fun restoreBackup(backupFilePath: Path, password: String?) = viewModelScope.launch {
        val result = when (state.backupFileFormat) {
            BackupFileFormat.ANDROID -> importBackup(backupFilePath, password)
            BackupFileFormat.MULTIPLATFORM -> importMpBackup(backupFilePath, password) { progress ->
                updateRestoreProgress(progress)
            }
        }
        when (result) {
            RestoreBackupResult.Success -> {
                state = state.copy(
                    backupRestoreProgress = BackupRestoreProgress.Finished,
                    restorePasswordValidation = PasswordValidation.Valid
                )
                restoreBackupPasswordState.clearText()
                AnonymousAnalyticsManagerImpl.sendEvent(event = AnalyticsEvent.BackupRestoreSucceeded)
            }

            is RestoreBackupResult.Failure -> {
                mapBackupRestoreFailure(result.failure)
                AnonymousAnalyticsManagerImpl.sendEvent(event = AnalyticsEvent.BackupRestoreFailed)
            }
        }
    }

    private fun updateCreationProgress(progress: Float) = viewModelScope.launch {
        state = state.copy(backupCreationProgress = BackupCreationProgress.InProgress(progress))
    }

    private fun updateRestoreProgress(progress: Float) = viewModelScope.launch {
        state = state.copy(backupRestoreProgress = BackupRestoreProgress.InProgress(progress))
    }

    internal companion object {
        const val TEMP_IMPORTED_BACKUP_FILE_NAME = "tempImportedBackup.zip"
        const val SMALL_DELAY = 300L
    }
}
