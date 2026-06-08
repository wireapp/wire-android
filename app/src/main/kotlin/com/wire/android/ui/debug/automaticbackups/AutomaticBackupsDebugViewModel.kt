/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.debug.automaticbackups

import android.net.Uri
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.backup.BackupRootKeyInfo
import com.wire.kalium.logic.feature.backup.CreateOnlineBackupResult
import com.wire.kalium.logic.feature.backup.CreateOnlineBackupUseCase
import com.wire.kalium.logic.feature.backup.ExportBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.ExportBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.GenerateAndForcePushBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.GenerateAndForcePushBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.GetBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.GetBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.ImportBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.ImportBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.PushBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.RestoreLatestOnlineBackupResult
import com.wire.kalium.logic.feature.backup.RestoreLatestOnlineBackupUseCase
import com.wire.kalium.logic.feature.backup.SyncBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.SyncBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.toBackupRootKeyInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AutomaticBackupsDebugViewModel(
    private val getBackupRootKey: GetBackupRootKeyUseCase,
    private val syncBackupRootKey: SyncBackupRootKeyUseCase,
    private val generateAndForcePushBackupRootKey: GenerateAndForcePushBackupRootKeyUseCase,
    private val exportBackupRootKey: ExportBackupRootKeyUseCase,
    private val importBackupRootKey: ImportBackupRootKeyUseCase,
    private val createOnlineBackup: CreateOnlineBackupUseCase,
    private val restoreLatestOnlineBackup: RestoreLatestOnlineBackupUseCase,
    private val kaliumFileSystem: KaliumFileSystem,
    private val fileManager: FileManager,
    private val dispatcher: DispatcherProvider,
) : ViewModel() {

    val exportBackupRootKeyPasswordState: TextFieldState = TextFieldState()
    val importBackupRootKeyPasswordState: TextFieldState = TextFieldState()

    private val _state = MutableStateFlow(AutomaticBackupsDebugState())
    val state: StateFlow<AutomaticBackupsDebugState> = _state.asStateFlow()

    private val _infoMessage = MutableSharedFlow<UIText>()
    val infoMessage: SharedFlow<UIText> = _infoMessage.asSharedFlow()

    private val _effect = MutableSharedFlow<AutomaticBackupsDebugEffect>()
    val effect: SharedFlow<AutomaticBackupsDebugEffect> = _effect.asSharedFlow()

    init {
        loadBackupRootKey()
    }

    fun loadBackupRootKey() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = getBackupRootKey()) {
                is GetBackupRootKeyResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            backupRootKey = result.backupRootKey?.toBackupRootKeyInfo(),
                        )
                    }
                }
                is GetBackupRootKeyResult.Failure -> {
                    _state.update { it.copy(isLoading = false) }
                    _infoMessage.emit(UIText.DynamicString("Failed to load Backup Root Key: ${result.cause.message.orEmpty()}"))
                }
            }
        }
    }

    fun generateNewBackupRootKey() {
        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true) }
            when (val result = generateAndForcePushBackupRootKey()) {
                is GenerateAndForcePushBackupRootKeyResult.Success -> {
                    _state.update {
                        it.copy(
                            isGenerating = false,
                            backupRootKey = result.backupRootKey.toBackupRootKeyInfo(),
                        )
                    }
                    _infoMessage.emit(result.toInfoMessage())
                }
                is GenerateAndForcePushBackupRootKeyResult.Failure -> {
                    _state.update { it.copy(isGenerating = false) }
                    _infoMessage.emit(UIText.DynamicString("Failed to generate Backup Root Key: ${result.cause}"))
                }
            }
        }
    }

    fun fetchBackupRootKey() {
        viewModelScope.launch {
            _state.update { it.copy(isFetchingBackupRootKey = true) }
            when (val result = syncBackupRootKey()) {
                is SyncBackupRootKeyResult.Found -> {
                    _state.update {
                        it.copy(
                            isFetchingBackupRootKey = false,
                            backupRootKey = result.backupRootKey.toBackupRootKeyInfo(),
                        )
                    }
                    _infoMessage.emit(UIText.DynamicString("Backup Root Key fetched from another client"))
                }

                SyncBackupRootKeyResult.LocalKeyExists -> {
                    _state.update { it.copy(isFetchingBackupRootKey = false) }
                    loadBackupRootKey()
                    _infoMessage.emit(UIText.DynamicString("Backup Root Key already exists on this device"))
                }

                SyncBackupRootKeyResult.Unavailable -> {
                    _state.update { it.copy(isFetchingBackupRootKey = false) }
                    _infoMessage.emit(UIText.DynamicString("Backup Root Key unavailable from other clients"))
                }

                is SyncBackupRootKeyResult.Failure -> {
                    _state.update { it.copy(isFetchingBackupRootKey = false) }
                    _infoMessage.emit(UIText.DynamicString("Failed to fetch Backup Root Key: ${result.cause.message.orEmpty()}"))
                }
            }
        }
    }

    fun showExportBackupRootKeyPasswordDialog() {
        _state.update { it.copy(showExportBackupRootKeyPasswordDialog = true) }
    }

    fun dismissExportBackupRootKeyPasswordDialog() {
        exportBackupRootKeyPasswordState.clearText()
        _state.update { it.copy(showExportBackupRootKeyPasswordDialog = false) }
    }

    fun exportBackupRootKey() {
        viewModelScope.launch {
            _state.update { it.copy(isExportingBackupRootKey = true) }
            when (val result = exportBackupRootKey(exportBackupRootKeyPasswordState.text.toString())) {
                is ExportBackupRootKeyResult.Success -> {
                    exportBackupRootKeyPasswordState.clearText()
                    _state.update {
                        it.copy(
                            isExportingBackupRootKey = false,
                            showExportBackupRootKeyPasswordDialog = false,
                            pendingExportedBackupRootKey = PendingExportedBackupRootKey(
                                path = result.exportFilePath,
                                fileName = result.fileName,
                            ),
                        )
                    }
                    _effect.emit(AutomaticBackupsDebugEffect.CreateBackupRootKeyExportFile(result.fileName))
                }

                is ExportBackupRootKeyResult.Failure -> {
                    _state.update { it.copy(isExportingBackupRootKey = false) }
                    _infoMessage.emit(result.toInfoMessage())
                }
            }
        }
    }

    fun saveExportedBackupRootKey(uri: Uri?) {
        viewModelScope.launch {
            val pendingExport = state.value.pendingExportedBackupRootKey
            if (uri == null || pendingExport == null) {
                _state.update { it.copy(pendingExportedBackupRootKey = null) }
                _infoMessage.emit(UIText.DynamicString("Backup Root Key export save cancelled"))
                return@launch
            }

            try {
                fileManager.copyToUri(pendingExport.path, uri, dispatcher)
                _state.update { it.copy(pendingExportedBackupRootKey = null) }
                _infoMessage.emit(UIText.DynamicString("Backup Root Key exported"))
            } catch (e: Exception) {
                _state.update { it.copy(pendingExportedBackupRootKey = null) }
                _infoMessage.emit(UIText.DynamicString("Failed to save Backup Root Key export: ${e.message.orEmpty()}"))
            }
        }
    }

    fun chooseBackupRootKeyToImport(uri: Uri?) {
        viewModelScope.launch {
            if (uri == null) {
                _state.update { it.copy(pendingImportedBackupRootKeyPath = null) }
                _infoMessage.emit(UIText.DynamicString("Backup Root Key import cancelled"))
                return@launch
            }

            val importedBackupRootKeyPath = kaliumFileSystem.tempFilePath(TEMP_IMPORTED_BACKUP_ROOT_KEY_FILE_NAME)
            try {
                fileManager.copyToPath(uri, importedBackupRootKeyPath, dispatcher)
                _state.update {
                    it.copy(
                        pendingImportedBackupRootKeyPath = importedBackupRootKeyPath,
                        showImportBackupRootKeyPasswordDialog = true,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        pendingImportedBackupRootKeyPath = null,
                        showImportBackupRootKeyPasswordDialog = false,
                    )
                }
                _infoMessage.emit(UIText.DynamicString("Failed to read Backup Root Key import: ${e.message.orEmpty()}"))
            }
        }
    }

    fun dismissImportBackupRootKeyPasswordDialog() {
        importBackupRootKeyPasswordState.clearText()
        _state.update {
            it.copy(
                showImportBackupRootKeyPasswordDialog = false,
                pendingImportedBackupRootKeyPath = null,
            )
        }
    }

    fun importBackupRootKey() {
        viewModelScope.launch {
            val pendingImportPath = state.value.pendingImportedBackupRootKeyPath
            if (pendingImportPath == null) {
                _infoMessage.emit(UIText.DynamicString("Select a Backup Root Key export file to import"))
                return@launch
            }

            _state.update { it.copy(isImportingBackupRootKey = true) }
            when (val result = importBackupRootKey(pendingImportPath, importBackupRootKeyPasswordState.text.toString())) {
                is ImportBackupRootKeyResult.Success -> {
                    importBackupRootKeyPasswordState.clearText()
                    _state.update {
                        it.copy(
                            isImportingBackupRootKey = false,
                            showImportBackupRootKeyPasswordDialog = false,
                            pendingImportedBackupRootKeyPath = null,
                            backupRootKey = result.backupRootKey.toBackupRootKeyInfo(),
                        )
                    }
                    _infoMessage.emit(UIText.DynamicString("Backup Root Key imported"))
                }

                is ImportBackupRootKeyResult.Failure -> {
                    _state.update { it.copy(isImportingBackupRootKey = false) }
                    _infoMessage.emit(result.toInfoMessage())
                }
            }
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isCreatingBackup = true,
                    backupCreationProgress = 0f,
                )
            }
            val result = createOnlineBackup { progress ->
                _state.update { it.copy(backupCreationProgress = progress) }
            }
            _state.update {
                it.copy(
                    isCreatingBackup = false,
                    backupCreationProgress = 0f,
                )
            }
            _infoMessage.emit(result.toInfoMessage())
        }
    }

    fun restoreLatestBackup() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isRestoringBackup = true,
                    backupRestoreProgress = 0f,
                )
            }
            val result = restoreLatestOnlineBackup { progress ->
                _state.update { it.copy(backupRestoreProgress = progress) }
            }
            _state.update {
                it.copy(
                    isRestoringBackup = false,
                    backupRestoreProgress = 0f,
                )
            }
            _infoMessage.emit(result.toInfoMessage())
        }
    }

    private fun RestoreLatestOnlineBackupResult.toInfoMessage(): UIText = UIText.DynamicString(
        when (this) {
            is RestoreLatestOnlineBackupResult.Success -> "Backup restored: ${metadata.fileName}"
            RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable ->
                "Restore failed: no backup root key available"
            RestoreLatestOnlineBackupResult.Failure.NoOnlineBackupFound ->
                "Restore failed: no online backup found"
            RestoreLatestOnlineBackupResult.Failure.RootKeyIdMismatch ->
                "Restore failed: backup root key id mismatch"
            RestoreLatestOnlineBackupResult.Failure.BackupBelongsToAnotherUser ->
                "Restore failed: backup belongs to another user"
            is RestoreLatestOnlineBackupResult.Failure.BackupListFailed ->
                "Restore failed while listing backups: $cause"
            is RestoreLatestOnlineBackupResult.Failure.DownloadFailed ->
                "Restore failed while downloading backup: $cause"
            RestoreLatestOnlineBackupResult.Failure.InvalidPassphrase ->
                "Restore failed: invalid passphrase"
            is RestoreLatestOnlineBackupResult.Failure.RestoreFailed ->
                "Restore failed while importing backup: ${cause.cause}"
            is RestoreLatestOnlineBackupResult.Failure.Unknown ->
                "Restore failed: ${cause.message.orEmpty()}"
        }
    )

    private fun CreateOnlineBackupResult.toInfoMessage(): UIText = UIText.DynamicString(
        when (this) {
            is CreateOnlineBackupResult.Success -> "Backup created: ${metadata.fileName}"
            CreateOnlineBackupResult.Skipped.NoReceivedMessages -> "Backup skipped: no received messages"
            is CreateOnlineBackupResult.Skipped.UpToDate -> "Backup skipped: already up to date"
            is CreateOnlineBackupResult.Failure.BackupListFailed -> "Backup failed while listing backups: $cause"
            is CreateOnlineBackupResult.Failure.MessageTimestampFailed -> "Backup failed while reading latest message timestamp: $cause"
            is CreateOnlineBackupResult.Failure.BackupCreationFailed -> "Backup failed while creating backup: $cause"
            is CreateOnlineBackupResult.Failure.UploadFailed -> "Backup failed while uploading backup: $cause"
            is CreateOnlineBackupResult.Failure.MetadataRegistrationFailed -> "Backup failed while registering metadata: $cause"
            is CreateOnlineBackupResult.Failure.Unknown -> "Backup failed: ${cause.message.orEmpty()}"
        }
    )

    private fun GenerateAndForcePushBackupRootKeyResult.Success.toInfoMessage(): UIText = UIText.DynamicString(
        when (val result = pushResult) {
            PushBackupRootKeyResult.Success -> "Backup Root Key generated and pushed"
            is PushBackupRootKeyResult.PartialFailure -> "Backup Root Key generated, but push was partial: ${result.cause}"
            is PushBackupRootKeyResult.Failure -> "Backup Root Key generated, but push failed: ${result.cause.message.orEmpty()}"
        }
    )

    private fun ExportBackupRootKeyResult.Failure.toInfoMessage(): UIText = UIText.DynamicString(
        when (this) {
            ExportBackupRootKeyResult.Failure.BlankPassword -> "Enter a password to export Backup Root Key"
            ExportBackupRootKeyResult.Failure.NoBackupRootKey -> "Backup Root Key export failed: no key available"
            is ExportBackupRootKeyResult.Failure.EncryptionFailure ->
                "Backup Root Key export failed while encrypting: ${cause.message.orEmpty()}"
            is ExportBackupRootKeyResult.Failure.StorageFailure ->
                "Backup Root Key export failed while accessing storage: ${cause.message.orEmpty()}"
        }
    )

    private fun ImportBackupRootKeyResult.Failure.toInfoMessage(): UIText = UIText.DynamicString(
        when (this) {
            ImportBackupRootKeyResult.Failure.BlankPassword -> "Enter a password to import Backup Root Key"
            ImportBackupRootKeyResult.Failure.InvalidFile -> "Backup Root Key import failed: invalid export file"
            ImportBackupRootKeyResult.Failure.AuthenticationFailure -> "Backup Root Key import failed: wrong password"
            ImportBackupRootKeyResult.Failure.UserMismatch -> "Backup Root Key import failed: key belongs to another user"
            ImportBackupRootKeyResult.Failure.FingerprintMismatch ->
                "Backup Root Key import failed: key fingerprint mismatch"
            is ImportBackupRootKeyResult.Failure.DecryptionFailure ->
                "Backup Root Key import failed while decrypting: ${cause.message.orEmpty()}"
            is ImportBackupRootKeyResult.Failure.StorageFailure ->
                "Backup Root Key import failed while accessing storage: ${cause.message.orEmpty()}"
        }
    )
}

data class AutomaticBackupsDebugState(
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val isFetchingBackupRootKey: Boolean = false,
    val showExportBackupRootKeyPasswordDialog: Boolean = false,
    val isExportingBackupRootKey: Boolean = false,
    val pendingExportedBackupRootKey: PendingExportedBackupRootKey? = null,
    val showImportBackupRootKeyPasswordDialog: Boolean = false,
    val isImportingBackupRootKey: Boolean = false,
    val pendingImportedBackupRootKeyPath: okio.Path? = null,
    val isCreatingBackup: Boolean = false,
    val backupCreationProgress: Float = 0f,
    val isRestoringBackup: Boolean = false,
    val backupRestoreProgress: Float = 0f,
    val backupRootKey: BackupRootKeyInfo? = null,
)

data class PendingExportedBackupRootKey(
    val path: okio.Path,
    val fileName: String,
)

sealed interface AutomaticBackupsDebugEffect {
    data class CreateBackupRootKeyExportFile(val fileName: String) : AutomaticBackupsDebugEffect
}

private const val TEMP_IMPORTED_BACKUP_ROOT_KEY_FILE_NAME = "imported-backup-root-key.wbrk"
