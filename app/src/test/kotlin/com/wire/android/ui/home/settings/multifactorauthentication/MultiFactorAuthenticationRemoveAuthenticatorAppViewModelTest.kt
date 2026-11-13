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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MultiFactorAuthenticationRemoveAuthenticatorAppViewModelTest {
    private val owner = ViewModelStore()

    @AfterEach
    fun tearDown() {
        owner.clear()
    }

    @Test
    fun `password and a valid code are required and duplicate submissions are ignored`() = runTest {
        val remove = mockk<RemoveAuthenticatorAppUseCase>()
        coEvery {
            remove(any(), any(), any())
        } coAnswers {
            RemoveAuthenticatorAppUseCase()(firstArg(), secondArg(), thirdArg())
        }
        val viewModel = createViewModel(remove)
        viewModel.confirmRemoval("backup")
        coVerify(exactly = 0) { remove(any(), any(), any()) }
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.code.setTextAndPlaceCursorAtEnd("000000")
        viewModel.confirmRemoval("backup")
        advanceUntilIdle()
        assertEquals(RemoveAuthenticatorAppError.INVALID_CODE, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isValidated)

        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.confirmRemoval("backup")
        viewModel.confirmRemoval("backup")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isValidated)
        assertTrue(viewModel.password.text.isEmpty())
        assertTrue(viewModel.code.text.isEmpty())
        coVerify(exactly = 1) { remove("backup", "password", "284913") }
        viewModel.removalRejected(AuthenticatorAppMutationResult.LAST_APP)
        assertEquals(RemoveAuthenticatorAppError.LAST_APP, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isValidated)
    }

    @Test
    fun `a failure can be retried without reporting a successful removal`() = runTest {
        val remove = mockk<RemoveAuthenticatorAppUseCase>()
        coEvery { remove(any(), any(), any()) } throws IOException("Offline")
        val viewModel = createViewModel(remove)
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.confirmRemoval("backup")
        advanceUntilIdle()
        assertEquals(RemoveAuthenticatorAppError.GENERIC, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isValidated)

        coEvery { remove(any(), any(), any()) } returns true
        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.confirmRemoval("backup")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isValidated)
    }

    @Test
    fun `cancelling or disposing a pending operation clears input without validating later`() = runTest {
        val viewModel = createViewModel()
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.confirmRemoval("backup")
        viewModel.cancel()
        advanceUntilIdle()
        assertEquals(MultiFactorAuthenticationRemoveAuthenticatorAppState(), viewModel.state.value)
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.code.setTextAndPlaceCursorAtEnd("284913")
        viewModel.confirmRemoval("backup")
        owner.clear()
        advanceUntilIdle()
        assertEquals(MultiFactorAuthenticationRemoveAuthenticatorAppState(), viewModel.state.value)
        assertTrue(viewModel.password.text.isEmpty())
        assertTrue(viewModel.code.text.isEmpty())
    }

    private fun createViewModel(
        remove: RemoveAuthenticatorAppUseCase = RemoveAuthenticatorAppUseCase(),
    ): MultiFactorAuthenticationRemoveAuthenticatorAppViewModel = ViewModelProvider.create(
        owner,
        viewModelFactory { initializer { MultiFactorAuthenticationRemoveAuthenticatorAppViewModel(remove) } },
    )[MultiFactorAuthenticationRemoveAuthenticatorAppViewModel::class]
}
