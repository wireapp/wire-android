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

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.UserDataStore
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.feature.server.GetTeamUrlUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamFailure
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamResult
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class TeamMigrationViewModelTest {

    @Test
    fun `given the step of migration flow, when setCurrentStep is called, then send the event`() =
        runTest {
            val step = 2
            val (arrangement, viewModel) = Arrangement()
                .arrange()

            viewModel.setCurrentStep(step)

            verify(exactly = 1) {
                arrangement.anonymousAnalyticsManager.sendEvent(
                    AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowTeamName
                )
            }
        }

    @Test
    fun `given team name, when migrateFromPersonalToTeamAccount return success, then call use case and set state to completed`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withMigrateFromPersonalToTeamSuccess()
                .arrange()

            viewModel.migrateFromPersonalToTeamAccount()

            coVerify(exactly = 1) {
                arrangement.migrateFromPersonalToTeam(Arrangement.TEAM_NAME)
            }
            assertEquals(true, viewModel.teamMigrationState.migrationCompleted)
        }

    @Test
    fun `given team name, when migrateFromPersonalToTeamAccount return unknown failure, then call use case and handle the failure`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withMigrateFromPersonalToTeamErrorUnknown()
                .arrange()

            viewModel.migrateFromPersonalToTeamAccount()

            coVerify(exactly = 1) {
                arrangement.migrateFromPersonalToTeam(Arrangement.TEAM_NAME)
            }
            Assertions.assertNotNull(viewModel.teamMigrationState.migrationFailure)
            viewModel.failureHandled()
            Assertions.assertNull(viewModel.teamMigrationState.migrationFailure)
        }

    @Test
    fun `given team name, when migrateFromPersonalToTeamAccount return user already in team failure, then call use case and handle the failure`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withMigrateFromPersonalToTeamErrorAlreadyInTeam()
                .arrange()

            viewModel.migrateFromPersonalToTeamAccount()

            coVerify(exactly = 1) {
                arrangement.migrateFromPersonalToTeam(Arrangement.TEAM_NAME)
            }
            Assertions.assertNotNull(viewModel.teamMigrationState.migrationFailure)
            viewModel.failureHandled()
            Assertions.assertNull(viewModel.teamMigrationState.migrationFailure)
        }

    @Test
    fun `given team name, when migrateFromPersonalToTeamAccount return no network failure, then call use case and handle the failure`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withMigrateFromPersonalToTeamErrorNoNetwork()
                .arrange()

            viewModel.migrateFromPersonalToTeamAccount()

            coVerify(exactly = 1) {
                arrangement.migrateFromPersonalToTeam(Arrangement.TEAM_NAME)
            }
            Assertions.assertNotNull(viewModel.teamMigrationState.migrationFailure)
            viewModel.failureHandled()
            Assertions.assertNull(viewModel.teamMigrationState.migrationFailure)
        }

    @Test
    fun `given team name with spaces at start or end, when invoking migration, then trim the name`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withMigrateFromPersonalToTeamSuccess()
            .arrange()
        viewModel.teamMigrationState.teamNameTextState.setTextAndPlaceCursorAtEnd(" ${Arrangement.TEAM_NAME} ")
        // when
        viewModel.migrateFromPersonalToTeamAccount()
        // then
        coVerify(exactly = 1) {
            arrangement.migrateFromPersonalToTeam(Arrangement.TEAM_NAME)
        }
    }

    private class Arrangement {

        @MockK
        lateinit var anonymousAnalyticsManager: AnonymousAnalyticsManager

        @MockK
        lateinit var migrateFromPersonalToTeam: MigrateFromPersonalToTeamUseCase

        @MockK
        lateinit var getSelfUser: ObserveSelfUserUseCase

        @MockK
        lateinit var getTeamUrl: GetTeamUrlUseCase

        @MockK
        lateinit var dataStore: UserDataStore

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { getSelfUser() } returns flowOf()
            coEvery { getTeamUrl() } returns "TeamUrl"
            coEvery { dataStore.isCreateTeamNoticeRead() } returns flowOf(false)
        }

        fun arrange() = this to TeamMigrationViewModel(
            anonymousAnalyticsManager = anonymousAnalyticsManager,
            migrateFromPersonalToTeam = migrateFromPersonalToTeam,
            observeSelfUser = getSelfUser,
            dataStore = dataStore,
            getTeamUrl = getTeamUrl
        ).also { viewModel ->
            viewModel.teamMigrationState.teamNameTextState.setTextAndPlaceCursorAtEnd(TEAM_NAME)
        }

        fun withMigrateFromPersonalToTeamSuccess() = apply {
            coEvery { migrateFromPersonalToTeam(any()) } returns MigrateFromPersonalToTeamResult.Success
        }

        fun withMigrateFromPersonalToTeamErrorUnknown() = apply {
            coEvery { migrateFromPersonalToTeam(any()) } returns MigrateFromPersonalToTeamResult.Error(
                MigrateFromPersonalToTeamFailure.UnknownError(NetworkFailure.ProxyError(null))
            )
        }

        fun withMigrateFromPersonalToTeamErrorAlreadyInTeam() = apply {
            coEvery { migrateFromPersonalToTeam(any()) } returns MigrateFromPersonalToTeamResult.Error(
                MigrateFromPersonalToTeamFailure.UserAlreadyInTeam()
            )
        }

        fun withMigrateFromPersonalToTeamErrorNoNetwork() = apply {
            coEvery { migrateFromPersonalToTeam(any()) } returns MigrateFromPersonalToTeamResult.Error(
                MigrateFromPersonalToTeamFailure.NoNetwork
            )
        }

        companion object {
            const val TEAM_NAME = "teamName"
        }
    }
}
