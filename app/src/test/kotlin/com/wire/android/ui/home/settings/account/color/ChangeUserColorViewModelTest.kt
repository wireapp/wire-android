/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.settings.account.color

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.framework.TestUser
import com.wire.android.ui.theme.Accent
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.feature.client.IsChatBubblesEnabledUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UpdateAccentColorResult
import com.wire.kalium.logic.feature.user.UpdateAccentColorUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class ChangeUserColorViewModelTest {

    @Test
    fun `when ViewModel initializes, then accentState reflects self user's accent`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        advanceUntilIdle()

        val expectedAccent = Accent.fromAccentId(TestUser.SELF_USER.accentId)
        assertEquals(expectedAccent, viewModel.accentState.accent)
        assertEquals(false, viewModel.accentState.isPerformingAction)
    }

    @Test
    fun `when changeAccentColor is called, then accent in state updates`() = runTest {
        val (_, viewModel) = Arrangement().arrange()
        advanceUntilIdle()

        val newAccent = Accent.fromAccentId(Accent.Blue.ordinal)
        viewModel.changeAccentColor(newAccent)

        assertEquals(newAccent, viewModel.accentState.accent)
    }

    @Test
    fun `given valid accent, when saveAccentColor succeeds, then action is Success`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUpdateAccentResult(UpdateAccentColorResult.Success)
            .arrange()
        advanceUntilIdle()

        val newAccent = Accent.fromAccentId(Accent.Red.ordinal)
        viewModel.changeAccentColor(newAccent)

        viewModel.saveAccentColor()
        advanceUntilIdle()

        assertEquals(false, viewModel.accentState.isPerformingAction)
        assertEquals(ChangeUserColorAction.Success, viewModel.actions.first())
    }

    @Test
    fun `given valid accent, when saveAccentColor fails, then action is Failure`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUpdateAccentResult(UpdateAccentColorResult.Failure(StorageFailure.DataNotFound))
            .arrange()
        advanceUntilIdle()

        val newAccent = Accent.fromAccentId(Accent.Amber.ordinal)
        viewModel.changeAccentColor(newAccent)

        viewModel.saveAccentColor()
        advanceUntilIdle()

        assertEquals(false, viewModel.accentState.isPerformingAction)
        assertEquals(ChangeUserColorAction.Failure, viewModel.actions.first())
    }

    @Test
    fun `given valid accent, when saving, then use case called with accentId only`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withUpdateAccentResult(UpdateAccentColorResult.Success)
            .arrange()
        advanceUntilIdle()

        val newAccent = Accent.fromAccentId(Accent.Petrol.ordinal)
        viewModel.changeAccentColor(newAccent)

        viewModel.saveAccentColor()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.updateAccentColor(newAccent.accentId) }
    }

    private class Arrangement {

        @MockK
        lateinit var getSelfUserUseCase: GetSelfUserUseCase

        @MockK
        lateinit var updateAccentColor: UpdateAccentColorUseCase

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK
        lateinit var isChatBubblesEnabled: IsChatBubblesEnabledUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { getSelfUserUseCase() } returns TestUser.SELF_USER
            coEvery { updateAccentColor(any()) } returns UpdateAccentColorResult.Success
            coEvery { globalDataStore.observeIsBubbleUI() }.returns(flowOf(true))
            coEvery { isChatBubblesEnabled() } returns false
        }

        fun withUpdateAccentResult(result: UpdateAccentColorResult) = apply {
            coEvery { updateAccentColor(any()) } returns result
        }

        fun arrange() = this to ChangeUserColorViewModel(
            getSelf = getSelfUserUseCase,
            updateAccentColor = updateAccentColor,
            isChatBubblesEnabled = isChatBubblesEnabled,
            globalDataStore
        )
    }
}
