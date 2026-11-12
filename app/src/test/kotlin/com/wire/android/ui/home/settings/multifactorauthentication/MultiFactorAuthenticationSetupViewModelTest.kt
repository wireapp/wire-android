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
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MultiFactorAuthenticationSetupViewModelTest {
    private val owner = ViewModelStore()

    @AfterEach
    fun tearDown() {
        owner.clear()
    }

    @Test
    fun `setup requires verification and acknowledgement before it can finish`() = runTest {
        val viewModel = createViewModel()
        viewModel.finishSetup("Android phone")
        assertNull(viewModel.state.value.completion)

        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        assertTrue(viewModel.state.value.isBusy)
        assertTrue(viewModel.password.text.isEmpty())
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.enrollment)
        assertEquals(MultiFactorAuthenticationSetupStep.LINK_AUTHENTICATOR, viewModel.state.value.nextStep)
        viewModel.navigationHandled()

        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.verifyAuthenticationCode()
        advanceUntilIdle()
        assertEquals(MultiFactorAuthenticationSetupStep.RECOVERY_CODES, viewModel.state.value.nextStep)
        assertEquals(10, viewModel.state.value.recoveryCodes.size)
        assertTrue(viewModel.code.text.isEmpty())
        viewModel.finishSetup("Android phone")
        assertNull(viewModel.state.value.completion)

        viewModel.setRecoveryCodesSaved(true)
        viewModel.finishSetup("Android phone")
        assertEquals(viewModel.state.value.enrollment!!.id, viewModel.state.value.completion!!.authenticatorId)
        assertEquals(10, viewModel.state.value.completion!!.recoveryCodesCount)
    }

    @Test
    fun `an invalid code can be retried without losing the enrollment`() = runTest {
        val viewModel = createViewModel()
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        advanceUntilIdle()
        viewModel.navigationHandled()
        val enrollment = viewModel.state.value.enrollment

        viewModel.code.setTextAndPlaceCursorAtEnd("000000")
        viewModel.verifyAuthenticationCode()
        advanceUntilIdle()
        assertEquals(MultiFactorAuthenticationSetupError.INVALID_CODE, viewModel.state.value.error)
        assertNull(viewModel.state.value.nextStep)
        assertTrue(viewModel.state.value.recoveryCodes.isEmpty())

        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.verifyAuthenticationCode()
        advanceUntilIdle()
        assertNull(viewModel.state.value.error)
        assertEquals(enrollment, viewModel.state.value.enrollment)
        assertEquals(10, viewModel.state.value.recoveryCodes.size)
    }

    @Test
    fun `revisiting confirmation and verification retains the original enrollment and recovery codes`() = runTest {
        val viewModel = createViewModel()
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        advanceUntilIdle()
        viewModel.navigationHandled()
        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.verifyAuthenticationCode()
        advanceUntilIdle()
        val original = viewModel.state.value
        viewModel.navigationHandled()

        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        advanceUntilIdle()
        viewModel.navigationHandled()
        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.verifyAuthenticationCode()
        advanceUntilIdle()

        assertEquals(original.enrollment, viewModel.state.value.enrollment)
        assertEquals(original.recoveryCodes, viewModel.state.value.recoveryCodes)
    }

    @Test
    fun `back during confirmation cancels the operation and does not advance later`() = runTest {
        val viewModel = createViewModel()
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        viewModel.cancelPendingOperation()
        advanceUntilIdle()

        assertNull(viewModel.state.value.enrollment)
        assertNull(viewModel.state.value.nextStep)
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isBusy)
    }

    @Test
    fun `failure is retryable and disposing the flow clears its temporary data`() = runTest {
        val startSetup = mockk<StartMultiFactorAuthenticationSetupUseCase>()
        coEvery { startSetup(any()) } throws IOException("Offline")
        val viewModel = createViewModel(startSetup)
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        advanceUntilIdle()
        assertEquals(MultiFactorAuthenticationSetupError.GENERIC, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isBusy)

        coEvery { startSetup(any()) } returns MultiFactorAuthenticationEnrollment("id", "demo@wire.example", "secret")
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.enrollment)
        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.verifyAuthenticationCode()
        advanceUntilIdle()

        owner.clear()

        assertEquals(MultiFactorAuthenticationSetupState(), viewModel.state.value)
        assertTrue(viewModel.password.text.isEmpty())
        assertTrue(viewModel.code.text.isEmpty())
    }

    private fun createViewModel(
        startSetup: StartMultiFactorAuthenticationSetupUseCase = StartMultiFactorAuthenticationSetupUseCase(),
    ): MultiFactorAuthenticationSetupViewModel = ViewModelProvider.create(
        owner,
        viewModelFactory {
            initializer { MultiFactorAuthenticationSetupViewModel(startSetup, VerifyMultiFactorAuthenticationSetupCodeUseCase()) }
        },
    )[MultiFactorAuthenticationSetupViewModel::class]
}
