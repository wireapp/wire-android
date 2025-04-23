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
package com.wire.android.ui.home

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.framework.TestUser
import com.wire.android.migration.userDatabase.ShouldTriggerMigrationForUserUserCase
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.legalhold.LegalHoldStateForSelfUser
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.personaltoteamaccount.CanMigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class HomeViewModelTest {
    @Test
    fun `given legal hold request pending, then shouldDisplayLegalHoldIndicator is true`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withLegalHoldStatus(flowOf(LegalHoldStateForSelfUser.PendingRequest))
                .arrange()
            // then
            assertEquals(true, viewModel.homeState.shouldDisplayLegalHoldIndicator)
        }

    @Test
    fun `given legal hold enabled, then shouldDisplayLegalHoldIndicator is true`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withLegalHoldStatus(flowOf(LegalHoldStateForSelfUser.Enabled))
            .arrange()
        // then
        assertEquals(true, viewModel.homeState.shouldDisplayLegalHoldIndicator)
    }

    @Test
    fun `given legal hold disabled and no request available, then shouldDisplayLegalHoldIndicator is false`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withLegalHoldStatus(flowOf(LegalHoldStateForSelfUser.Disabled))
                .arrange()
            // then
            assertEquals(false, viewModel.homeState.shouldDisplayLegalHoldIndicator)
        }

    @Test
    fun `given legal hold enabled, when user status changes, then shouldDisplayLegalHoldIndicator should keep the same`() =
        runTest {
            // given
            val selfFlow =
                MutableStateFlow(TestUser.SELF_USER.copy(availabilityStatus = UserAvailabilityStatus.AVAILABLE))
            val (_, viewModel) = Arrangement()
                .withLegalHoldStatus(flowOf(LegalHoldStateForSelfUser.Enabled))
                .withSelfUser(selfFlow)
                .arrange()
            // when
            selfFlow.emit(TestUser.SELF_USER.copy(availabilityStatus = UserAvailabilityStatus.AWAY))
            // then
            assertEquals(true, viewModel.homeState.shouldDisplayLegalHoldIndicator)
        }

    @Test
    fun `given migration not completed, when checking requirements, then return HomeRequirement Migration`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withShouldTriggerMigrationForUserReturning(true)
            .arrange()
        viewModel.actions.test {
            // when
            viewModel.checkRequirements()
            advanceUntilIdle()
            // then
            assertEquals(HomeRequirement.Migration(TestUser.SELF_USER.id), expectMostRecentItem())
        }
    }

    @Test
    fun `given client not registered, when checking requirements, then return HomeRequirement RegisterDevice`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withShouldTriggerMigrationForUserReturning(false)
            .withNeedsToRegisterClientReturning(true)
            .arrange()
        viewModel.actions.test {
            // when
            viewModel.checkRequirements()
            // then
            assertEquals(HomeRequirement.RegisterDevice, expectMostRecentItem())
        }
    }

    @Test
    fun `given initial sync not completed, when checking requirements, then return HomeRequirement InitialSync`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withShouldTriggerMigrationForUserReturning(false)
            .withNeedsToRegisterClientReturning(false)
            .withInitialSyncCompletedReturning(flowOf(false))
            .arrange()
        viewModel.actions.test {
            // when
            viewModel.checkRequirements()
            // then
            assertEquals(HomeRequirement.InitialSync, expectMostRecentItem())
        }
    }

    @Test
    fun `given handle not set, when checking requirements, then return HomeRequirement CreateAccountUsername`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withShouldTriggerMigrationForUserReturning(false)
            .withNeedsToRegisterClientReturning(false)
            .withInitialSyncCompletedReturning(flowOf(true))
            .withSelfUser(flowOf(TestUser.SELF_USER.copy(handle = null)))
            .arrange()
        viewModel.actions.test {
            // when
            viewModel.checkRequirements()
            // then
            assertEquals(HomeRequirement.CreateAccountUsername, expectMostRecentItem())
        }
    }

    @Test
    fun `given migration completed and welcome not yet shown, when checking requirements, then show welcome message`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withShouldTriggerMigrationForUserReturning(false)
            .withNeedsToRegisterClientReturning(false)
            .withInitialSyncCompletedReturning(flowOf(true))
            .withSelfUser(flowOf(TestUser.SELF_USER.copy(handle = "handle")))
            .withMigrationCompletedReturning(true)
            .withWelcomeScreenPresentedReturning(false)
            .arrange()
        // when
        viewModel.checkRequirements()
        // then
        assertEquals(true, viewModel.homeState.shouldDisplayWelcomeMessage)
    }

    @Test
    fun `given migration completed and welcome already shown, when checking requirements, then do not show welcome message`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withShouldTriggerMigrationForUserReturning(false)
            .withNeedsToRegisterClientReturning(false)
            .withInitialSyncCompletedReturning(flowOf(true))
            .withSelfUser(flowOf(TestUser.SELF_USER.copy(handle = "handle")))
            .withMigrationCompletedReturning(true)
            .withWelcomeScreenPresentedReturning(true)
            .arrange()
        // when
        viewModel.checkRequirements()
        // then
        assertEquals(false, viewModel.homeState.shouldDisplayWelcomeMessage)
    }

    internal class Arrangement {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK
        lateinit var dataStore: UserDataStore

        @MockK
        lateinit var observeSelfUser: ObserveSelfUserUseCase

        @MockK
        lateinit var needsToRegisterClient: NeedsToRegisterClientUseCase

        @MockK
        lateinit var observeLegalHoldStatusForSelfUser: ObserveLegalHoldStateForSelfUserUseCase

        @MockK
        lateinit var shouldTriggerMigrationForUser: ShouldTriggerMigrationForUserUserCase

        @MockK
        lateinit var canMigrateFromPersonalToTeam: CanMigrateFromPersonalToTeamUseCase

        private val viewModel by lazy {
            HomeViewModel(
                savedStateHandle = savedStateHandle,
                globalDataStore = globalDataStore,
                dataStore = dataStore,
                observeSelf = observeSelfUser,
                needsToRegisterClient = needsToRegisterClient,
                observeLegalHoldStatusForSelfUser = observeLegalHoldStatusForSelfUser,
                shouldTriggerMigrationForUser = shouldTriggerMigrationForUser,
                canMigrateFromPersonalToTeam = canMigrateFromPersonalToTeam,
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            withSelfUser(flowOf(TestUser.SELF_USER))
            withCanMigrateFromPersonalToTeamReturning(true)
            withLegalHoldStatus(flowOf(LegalHoldStateForSelfUser.Disabled))
        }

        fun withSelfUser(result: Flow<SelfUser>) = apply {
            coEvery { observeSelfUser.invoke() } returns result
        }

        private fun withCanMigrateFromPersonalToTeamReturning(result: Boolean) = apply {
            coEvery { canMigrateFromPersonalToTeam.invoke() } returns result
            coEvery { dataStore.isCreateTeamNoticeRead() } returns flowOf(false)
        }

        fun withLegalHoldStatus(result: Flow<LegalHoldStateForSelfUser>) = apply {
            coEvery { observeLegalHoldStatusForSelfUser.invoke() } returns result
        }

        fun withShouldTriggerMigrationForUserReturning(result: Boolean) = apply {
            coEvery { shouldTriggerMigrationForUser(any()) } returns result
        }

        fun withNeedsToRegisterClientReturning(result: Boolean) = apply {
            coEvery { needsToRegisterClient() } returns result
        }

        fun withInitialSyncCompletedReturning(result: Flow<Boolean>) = apply {
            coEvery { dataStore.initialSyncCompleted } returns result
        }

        fun withMigrationCompletedReturning(result: Boolean) = apply {
            coEvery { globalDataStore.isMigrationCompleted() } returns result
        }

        fun withWelcomeScreenPresentedReturning(result: Boolean) = apply {
            coEvery { globalDataStore.isWelcomeScreenPresented() } returns result
        }

        fun arrange() = this to viewModel
    }
}
