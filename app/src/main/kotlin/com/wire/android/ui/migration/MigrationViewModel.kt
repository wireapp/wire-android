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

package com.wire.android.ui.migration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.wire.android.appLogger
import com.wire.android.migration.MigrationData
import com.wire.android.migration.MigrationManager
import com.wire.android.ui.navArgs
import com.wire.android.workmanager.worker.enqueueMigrationWorker
import com.wire.android.workmanager.worker.enqueueSingleUserMigrationWorker
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MigrationViewModel @Inject constructor(
    private val getCurrentSession: CurrentSessionUseCase,
    private val workManager: WorkManager,
    savedStateHandle: SavedStateHandle,
    private val migrationManager: MigrationManager
) : ViewModel() {

    var state: MigrationState by mutableStateOf(MigrationState.InProgress(MigrationData.Progress.Type.UNKNOWN))
        private set

    private val migrationNavArgs: MigrationNavArgs = savedStateHandle.navArgs()
    private val migrationType: MigrationType = migrationNavArgs.userId?.let { MigrationType.SingleUser(it) } ?: MigrationType.Full

    init {
        viewModelScope.launch {
            enqueueMigrationAndListenForStateChanges()
        }
    }

    fun retry() {
        viewModelScope.launch {
            // Flow collected in `enqueueMigrationAndListenForStateChanges` will still get updates for the newly enqueued work
            workManager.enqueueMigrationWorker()
        }
        state = MigrationState.InProgress(MigrationData.Progress.Type.UNKNOWN)
    }

    fun accountLogin(userHandle: String) {
        viewModelScope.launch {
            migrationManager.dismissMigrationFailureNotification()
            when (getCurrentSession()) {
                is CurrentSessionResult.Failure.Generic,
                CurrentSessionResult.Failure.SessionNotFound -> state = MigrationState.LoginRequired(userHandle)
                is CurrentSessionResult.Success -> state = MigrationState.Success(true)
            }
        }
    }

    fun finish() {
        viewModelScope.launch {
            migrationManager.dismissMigrationFailureNotification()
            updateStateAfterMigration()
        }
    }

    private suspend fun enqueueMigrationAndListenForStateChanges() {
        when (migrationType) {
            is MigrationType.SingleUser -> {
                appLogger.d("Enqueuing single user migration for user: ${migrationType.userId.value.obfuscateId()}")
                workManager.enqueueSingleUserMigrationWorker(migrationType.userId).collect {
                    handleMigrationResult(it)
                }
            }

            is MigrationType.Full -> {
                appLogger.d("Enqueuing migration for all users")
                workManager.enqueueMigrationWorker().collect {
                    handleMigrationResult(it)
                }
            }
        }
    }

    private suspend fun handleMigrationResult(data: MigrationData) {
        when (data) {
            is MigrationData.Result.Success -> updateStateAfterMigration()
            is MigrationData.Progress -> state = MigrationState.InProgress(data.type)
            is MigrationData.Result.Failure -> state = when (data) {
                is MigrationData.Result.Failure.Account.Any -> {
                    data.migrationReport?.let { error(it) }
                    MigrationState.Failed.Account.Any
                }
                is MigrationData.Result.Failure.Account.Specific -> {
                    data.migrationReport?.let { error(it) }
                    MigrationState.Failed.Account.Specific(data.userName, data.userHandle)
                }
                is MigrationData.Result.Failure.Messages -> {
                    data.migrationReport?.let { error(it) }
                    MigrationState.Failed.Messages(data.errorCode)
                }
                is MigrationData.Result.Failure.NoNetwork -> MigrationState.Failed.NoNetwork
                // for now we treat such an unknown error as one that requires re-logging in,
                // we do not show a special screen with any error code to users to not to discourage them
                is MigrationData.Result.Failure.Unknown -> {
                    data.migrationReport?.let { error(it) }
                    MigrationState.Failed.Account.Any
                }
            }
        }
    }

    private suspend fun updateStateAfterMigration() {
        when (getCurrentSession()) {
            is CurrentSessionResult.Success ->
                state = MigrationState.Success(true)

            else ->
                state = MigrationState.Success(false)
        }
    }
}
