/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
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
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.workmanager.worker.enqueueMigrationWorker
import com.wire.android.workmanager.worker.enqueueSingleUserMigrationWorker
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MigrationViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val getCurrentSession: CurrentSessionUseCase,
    private val workManager: WorkManager,
    savedStateHandle: SavedStateHandle,
    private val migrationManager: MigrationManager
) : ViewModel() {

    var state: MigrationState by mutableStateOf(MigrationState.InProgress(MigrationData.Progress.Type.UNKNOWN))
        private set

    private val migrationType: MigrationType = savedStateHandle.get<String>(EXTRA_USER_ID)?.let {
        QualifiedIdMapperImpl(null).fromStringToQualifiedID(it)
    }?.let { MigrationType.SingleUser(it) } ?: MigrationType.Full

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
                CurrentSessionResult.Failure.SessionNotFound -> navigateToLogin(userHandle)
                is CurrentSessionResult.Success -> navigationManager.navigate(
                    NavigationCommand(
                        NavigationItem.Home.getRouteWithArgs(),
                        BackStackMode.CLEAR_WHOLE
                    )
                )
            }
        }
    }

    fun finish() {
        viewModelScope.launch {
            migrationManager.dismissMigrationFailureNotification()
            navigateAfterMigration()
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
            is MigrationData.Result.Success -> navigateAfterMigration()
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

    private suspend fun navigateAfterMigration() {
        when (getCurrentSession()) {
            is CurrentSessionResult.Success ->
                navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))

            else ->
                navigationManager.navigate(NavigationCommand(NavigationItem.Welcome.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
        }
    }

    private suspend fun navigateToLogin(userHandle: String) {
        navigationManager.navigate(
            NavigationCommand(NavigationItem.Login.getRouteWithArgs(listOf(userHandle)), BackStackMode.CLEAR_WHOLE)
        )
    }
}
