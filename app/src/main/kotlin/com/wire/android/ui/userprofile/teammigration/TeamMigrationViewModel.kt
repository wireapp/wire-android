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

    fun setCurrentStep(step: Int) {
        sendPersonalTeamCreationFlowStepEvent(step)
    }

    fun migrateFromPersonalToTeamAccount() {
        viewModelScope.launch {
            teamMigrationState = teamMigrationState.copy(isMigrating = true)
            migrateFromPersonalToTeam.invoke(
                teamMigrationState.teamNameTextState.text.trim().toString(),
            ).let { result ->
                teamMigrationState = when (result) {
                    is MigrateFromPersonalToTeamResult.Success ->
                        teamMigrationState.copy(isMigrating = false, migrationCompleted = true)

                    is MigrateFromPersonalToTeamResult.Error ->
                        teamMigrationState.copy(isMigrating = false, migrationFailure = result.failure)
                }
            }
        }
    }

    fun failureHandled() {
        teamMigrationState = teamMigrationState.copy(migrationFailure = null)
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
            TEAM_MIGRATION_TEAM_PLAN_STEP -> AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowTeamPlan(
                isMigrationDotActive = teamMigrationState.isMigrationDotActive
            )

            TEAM_MIGRATION_TEAM_NAME_STEP -> AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowTeamName
            TEAM_MIGRATION_CONFIRMATION_STEP -> AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowConfirm
            TEAM_MIGRATION_DONE_STEP -> AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowCompleted
            else -> null
        }
        event?.let { anonymousAnalyticsManager.sendEvent(event) }
    }

    companion object {
        const val TEAM_MIGRATION_TEAM_PLAN_STEP = 1
        const val TEAM_MIGRATION_TEAM_NAME_STEP = 2
        const val TEAM_MIGRATION_CONFIRMATION_STEP = 3
        const val TEAM_MIGRATION_DONE_STEP = 4
    }
}
