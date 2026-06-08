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

package com.wire.android.ui.initialsync

import android.net.Uri
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.lifecycle.AutomatedLoginManager
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.backup.ImportBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.ImportBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.RestoreLatestOnlineBackupResult
import com.wire.kalium.logic.feature.backup.RestoreLatestOnlineBackupUseCase
import com.wire.kalium.logic.feature.conversation.SyncConversationsUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class InitialSyncViewModel(
    private val observeSyncState: ObserveSyncStateUseCase,
    private val userDataStoreProvider: UserDataStoreProvider,
    @CurrentAccount private val userId: UserId,
    private val dispatchers: DispatcherProvider,
    private val automatedLoginManager: AutomatedLoginManager,
    private val syncConversations: SyncConversationsUseCase,
    private val restoreLatestOnlineBackup: RestoreLatestOnlineBackupUseCase,
    private val importBackupRootKey: ImportBackupRootKeyUseCase,
    private val kaliumFileSystem: KaliumFileSystem,
    private val fileManager: FileManager,
) : ViewModel() {

    internal val importBackupRootKeyPasswordState: TextFieldState = TextFieldState()

    internal var syncCompletionState: SyncCompletionState? by mutableStateOf(null)
        private set

    internal var backupRestoreState: InitialSyncBackupRestoreState by mutableStateOf(
        InitialSyncBackupRestoreState.None
    )
        private set

    internal var showBackupRootKeyUnavailableDialog: Boolean by mutableStateOf(false)
        private set

    internal var showImportBackupRootKeyPasswordDialog: Boolean by mutableStateOf(false)
        private set

    internal var isImportingBackupRootKey: Boolean by mutableStateOf(false)
        private set

    internal var pendingImportedBackupRootKeyPath: okio.Path? by mutableStateOf(null)
        private set

    private var pendingSyncCompletionState: SyncCompletionState? = null

    private val _restoreErrorToast = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    internal val restoreErrorToast: SharedFlow<Int> = _restoreErrorToast.asSharedFlow()

    internal val isSyncCompleted: Boolean
        get() = syncCompletionState != null

    internal val shouldMoveToBackground: Boolean
        get() = syncCompletionState?.shouldMoveToBackground == true

    init {
        waitUntilSyncIsCompleted()
    }

    private fun waitUntilSyncIsCompleted() =
        viewModelScope.launch(dispatchers.io()) {
            delay(DefaultDurationMillis.toLong()) // it can be triggered instantly so it's added to keep smooth transitions
            observeSyncState().firstOrNull { it is SyncState.Live }?.let {
                userDataStoreProvider.getOrCreate(userId).setInitialSyncCompleted()
                val shouldMoveToBackground = automatedLoginManager.consumePendingMoveToBackgroundAfterSync()
                pendingSyncCompletionState = SyncCompletionState(
                    shouldMoveToBackground = shouldMoveToBackground
                )
                loadConversationsBeforeBackupRestore()
                if (restoreLatestOnlineBackupIfExists()) {
                    completeInitialSync()
                }
            } ?: run {
                appLogger.e("InitialSyncViewModel: SyncState is null")
            }
        }

    internal fun onBackupRootKeyDialogTryAgain() {
        showBackupRootKeyUnavailableDialog = false
        viewModelScope.launch(dispatchers.io()) {
            if (restoreLatestOnlineBackupIfExists()) {
                completeInitialSync()
            }
        }
    }

    internal fun onBackupRootKeyDialogCancel() {
        showBackupRootKeyUnavailableDialog = false
        completeInitialSync()
    }

    internal fun onBackupRootKeyImportFileSelected(uri: Uri?) {
        viewModelScope.launch(dispatchers.io()) {
            if (uri == null) {
                pendingImportedBackupRootKeyPath = null
                showImportBackupRootKeyPasswordDialog = false
                showBackupRootKeyUnavailableDialog = true
                return@launch
            }

            val importedBackupRootKeyPath = kaliumFileSystem.tempFilePath(TEMP_IMPORTED_BACKUP_ROOT_KEY_FILE_NAME)
            try {
                fileManager.copyToPath(uri, importedBackupRootKeyPath, dispatchers)
                pendingImportedBackupRootKeyPath = importedBackupRootKeyPath
                showBackupRootKeyUnavailableDialog = false
                showImportBackupRootKeyPasswordDialog = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                pendingImportedBackupRootKeyPath = null
                showImportBackupRootKeyPasswordDialog = false
                showBackupRootKeyUnavailableDialog = true
                _restoreErrorToast.emit(R.string.initial_sync_import_backup_root_key_failed)
            }
        }
    }

    internal fun onImportBackupRootKeyPasswordDialogDismiss() {
        importBackupRootKeyPasswordState.clearText()
        pendingImportedBackupRootKeyPath = null
        isImportingBackupRootKey = false
        showImportBackupRootKeyPasswordDialog = false
        showBackupRootKeyUnavailableDialog = true
    }

    internal fun onImportBackupRootKey() {
        viewModelScope.launch(dispatchers.io()) {
            val pendingImportPath = pendingImportedBackupRootKeyPath
            if (pendingImportPath == null) {
                showImportBackupRootKeyPasswordDialog = false
                showBackupRootKeyUnavailableDialog = true
                _restoreErrorToast.emit(R.string.initial_sync_import_backup_root_key_failed)
                return@launch
            }

            isImportingBackupRootKey = true
            val result = try {
                importBackupRootKey(pendingImportPath, importBackupRootKeyPasswordState.text.toString())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appLogger.e("InitialSyncViewModel: backup root key import failed: ${e.message.orEmpty()}")
                isImportingBackupRootKey = false
                _restoreErrorToast.emit(R.string.initial_sync_import_backup_root_key_failed)
                return@launch
            }

            when (result) {
                is ImportBackupRootKeyResult.Success -> {
                    importBackupRootKeyPasswordState.clearText()
                    pendingImportedBackupRootKeyPath = null
                    isImportingBackupRootKey = false
                    showImportBackupRootKeyPasswordDialog = false
                    showBackupRootKeyUnavailableDialog = false
                    if (restoreLatestOnlineBackupIfExists()) {
                        completeInitialSync()
                    }
                }
                is ImportBackupRootKeyResult.Failure -> {
                    isImportingBackupRootKey = false
                    _restoreErrorToast.emit(result.messageResId())
                }
            }
        }
    }

    private fun completeInitialSync() {
        syncCompletionState = pendingSyncCompletionState ?: SyncCompletionState(shouldMoveToBackground = false)
        pendingSyncCompletionState = null
    }

    private suspend fun loadConversationsBeforeBackupRestore() {
        syncConversations()
            .onFailure { appLogger.e("InitialSyncViewModel: failed to sync conversations before backup restore: $it") }
    }

    private suspend fun restoreLatestOnlineBackupIfExists(): Boolean {
        backupRestoreState = InitialSyncBackupRestoreState.Checking
        try {
            when (val result = restoreLatestOnlineBackup { progress ->
                backupRestoreState = InitialSyncBackupRestoreState.Restoring(progress.coerceIn(0f, 1f))
            }) {
                is RestoreLatestOnlineBackupResult.Success -> {
                    appLogger.i("InitialSyncViewModel: latest online backup restored")
                    return true
                }
                RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable -> {
                    appLogger.i("InitialSyncViewModel: latest online backup restore paused: NoBackupRootKeyAvailable")
                    showBackupRootKeyUnavailableDialog = true
                    return false
                }
                RestoreLatestOnlineBackupResult.Failure.NoOnlineBackupFound -> {
                    appLogger.i("InitialSyncViewModel: latest online backup restore skipped: NoOnlineBackupFound")
                    return true
                }
                is RestoreLatestOnlineBackupResult.Failure -> {
                    appLogger.e("InitialSyncViewModel: latest online backup restore failed: ${result.logName()}")
                    _restoreErrorToast.emit(R.string.initial_sync_restore_backup_failed)
                    return true
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            appLogger.e("InitialSyncViewModel: latest online backup restore failed: ${e.message.orEmpty()}")
            _restoreErrorToast.emit(R.string.initial_sync_restore_backup_failed)
            return true
        } finally {
            backupRestoreState = InitialSyncBackupRestoreState.None
        }
    }
}

internal data class SyncCompletionState(
    val shouldMoveToBackground: Boolean,
)

internal sealed interface InitialSyncBackupRestoreState {
    data object None : InitialSyncBackupRestoreState
    data object Checking : InitialSyncBackupRestoreState
    data class Restoring(val progress: Float) : InitialSyncBackupRestoreState
}

private fun RestoreLatestOnlineBackupResult.Failure.logName(): String =
    when (this) {
        RestoreLatestOnlineBackupResult.Failure.BackupBelongsToAnotherUser -> "BackupBelongsToAnotherUser"
        is RestoreLatestOnlineBackupResult.Failure.BackupListFailed -> "BackupListFailed"
        is RestoreLatestOnlineBackupResult.Failure.DownloadFailed -> "DownloadFailed"
        RestoreLatestOnlineBackupResult.Failure.InvalidPassphrase -> "InvalidPassphrase"
        RestoreLatestOnlineBackupResult.Failure.NoBackupRootKeyAvailable -> "NoBackupRootKeyAvailable"
        RestoreLatestOnlineBackupResult.Failure.NoOnlineBackupFound -> "NoOnlineBackupFound"
        RestoreLatestOnlineBackupResult.Failure.RootKeyIdMismatch -> "RootKeyIdMismatch"
        is RestoreLatestOnlineBackupResult.Failure.RestoreFailed -> "RestoreFailed"
        is RestoreLatestOnlineBackupResult.Failure.Unknown -> "Unknown"
    }

private fun ImportBackupRootKeyResult.Failure.messageResId(): Int =
    when (this) {
        ImportBackupRootKeyResult.Failure.BlankPassword -> R.string.initial_sync_import_backup_root_key_blank_password
        ImportBackupRootKeyResult.Failure.InvalidFile -> R.string.initial_sync_import_backup_root_key_invalid_file
        ImportBackupRootKeyResult.Failure.AuthenticationFailure -> R.string.initial_sync_import_backup_root_key_wrong_password
        ImportBackupRootKeyResult.Failure.UserMismatch -> R.string.initial_sync_import_backup_root_key_wrong_user
        ImportBackupRootKeyResult.Failure.FingerprintMismatch -> R.string.initial_sync_import_backup_root_key_fingerprint_mismatch
        is ImportBackupRootKeyResult.Failure.DecryptionFailure -> R.string.initial_sync_import_backup_root_key_failed
        is ImportBackupRootKeyResult.Failure.StorageFailure -> R.string.initial_sync_import_backup_root_key_failed
    }

private const val TEMP_IMPORTED_BACKUP_ROOT_KEY_FILE_NAME = "imported-backup-root-key.wbrk"
