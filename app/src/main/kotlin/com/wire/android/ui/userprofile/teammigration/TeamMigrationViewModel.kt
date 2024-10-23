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
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TeamMigrationViewModel @Inject constructor(
    private val analyticsManager: AnonymousAnalyticsManager
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
        analyticsManager.sendEvent(
            AnalyticsEvent.PersonalTeamMigration.ClickedPersonalTeamMigrationCta(
                dismissCreateTeamButtonClicked = true
            )
        )
    }

    fun sendPersonalTeamCreationFlowStartedEvent(step: Int) {
        analyticsManager.sendEvent(
            AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowStarted(step)
        )
    }

    fun sendPersonalTeamCreationFlowCanceledEvent(
        modalLeaveClicked: Boolean = false,
        modalContinueClicked: Boolean = false
    ) {
        analyticsManager.sendEvent(
            AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowCanceled(
                teamName = teamMigrationState.teamNameTextState.text.toString(),
                modalLeaveClicked = modalLeaveClicked,
                modalContinueClicked = modalContinueClicked
            )
        )
    }

    fun sendPersonalTeamCreationFlowCompletedEvent(
        modalOpenTeamManagementButtonClicked: Boolean = false,
        backToWireButtonClicked: Boolean = false
    ) {
        analyticsManager.sendEvent(
            AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowCompleted(
                teamName = teamMigrationState.teamNameTextState.text.toString(),
                modalOpenTeamManagementButtonClicked = modalOpenTeamManagementButtonClicked,
                backToWireButtonClicked = backToWireButtonClicked
            )
        )
    }
}
