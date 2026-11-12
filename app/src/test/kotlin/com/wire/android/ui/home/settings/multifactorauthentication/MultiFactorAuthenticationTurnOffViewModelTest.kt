/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
 *
 */

package com.wire.android.ui.home.settings.multifactorauthentication

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wire.android.config.CoroutineTestExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MultiFactorAuthenticationTurnOffViewModelTest {
    private val owner = ViewModelStore()

    @AfterEach
    fun tearDown() {
        owner.clear()
    }

    @Test
    fun `missing password or malformed code never submits the operation`() = runTest {
        val useCase = mockk<TurnOffMultiFactorAuthenticationUseCase>()
        val viewModel = createViewModel(useCase)
        listOf("" to "284913", "   " to "284913", "password" to "123", "password" to "abcdef").forEach { (password, code) ->
            viewModel.password.setTextAndPlaceCursorAtEnd(password)
            viewModel.code.setTextAndPlaceCursorAtEnd(code)
            viewModel.turnOff()
        }

        assertEquals(MultiFactorAuthenticationTurnOffState(), viewModel.state.value)
        coVerify(exactly = 0) { useCase(any(), any()) }
    }

    @Test
    fun `successful submission prevents duplicates and clears credentials`() = runTest {
        val useCase = mockk<TurnOffMultiFactorAuthenticationUseCase>()
        coEvery { useCase(any(), any()) } coAnswers {
            delay(500)
            true
        }
        val viewModel = createViewModel(useCase)
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.turnOff()
        assertTrue(viewModel.state.value.isBusy)
        assertTrue(viewModel.code.text.isEmpty())

        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.turnOff()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isCompleted)
        assertFalse(viewModel.state.value.isBusy)
        assertTrue(viewModel.password.text.isEmpty())
        assertTrue(viewModel.code.text.isEmpty())
        coVerify(exactly = 1) { useCase("password", "284913") }
    }

    @Test
    fun `an invalid code can be retried while retaining the password`() = runTest {
        val viewModel = createViewModel()
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.code.setTextAndPlaceCursorAtEnd("000000")
        viewModel.turnOff()
        advanceUntilIdle()

        assertEquals(MultiFactorAuthenticationTurnOffError.INVALID_CODE, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isCompleted)
        assertEquals("password", viewModel.password.text.toString())

        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.turnOff()
        advanceUntilIdle()

        assertEquals(MultiFactorAuthenticationTurnOffState(isCompleted = true), viewModel.state.value)
    }

    @Test
    fun `an operation failure is retryable and does not report success`() = runTest {
        val useCase = mockk<TurnOffMultiFactorAuthenticationUseCase>()
        coEvery { useCase(any(), any()) } throws IOException("Offline")
        val viewModel = createViewModel(useCase)
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.turnOff()
        advanceUntilIdle()

        assertEquals(MultiFactorAuthenticationTurnOffError.GENERIC, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isCompleted)
        coEvery { useCase(any(), any()) } returns true
        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.turnOff()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isCompleted)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `cancelling an in-flight operation clears input and never completes later`() = runTest {
        val viewModel = createViewModel()
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.turnOff()

        viewModel.cancel()
        advanceUntilIdle()

        assertEquals(MultiFactorAuthenticationTurnOffState(), viewModel.state.value)
        assertTrue(viewModel.password.text.isEmpty())
        assertTrue(viewModel.code.text.isEmpty())
    }

    @Test
    fun `disposing the screen cancels pending work and clears temporary data`() = runTest {
        val viewModel = createViewModel()
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.turnOff()

        owner.clear()
        advanceUntilIdle()

        assertEquals(MultiFactorAuthenticationTurnOffState(), viewModel.state.value)
        assertTrue(viewModel.password.text.isEmpty())
        assertTrue(viewModel.code.text.isEmpty())
    }

    private fun createViewModel(
        useCase: TurnOffMultiFactorAuthenticationUseCase = TurnOffMultiFactorAuthenticationUseCase(),
    ): MultiFactorAuthenticationTurnOffViewModel = ViewModelProvider.create(
        owner,
        viewModelFactory { initializer { MultiFactorAuthenticationTurnOffViewModel(useCase) } },
    )[MultiFactorAuthenticationTurnOffViewModel::class]
}
