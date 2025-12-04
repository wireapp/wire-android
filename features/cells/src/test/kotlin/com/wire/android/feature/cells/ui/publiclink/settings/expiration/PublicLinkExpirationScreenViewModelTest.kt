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
package com.wire.android.feature.cells.ui.publiclink.settings.expiration

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.ui.common.datetime.TimePickerResult
import com.wire.kalium.cells.domain.usecase.publiclink.SetPublicLinkExpirationUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.left
import com.wire.kalium.common.functional.right
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days

class PublicLinkExpirationScreenViewModelTest {

    @BeforeEach
    fun beforeEach() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given link expiration is set when view model is created then correct initial state is emitted`() = runTest {
        val (_, viewModel) = Arrangement().apply {
            expiresAt = Clock.System.now().toEpochMilliseconds() + 1.days.inWholeMilliseconds
        }.arrange()

        viewModel.state.test {
            with(awaitItem()) {
                assertTrue(isEnabled)
                assertTrue(isValidExpirationDate)
            }
        }
    }

    @Test
    fun `given link expiration is not set when view model is created then correct initial state is emitted`() = runTest {
        val (_, viewModel) = Arrangement().apply {
            expiresAt = null
        }.arrange()

        viewModel.state.test {
            with(awaitItem()) {
                assertFalse(isEnabled)
                assertTrue(isValidExpirationDate)
            }
        }
    }

    @Test
    fun `given link has expired when view model is created then correct initial state is emitted`() = runTest {
        val (_, viewModel) = Arrangement().apply {
            expiresAt = Clock.System.now().toEpochMilliseconds() - 1.days.inWholeMilliseconds
        }.arrange()

        viewModel.state.test {
            assertFalse(awaitItem().isValidExpirationDate)
        }
    }

    @Test
    fun `given link expiration not set when enabled then correct state is emitted`() = runTest {
        val (_, viewModel) = Arrangement().apply {
            expiresAt = null
            setExpiration = SetPublicLinkExpirationUseCase { _, _ -> throw IllegalStateException("Must not be called") }
        }.arrange()

        viewModel.state.test {
            assertFalse(awaitItem().isEnabled)
            viewModel.onEnableClick()
            assertTrue(awaitItem().isEnabled)
        }
    }

    @Test
    fun `given link expiration is set when disabled then expiration is removed`() = runTest {
        var removeCalled = false
        val (_, viewModel) = Arrangement().apply {
            expiresAt = System.currentTimeMillis()
            setExpiration = SetPublicLinkExpirationUseCase { _, expiresAt ->
                removeCalled = expiresAt == null
                Unit.right()
            }
        }.arrange()

        viewModel.onEnableClick()

        assertTrue(removeCalled)
    }

    @Test
    fun `given no selection when date selected then correct state is emitted`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        val selectedDate = System.currentTimeMillis() + 100_000

        viewModel.state.test {
            skipItems(1)
            viewModel.onDateSelected(selectedDate)

            with(awaitItem()) {
                assertFalse(isSetButtonEnabled)
                assertTrue(isValidExpirationDate)
            }
        }
    }

    @Test
    fun `given no selection when time selected then correct state is emitted`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        viewModel.state.test {
            skipItems(1)
            viewModel.onTimeSelected(TimePickerResult(10, 10))

            with(awaitItem()) {
                assertFalse(isSetButtonEnabled)
                assertTrue(isValidExpirationDate)
            }
        }
    }

    @Test
    fun `given date selected when time selected then correct state is emitted`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        val selectedDate = Clock.System.now().toEpochMilliseconds() + 1.days.inWholeMilliseconds

        viewModel.state.test {
            skipItems(1)
            viewModel.onDateSelected(selectedDate)
            skipItems(1)
            viewModel.onTimeSelected(TimePickerResult(10, 10))

            with(awaitItem()) {
                assertTrue(isSetButtonEnabled)
                assertTrue(isValidExpirationDate)
            }
        }
    }

    @Test
    fun `given time selected when date selected then correct state is emitted`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        val selectedDate = Clock.System.now().toEpochMilliseconds() + 1.days.inWholeMilliseconds

        viewModel.state.test {
            skipItems(1)
            viewModel.onTimeSelected(TimePickerResult(10, 10))
            skipItems(1)
            viewModel.onDateSelected(selectedDate)

            with(awaitItem()) {
                assertTrue(isSetButtonEnabled)
                assertTrue(isValidExpirationDate)
            }
        }
    }

    @Test
    fun `given view model when date clicked then correct action is emitted`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        val selectedDate = Clock.System.now().toEpochMilliseconds() + 1.days.inWholeMilliseconds
        viewModel.onDateSelected(selectedDate)

        viewModel.actions.test {
            viewModel.onDateClick()
            assertEquals(ShowDatePicker(selectedDate), awaitItem())
        }
    }

    @Test
    fun `given view model when time clicked then correct action is emitted`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        val selectedTime = TimePickerResult(10, 10)
        viewModel.onTimeSelected(selectedTime)

        viewModel.actions.test {
            viewModel.onTimeClick()
            assertEquals(ShowTimePicker(selectedTime), awaitItem())
        }
    }

    @Test
    fun `given set expiration is success when setting expiration then correct action is emitted`() = runTest {
        val selectedDate = Clock.System.now().toEpochMilliseconds() + 1.days.inWholeMilliseconds
        val (_, viewModel) = Arrangement()
            .apply { expiresAt = selectedDate }
            .withSetExpirationSuccess()
            .arrange()

        viewModel.actions.test {
            viewModel.setExpiration()
            val action = awaitItem()
            assertTrue(action is CloseScreen)
            assertTrue((action as CloseScreen).result.isExpirationSet)
        }
    }

    @Test
    fun `given set expiration fails when setting expiration then correct action is emitted`() = runTest {
        val selectedDate = Clock.System.now().toEpochMilliseconds() + 1.days.inWholeMilliseconds
        val (_, viewModel) = Arrangement()
            .apply { expiresAt = selectedDate }
            .withSetExpirationFailure()
            .arrange()

        viewModel.actions.test {
            viewModel.setExpiration()
            val action = awaitItem()
            assertTrue(action is ShowError)
            assertTrue((action as ShowError).error == ExpirationError.SetFailure)
        }
    }

    @Test
    fun `given remove expiration is success when removing expiration then correct state is emitted`() = runTest {
        val selectedDate = Clock.System.now().toEpochMilliseconds() + 1.days.inWholeMilliseconds
        val (_, viewModel) = Arrangement()
            .apply { expiresAt = selectedDate }
            .withSetExpirationSuccess()
            .arrange()

        viewModel.state.test {
            skipItems(1)
            viewModel.removeExpiration()
            with(awaitItem()) {
                assertFalse(viewModel.isExpirationSet)
                assertFalse(isEnabled)
                assertFalse(showProgress)
            }
        }
    }

    @Test
    fun `given remove expiration fails when removing expiration then correct action is emitted`() = runTest {
        val selectedDate = Clock.System.now().toEpochMilliseconds() + 1.days.inWholeMilliseconds
        val (_, viewModel) = Arrangement()
            .apply { expiresAt = selectedDate }
            .withSetExpirationFailure()
            .arrange()

        viewModel.actions.test {
            viewModel.removeExpiration()
            val action = awaitItem()
            assertTrue(action is ShowError)
            assertTrue((action as ShowError).error == ExpirationError.RemoveFailure)
        }
    }

    private class Arrangement {

        var expiresAt: Long? = null
        var setExpiration: SetPublicLinkExpirationUseCase = SetPublicLinkExpirationUseCase { _, _ -> Unit.right() }

        fun withSetExpirationSuccess() = apply {
            setExpiration = SetPublicLinkExpirationUseCase { _, _ -> Unit.right() }
        }

        fun withSetExpirationFailure() = apply {
            setExpiration = SetPublicLinkExpirationUseCase { _, _ -> CoreFailure.Unknown(IllegalStateException()).left() }
        }

        fun arrange(): Pair<Arrangement, PublicLinkExpirationScreenViewModel> {
            return this to PublicLinkExpirationScreenViewModel(
                setExpiration = setExpiration,
                savedStateHandle = SavedStateHandle(
                    mapOf<String, Any?>(
                        "linkUuid" to "public_link_uuid",
                        "expiresAt" to expiresAt
                    )
                )
            )
        }
    }
}
