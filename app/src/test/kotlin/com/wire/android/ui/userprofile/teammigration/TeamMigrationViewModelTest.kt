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

import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test

class TeamMigrationViewModelTest {

    @Test
    fun `given dialog state, when showMigrationLeaveDialog is called, then update shouldShowMigrationLeaveDialog to true`() =
        runTest {
            val (_, viewModel) = Arrangement()
                .arrange()

            viewModel.showMigrationLeaveDialog()

            assertEquals(true, viewModel.teamMigrationState.shouldShowMigrationLeaveDialog)
        }

    @Test
    fun `given dialog state, when hideMigrationLeaveDialog is called, then update shouldShowMigrationLeaveDialog to false`() =
        runTest {
            val (_, viewModel) = Arrangement()
                .arrange()

            viewModel.hideMigrationLeaveDialog()

            assertEquals(false, viewModel.teamMigrationState.shouldShowMigrationLeaveDialog)
        }

    @Test
    fun `given close modal event, when sendPersonalToTeamMigrationDismissed is called, then send the event`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .arrange()

            viewModel.sendPersonalToTeamMigrationDismissed()

            verify(exactly = 1) {
                arrangement.anonymousAnalyticsManager.sendEvent(
                    AnalyticsEvent.PersonalTeamMigration.ClickedPersonalTeamMigrationCta(
                        dismissCreateTeamButtonClicked = true
                    )
                )
            }
        }

    @Test
    fun `given the step of migration flow, when sendPersonalTeamCreationFlowStartedEvent is called, then send the event`() =
        runTest {
            val step = 2
            val (arrangement, viewModel) = Arrangement()
                .arrange()

            viewModel.sendPersonalTeamCreationFlowStartedEvent(2)

            verify(exactly = 1) {
                arrangement.anonymousAnalyticsManager.sendEvent(
                    AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowStarted(step)
                )
            }
        }

    @Test
    fun `given modalLeaveClicked event, when sendPersonalTeamCreationFlowCanceledEvent is called, then send the event`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .arrange()

            viewModel.sendPersonalTeamCreationFlowCanceledEvent(modalLeaveClicked = true)

            verify(exactly = 1) {
                arrangement.anonymousAnalyticsManager.sendEvent(
                    AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowCanceled(
                        teamName = viewModel.teamMigrationState.teamNameTextState.text.toString(),
                        modalLeaveClicked = true
                    )
                )
            }
        }

    @Test
    fun `given modalContinueClicked event, when sendPersonalTeamCreationFlowCanceledEvent is called, then send the event`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .arrange()

            viewModel.sendPersonalTeamCreationFlowCanceledEvent(modalContinueClicked = true)

            verify(exactly = 1) {
                arrangement.anonymousAnalyticsManager.sendEvent(
                    AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowCanceled(
                        teamName = viewModel.teamMigrationState.teamNameTextState.text.toString(),
                        modalContinueClicked = true
                    )
                )
            }
        }

    @Test
    fun `given modalOpenTeamManagementButtonClicked event, when sendPersonalTeamCreationFlowCompletedEvent is called, then send the event`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .arrange()

            viewModel.sendPersonalTeamCreationFlowCompletedEvent(
                modalOpenTeamManagementButtonClicked = true
            )

            verify(exactly = 1) {
                arrangement.anonymousAnalyticsManager.sendEvent(
                    AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowCompleted(
                        teamName = viewModel.teamMigrationState.teamNameTextState.text.toString(),
                        modalOpenTeamManagementButtonClicked = true
                    )
                )
            }
        }

    @Test
    fun `given backToWireButtonClicked event, when sendPersonalTeamCreationFlowCompletedEvent is called, then send the event`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .arrange()

            viewModel.sendPersonalTeamCreationFlowCompletedEvent(backToWireButtonClicked = true)

            verify(exactly = 1) {
                arrangement.anonymousAnalyticsManager.sendEvent(
                    AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowCompleted(
                        teamName = viewModel.teamMigrationState.teamNameTextState.text.toString(),
                        backToWireButtonClicked = true
                    )
                )
            }
        }

    private class Arrangement {

        @MockK
        lateinit var anonymousAnalyticsManager: AnonymousAnalyticsManager

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun arrange() = this to TeamMigrationViewModel(
            anonymousAnalyticsManager = anonymousAnalyticsManager
        )
    }
}
