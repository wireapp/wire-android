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

import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.lifecycle.AutomatedLoginManager
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
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
) : ViewModel() {

    internal var syncCompletionState: SyncCompletionState? by mutableStateOf(null)
        private set

    internal var isRestoringBackup: Boolean by mutableStateOf(false)
        private set

    internal var showBackupRootKeyUnavailableDialog: Boolean by mutableStateOf(false)
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

    private fun completeInitialSync() {
        syncCompletionState = pendingSyncCompletionState ?: SyncCompletionState(shouldMoveToBackground = false)
        pendingSyncCompletionState = null
    }

    private suspend fun loadConversationsBeforeBackupRestore() {
        syncConversations()
            .onFailure { appLogger.e("InitialSyncViewModel: failed to sync conversations before backup restore: $it") }
    }

    private suspend fun restoreLatestOnlineBackupIfExists(): Boolean {
        isRestoringBackup = true
        try {
            when (val result = restoreLatestOnlineBackup { }) {
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
            isRestoringBackup = false
        }
    }
}

internal data class SyncCompletionState(
    val shouldMoveToBackground: Boolean,
)

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
