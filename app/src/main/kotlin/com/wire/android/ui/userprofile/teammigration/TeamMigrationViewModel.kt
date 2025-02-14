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
package com.wire.android.ui.userprofile.teammigration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStore
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.kalium.logic.feature.server.GetTeamUrlUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamFailure
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamResult
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamMigrationViewModel @Inject constructor(
    private val anonymousAnalyticsManager: AnonymousAnalyticsManager,
    private val migrateFromPersonalToTeam: MigrateFromPersonalToTeamUseCase,
    private val observeSelfUser: ObserveSelfUserUseCase,
    private val dataStore: UserDataStore,
    private val getTeamUrl: GetTeamUrlUseCase
) : ViewModel() {

    var teamMigrationState by mutableStateOf(TeamMigrationState())
        private set

    init {
        setUsername()
        setTeamUrl()
        observeMigrationDotActive()
    }

    fun showMigrationLeaveDialog() {
        teamMigrationState = teamMigrationState.copy(shouldShowMigrationLeaveDialog = true)
    }

    fun hideMigrationLeaveDialog() {
        teamMigrationState = teamMigrationState.copy(shouldShowMigrationLeaveDialog = false)
    }

    fun setCurrentStep(step: Int) {
        teamMigrationState = teamMigrationState.copy(currentStep = step)
        sendPersonalTeamCreationFlowStepEvent(step)
    }

    fun setIsMigratingState(isMigrating: Boolean) {
        teamMigrationState = teamMigrationState.copy(isMigrating = isMigrating)
    }

    fun migrateFromPersonalToTeamAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            migrateFromPersonalToTeam.invoke(
                teamMigrationState.teamNameTextState.text.trim().toString(),
            ).let { result ->
                when (result) {
                    is MigrateFromPersonalToTeamResult.Success -> {
                        onSuccess()
                    }

                    is MigrateFromPersonalToTeamResult.Error -> {
                        onMigrationFailure(result.failure)
                    }
                }
            }
        }
    }

    fun failureHandled() {
        teamMigrationState = teamMigrationState.copy(migrationFailure = null)
    }

    private fun onMigrationFailure(failure: MigrateFromPersonalToTeamFailure) {
        teamMigrationState = teamMigrationState.copy(migrationFailure = failure)
    }

    private fun setUsername() {
        viewModelScope.launch {
            observeSelfUser().collect { selfUser ->
                selfUser.name?.let {
                    teamMigrationState = teamMigrationState.copy(username = it)
                }
            }
        }
    }

    private fun setTeamUrl() {
        viewModelScope.launch {
            teamMigrationState = teamMigrationState.copy(teamUrl = getTeamUrl())
        }
    }

    private fun observeMigrationDotActive() {
        viewModelScope.launch {
            dataStore.isCreateTeamNoticeRead().collect { isRead ->
                teamMigrationState = teamMigrationState.copy(
                    isMigrationDotActive = !isRead
                )
            }
        }
    }

    private fun sendPersonalTeamCreationFlowStepEvent(step: Int) {
        val event = when (step) {
            1 -> AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowTeamPlan(
                isMigrationDotActive = teamMigrationState.isMigrationDotActive
            )

            2 -> AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowTeamName
            3 -> AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowConfirm
            4 -> AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowCompleted
            else -> null
        }
        event?.let { anonymousAnalyticsManager.sendEvent(event) }
    }
}
