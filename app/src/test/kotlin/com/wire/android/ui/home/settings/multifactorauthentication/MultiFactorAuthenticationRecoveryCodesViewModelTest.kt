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
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MultiFactorAuthenticationRecoveryCodesViewModelTest {
    private val owner = ViewModelStore()

    @AfterEach
    fun tearDown() {
        owner.clear()
    }

    @Test
    fun `password and explicit confirmation are both required before generation`() = runTest {
        val generate = mockk<GenerateMultiFactorAuthenticationRecoveryCodesUseCase>()
        val viewModel = createViewModel(generate = generate)
        viewModel.requestGeneration()
        viewModel.generateNewCodes()
        assertFalse(viewModel.state.value.showGenerationConfirmation)
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        assertTrue(viewModel.state.value.isConfirmingPassword)
        assertTrue(viewModel.password.text.isEmpty())
        advanceUntilIdle()
        viewModel.navigationHandled()

        viewModel.generateNewCodes()
        viewModel.requestGeneration()
        assertTrue(viewModel.state.value.showGenerationConfirmation)
        viewModel.dismissGenerationConfirmation()

        assertTrue(viewModel.state.value.passwordConfirmed)
        assertFalse(viewModel.state.value.showGenerationConfirmation)
        assertTrue(viewModel.state.value.codes.isEmpty())
        coVerify(exactly = 0) { generate() }
    }

    @Test
    fun `generation blocks duplicate requests and back until the codes are acknowledged`() = runTest {
        val generate = mockk<GenerateMultiFactorAuthenticationRecoveryCodesUseCase>()
        val codes = listOf("aaaaa-bbbbb-ccccc", "ddddd-eeeee-fffff")
        coEvery { generate() } coAnswers {
            delay(500)
            codes
        }
        val viewModel = createViewModel(generate = generate)
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        advanceUntilIdle()
        viewModel.navigationHandled()
        viewModel.requestGeneration()

        viewModel.generateNewCodes()
        viewModel.generateNewCodes()
        assertFalse(viewModel.state.value.canNavigateBack)
        viewModel.dismissGenerationConfirmation()
        assertTrue(viewModel.state.value.showGenerationConfirmation)
        advanceUntilIdle()

        assertEquals(codes, viewModel.state.value.codes)
        assertEquals(MultiFactorAuthenticationRecoveryCodesStep.NEW_CODES, viewModel.state.value.nextStep)
        assertFalse(viewModel.state.value.canNavigateBack)
        viewModel.cancelPendingOperation()
        assertEquals(codes, viewModel.state.value.codes)
        viewModel.setCodesSaved(true)
        assertTrue(viewModel.state.value.canNavigateBack)
        viewModel.setCodesSaved(false)
        assertFalse(viewModel.state.value.canNavigateBack)
        viewModel.generateNewCodes()
        coVerify(exactly = 1) { generate() }
    }

    @Test
    fun `a generation error keeps the flow retryable without claiming that codes were changed`() = runTest {
        val generate = mockk<GenerateMultiFactorAuthenticationRecoveryCodesUseCase>()
        coEvery { generate() } throws IOException("Offline")
        val viewModel = createViewModel(generate = generate)
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        advanceUntilIdle()
        viewModel.navigationHandled()
        viewModel.requestGeneration()
        viewModel.generateNewCodes()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.hasError)
        assertTrue(viewModel.state.value.codes.isEmpty())
        assertNull(viewModel.state.value.nextStep)
        assertTrue(viewModel.state.value.canNavigateBack)
        coEvery { generate() } returns listOf("new-code")
        viewModel.requestGeneration()
        viewModel.generateNewCodes()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.hasError)
        assertEquals(listOf("new-code"), viewModel.state.value.codes)
    }

    @Test
    fun `password failures can be retried and cancelling confirmation never advances later`() = runTest {
        val confirm = mockk<ConfirmMultiFactorAuthenticationPasswordUseCase>()
        coEvery { confirm(any()) } throws IOException("Offline")
        val viewModel = createViewModel(confirm = confirm)
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.hasError)
        assertFalse(viewModel.state.value.passwordConfirmed)

        coEvery { confirm(any()) } coAnswers { delay(500) }
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        viewModel.cancelPendingOperation()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.hasError)
        assertFalse(viewModel.state.value.passwordConfirmed)
        assertNull(viewModel.state.value.nextStep)
    }

    @Test
    fun `each dummy generation creates a new set and disposing the flow clears the displayed codes`() = runTest {
        val generate = GenerateMultiFactorAuthenticationRecoveryCodesUseCase()
        val first = generate()
        val second = generate()
        assertEquals(10, first.size)
        assertEquals(10, first.toSet().size)
        assertNotEquals(first, second)

        val viewModel = createViewModel()
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        advanceUntilIdle()
        viewModel.requestGeneration()
        viewModel.generateNewCodes()
        advanceUntilIdle()
        owner.clear()

        assertEquals(MultiFactorAuthenticationRecoveryCodesState(), viewModel.state.value)
        assertTrue(viewModel.password.text.isEmpty())
    }

    private fun createViewModel(
        confirm: ConfirmMultiFactorAuthenticationPasswordUseCase = ConfirmMultiFactorAuthenticationPasswordUseCase(),
        generate: GenerateMultiFactorAuthenticationRecoveryCodesUseCase =
            GenerateMultiFactorAuthenticationRecoveryCodesUseCase(),
    ): MultiFactorAuthenticationRecoveryCodesViewModel = ViewModelProvider.create(
        owner,
        viewModelFactory { initializer { MultiFactorAuthenticationRecoveryCodesViewModel(confirm, generate) } },
    )[MultiFactorAuthenticationRecoveryCodesViewModel::class]
}
