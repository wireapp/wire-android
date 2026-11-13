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
class MultiFactorAuthenticationAddAuthenticatorAppViewModelTest {
    private val owner = ViewModelStore()

    @AfterEach
    fun tearDown() {
        owner.clear()
    }

    @Test
    fun `adding an app requires verification and a name and skips recovery codes`() = runTest {
        val finish = mockk<FinishAddingAuthenticatorAppUseCase>()
        coEvery { finish(any(), any()) } coAnswers { FinishAddingAuthenticatorAppUseCase()(firstArg(), secondArg()) }
        val viewModel = createViewModel(finish)
        viewModel.name.setTextAndPlaceCursorAtEnd("Backup phone")
        viewModel.addApp()
        assertNull(viewModel.state.value.completion)

        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        advanceUntilIdle()
        val enrollment = viewModel.state.value.enrollment!!
        viewModel.navigationHandled()
        viewModel.addApp()
        assertNull(viewModel.state.value.completion)

        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.verifyAuthenticationCode()
        advanceUntilIdle()
        assertEquals(MultiFactorAuthenticationSetupStep.NAME_APP, viewModel.state.value.nextStep)
        viewModel.name.setTextAndPlaceCursorAtEnd("   ")
        viewModel.addApp()
        assertNull(viewModel.state.value.completion)

        viewModel.name.setTextAndPlaceCursorAtEnd("  Backup phone  ")
        viewModel.addApp()
        viewModel.addApp()
        assertTrue(viewModel.state.value.isBusy)
        advanceUntilIdle()
        assertEquals(AuthenticatorAppAddedResult(enrollment.id, "Backup phone"), viewModel.state.value.completion)
        coVerify(exactly = 1) { finish(enrollment.id, "Backup phone") }
    }

    @Test
    fun `invalid codes and completion failures can be retried using the same enrollment`() = runTest {
        val finish = mockk<FinishAddingAuthenticatorAppUseCase>()
        coEvery { finish(any(), any()) } throws IOException("Offline")
        val viewModel = createViewModel(finish)
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        advanceUntilIdle()
        val enrollment = viewModel.state.value.enrollment!!
        viewModel.navigationHandled()
        viewModel.code.setTextAndPlaceCursorAtEnd("000000")
        viewModel.verifyAuthenticationCode()
        advanceUntilIdle()
        assertEquals(MultiFactorAuthenticationSetupError.INVALID_CODE, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isVerified)

        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.verifyAuthenticationCode()
        advanceUntilIdle()
        viewModel.name.setTextAndPlaceCursorAtEnd("Backup")
        viewModel.addApp()
        advanceUntilIdle()
        assertEquals(MultiFactorAuthenticationSetupError.GENERIC, viewModel.state.value.error)
        assertNull(viewModel.state.value.completion)
        coEvery { finish(any(), any()) } returns AuthenticatorAppAddedResult(enrollment.id, "Backup")
        viewModel.addApp()
        advanceUntilIdle()
        assertEquals(enrollment, viewModel.state.value.enrollment)
        assertEquals(AuthenticatorAppAddedResult(enrollment.id, "Backup"), viewModel.state.value.completion)
    }

    @Test
    fun `cancellation does not advance and disposing the flow clears its enrollment and drafts`() = runTest {
        val viewModel = createViewModel()
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        viewModel.cancelPendingOperation()
        advanceUntilIdle()
        assertNull(viewModel.state.value.enrollment)
        assertNull(viewModel.state.value.nextStep)
        assertNull(viewModel.state.value.error)

        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPassword()
        advanceUntilIdle()
        viewModel.name.setTextAndPlaceCursorAtEnd("Backup")
        owner.clear()
        assertEquals(MultiFactorAuthenticationAddAuthenticatorAppState(), viewModel.state.value)
        assertTrue(viewModel.password.text.isEmpty())
        assertTrue(viewModel.code.text.isEmpty())
        assertTrue(viewModel.name.text.isEmpty())
    }

    private fun createViewModel(
        finish: FinishAddingAuthenticatorAppUseCase = FinishAddingAuthenticatorAppUseCase(),
    ): MultiFactorAuthenticationAddAuthenticatorAppViewModel = ViewModelProvider.create(
        owner,
        viewModelFactory {
            initializer {
                MultiFactorAuthenticationAddAuthenticatorAppViewModel(
                    StartMultiFactorAuthenticationSetupUseCase(),
                    VerifyAuthenticatorAppCodeUseCase(),
                    finish,
                )
            }
        },
    )[MultiFactorAuthenticationAddAuthenticatorAppViewModel::class]
}
