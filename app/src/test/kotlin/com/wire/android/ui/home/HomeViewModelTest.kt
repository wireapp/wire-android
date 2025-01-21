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
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
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
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
                .withGetSelf(selfFlow)
                .arrange()
            // when
            selfFlow.emit(TestUser.SELF_USER.copy(availabilityStatus = UserAvailabilityStatus.AWAY))
            // then
            assertEquals(true, viewModel.homeState.shouldDisplayLegalHoldIndicator)
        }

    @Test
    fun `given open profile event, when sendOpenProfileEvent is called, then send the event with the unread indicator value`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withLegalHoldStatus(flowOf(LegalHoldStateForSelfUser.Enabled))
                .arrange()

            viewModel.sendOpenProfileEvent()

            verify(exactly = 1) {
                arrangement.analyticsManager.sendEvent(
                    AnalyticsEvent.UserProfileOpened(
                        isMigrationDotActive = viewModel.homeState.shouldShowCreateTeamUnreadIndicator
                    )
                )
            }
        }

    internal class Arrangement {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK
        lateinit var dataStore: UserDataStore

        @MockK
        lateinit var getSelf: ObserveSelfUserUseCase

        @MockK
        lateinit var needsToRegisterClient: NeedsToRegisterClientUseCase

        @MockK
        lateinit var observeLegalHoldStatusForSelfUser: ObserveLegalHoldStateForSelfUserUseCase

        @MockK
        lateinit var shouldTriggerMigrationForUser: ShouldTriggerMigrationForUserUserCase

        @MockK
        lateinit var analyticsManager: AnonymousAnalyticsManager

        @MockK
        lateinit var canMigrateFromPersonalToTeam: CanMigrateFromPersonalToTeamUseCase

        private val viewModel by lazy {
            HomeViewModel(
                savedStateHandle = savedStateHandle,
                globalDataStore = globalDataStore,
                dataStore = dataStore,
                observeSelf = getSelf,
                needsToRegisterClient = needsToRegisterClient,
                observeLegalHoldStatusForSelfUser = observeLegalHoldStatusForSelfUser,
                shouldTriggerMigrationForUser = shouldTriggerMigrationForUser,
                analyticsManager = analyticsManager,
                canMigrateFromPersonalToTeam = canMigrateFromPersonalToTeam
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            withGetSelf(flowOf(TestUser.SELF_USER))
            withCanMigrateFromPersonalToTeamReturning(true)
        }

        fun withGetSelf(result: Flow<SelfUser>) = apply {
            coEvery { getSelf.invoke() } returns result
        }

        private fun withCanMigrateFromPersonalToTeamReturning(result: Boolean) = apply {
            coEvery { canMigrateFromPersonalToTeam.invoke() } returns result
            coEvery { dataStore.isCreateTeamNoticeRead() } returns flowOf(false)
        }

        fun withLegalHoldStatus(result: Flow<LegalHoldStateForSelfUser>) = apply {
            coEvery { observeLegalHoldStatusForSelfUser.invoke() } returns result
        }

        fun arrange() = this to viewModel
    }
}
