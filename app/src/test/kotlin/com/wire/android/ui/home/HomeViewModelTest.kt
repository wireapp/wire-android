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
import com.wire.android.framework.TestUser
import com.wire.android.migration.userDatabase.ShouldTriggerMigrationForUserUserCase
import com.wire.android.ui.legalhold.ObserveLegalHoldStatusForCurrentUserUseCase
import com.wire.android.ui.legalhold.banner.LegalHoldUIState
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
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
    fun `given legal hold request pending, then shouldDisplayLegalHoldIndicator is true`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withLegalHoldStatus(flowOf(LegalHoldUIState.Pending))
            .arrange()
        // then
        assertEquals(true, viewModel.homeState.shouldDisplayLegalHoldIndicator)
    }
    @Test
    fun `given legal hold active, then shouldDisplayLegalHoldIndicator is true`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withLegalHoldStatus(flowOf(LegalHoldUIState.Active))
            .arrange()
        // then
        assertEquals(true, viewModel.homeState.shouldDisplayLegalHoldIndicator)
    }
    @Test
    fun `given legal hold disabled and no request available, then shouldDisplayLegalHoldIndicator is false`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withLegalHoldStatus(flowOf(LegalHoldUIState.None))
            .arrange()
        // then
        assertEquals(false, viewModel.homeState.shouldDisplayLegalHoldIndicator)
    }
    @Test
    fun `given legal hold active, when user status changes, then shouldDisplayLegalHoldIndicator should keep the same`() = runTest {
        // given
        val selfFlow = MutableStateFlow(TestUser.SELF_USER.copy(availabilityStatus = UserAvailabilityStatus.AVAILABLE))
        val (_, viewModel) = Arrangement()
            .withLegalHoldStatus(flowOf(LegalHoldUIState.Active))
            .withGetSelf(selfFlow)
            .arrange()
        // when
        selfFlow.emit(TestUser.SELF_USER.copy(availabilityStatus = UserAvailabilityStatus.AWAY))
        // then
        assertEquals(true, viewModel.homeState.shouldDisplayLegalHoldIndicator)
    }

    internal class Arrangement {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle
        @MockK
        lateinit var globalDataStore: GlobalDataStore
        @MockK
        lateinit var getSelf: GetSelfUserUseCase
        @MockK
        lateinit var needsToRegisterClient: NeedsToRegisterClientUseCase
        @MockK
        lateinit var observeLegalHoldStatusForCurrentUser: ObserveLegalHoldStatusForCurrentUserUseCase
        @MockK
        lateinit var wireSessionImageLoader: WireSessionImageLoader
        @MockK
        lateinit var shouldTriggerMigrationForUser: ShouldTriggerMigrationForUserUserCase

        private val viewModel by lazy {
            HomeViewModel(
                savedStateHandle = savedStateHandle,
                globalDataStore = globalDataStore,
                getSelf = getSelf,
                needsToRegisterClient = needsToRegisterClient,
                observeLegalHoldStatusForCurrentUser = observeLegalHoldStatusForCurrentUser,
                wireSessionImageLoader = wireSessionImageLoader,
                shouldTriggerMigrationForUser = shouldTriggerMigrationForUser
            )
        }
        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            withGetSelf(flowOf(TestUser.SELF_USER))
        }
        fun withGetSelf(result: Flow<SelfUser>) = apply {
            coEvery { getSelf.invoke() } returns result
        }
        fun withLegalHoldStatus(result: Flow<LegalHoldUIState>) = apply {
            coEvery { observeLegalHoldStatusForCurrentUser.invoke() } returns result
        }
        fun arrange() = this to viewModel
    }
}
