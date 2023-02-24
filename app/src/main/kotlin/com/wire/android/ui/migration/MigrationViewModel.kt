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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.wire.android.migration.MigrationData
import com.wire.android.migration.MigrationManager
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.workmanager.worker.enqueueMigrationWorker
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
    private val dispatchers: DispatcherProvider,
    private val migrationManager: MigrationManager
) : ViewModel() {

    var state: MigrationState by mutableStateOf(MigrationState.InProgress(MigrationData.Progress.Type.UNKNOWN))
        private set

    init {
        viewModelScope.launch(dispatchers.io()) {
            enqueueMigrationAndListenForStateChanges()
        }
    }

    fun retry() {
        viewModelScope.launch(dispatchers.io()) {
            // Flow collected in `enqueueMigrationAndListenForStateChanges` will still get updates for the newly enqueued work
            workManager.enqueueMigrationWorker()
        }
        state = MigrationState.InProgress(MigrationData.Progress.Type.UNKNOWN)
    }

    fun accountLogin(userHandle: String?) {
        viewModelScope.launch {
            migrationManager.dismissMigrationFailureNotification()
            navigateToLogin(userHandle)
        }
    }

    fun finish() {
        viewModelScope.launch {
            migrationManager.dismissMigrationFailureNotification()
            navigateAfterMigration()
        }
    }

    private suspend fun enqueueMigrationAndListenForStateChanges() {
        workManager.enqueueMigrationWorker().collect {
            when (it) {
                is MigrationData.Result.Success -> navigateAfterMigration()
                is MigrationData.Progress -> state = MigrationState.InProgress(it.type)
                is MigrationData.Result.Failure -> state = when (it) {
                    MigrationData.Result.Failure.Account.Any -> MigrationState.Failed.Account.Any
                    is MigrationData.Result.Failure.Account.Specific -> MigrationState.Failed.Account.Specific(it.userName, it.userHandle)
                    is MigrationData.Result.Failure.Messages -> MigrationState.Failed.Messages(it.errorCode)
                    MigrationData.Result.Failure.NoNetwork -> MigrationState.Failed.NoNetwork
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

    private suspend fun navigateToLogin(userHandle: String?) {
        // TODO: pass user handle to be pre-filled on the email login screen
        navigationManager.navigate(NavigationCommand(NavigationItem.Login.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
    }
}
