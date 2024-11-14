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
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.feature.team.migration.MigrateFromPersonalToTeamResult
import com.wire.kalium.logic.feature.team.migration.MigrateFromPersonalToTeamUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamMigrationViewModel @Inject constructor(
    private val anonymousAnalyticsManager: AnonymousAnalyticsManager,
    private val teamMigrationUseCase: MigrateFromPersonalToTeamUseCase
) : ViewModel() {

    var teamMigrationState by mutableStateOf(TeamMigrationState())
        private set

    fun showMigrationLeaveDialog() {
        teamMigrationState = teamMigrationState.copy(shouldShowMigrationLeaveDialog = true)
    }

    fun hideMigrationLeaveDialog() {
        teamMigrationState = teamMigrationState.copy(shouldShowMigrationLeaveDialog = false)
    }

    fun sendPersonalToTeamMigrationDismissed() {
        anonymousAnalyticsManager.sendEvent(
            AnalyticsEvent.PersonalTeamMigration.ClickedPersonalTeamMigrationCta(
                dismissCreateTeamButtonClicked = true
            )
        )
    }

    fun sendPersonalTeamCreationFlowStartedEvent(step: Int) {
        anonymousAnalyticsManager.sendEvent(
            AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowStarted(step)
        )
    }

    fun sendPersonalTeamCreationFlowCanceledEvent(
        modalLeaveClicked: Boolean? = null,
        modalContinueClicked: Boolean? = null
    ) {
        anonymousAnalyticsManager.sendEvent(
            AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowCanceled(
                teamName = teamMigrationState.teamNameTextState.text.toString(),
                modalLeaveClicked = modalLeaveClicked,
                modalContinueClicked = modalContinueClicked
            )
        )
    }

    fun sendPersonalTeamCreationFlowCompletedEvent(
        modalOpenTeamManagementButtonClicked: Boolean? = null,
        backToWireButtonClicked: Boolean? = null
    ) {
        anonymousAnalyticsManager.sendEvent(
            AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowCompleted(
                teamName = teamMigrationState.teamNameTextState.text.toString(),
                modalOpenTeamManagementButtonClicked = modalOpenTeamManagementButtonClicked,
                backToWireButtonClicked = backToWireButtonClicked
            )
        )
    }

    fun postTeamMigration(
        onSuccess: (teamId: String, teamName: String) -> Unit,
    ) {
        viewModelScope.launch {
            teamMigrationUseCase.invoke(
                teamMigrationState.teamNameTextState.text.toString(),
            ).let { result ->
                when (result) {
                    is MigrateFromPersonalToTeamResult.Success -> {
                        onSuccess(result.teamId, result.teamName)
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

    private fun onMigrationFailure(failure: CoreFailure) {
        teamMigrationState = teamMigrationState.copy(migrationFailure = failure)
    }
}
