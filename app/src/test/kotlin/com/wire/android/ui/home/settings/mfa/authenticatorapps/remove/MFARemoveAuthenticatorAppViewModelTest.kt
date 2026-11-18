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

package com.wire.android.ui.home.settings.mfa.authenticatorapps.remove

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.settings.mfa.MFATestFixture
import com.wire.kalium.logic.data.mfa.MFAFailure
import com.wire.kalium.logic.feature.mfa.RemoveAuthenticatorAppResult
import com.wire.kalium.logic.feature.mfa.RemoveAuthenticatorAppUseCase
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
class MFARemoveAuthenticatorAppViewModelTest {
    private val owner = ViewModelStore()

    @AfterEach
    fun tearDown() {
        owner.clear()
    }

    @Test
    fun `password and a valid code are required and duplicate submissions are ignored`() = runTest {
        val remove = MFATestFixture().remove
        val viewModel = createViewModel(remove)
        viewModel.confirmRemoval("backup")
        coVerify(exactly = 0) { remove(any(), any(), any()) }
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.code.setTextAndPlaceCursorAtEnd("000000")
        viewModel.confirmRemoval("backup")
        advanceUntilIdle()
        assertEquals(RemoveAuthenticatorAppError.INVALID_CODE, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isCompleted)

        viewModel.code.setTextAndPlaceCursorAtEnd("111111")
        viewModel.confirmRemoval("backup")
        viewModel.confirmRemoval("backup")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isCompleted)
        assertTrue(viewModel.password.text.isEmpty())
        assertTrue(viewModel.code.text.isEmpty())
        coVerify(exactly = 1) { remove("backup", "password", "111111") }
    }

    @Test
    fun `a failure can be retried without reporting a successful removal`() = runTest {
        val remove = mockk<RemoveAuthenticatorAppUseCase>()
        coEvery { remove(any(), any(), any()) } throws IOException("Offline")
        val viewModel = createViewModel(remove)
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.code.setTextAndPlaceCursorAtEnd("111111")
        viewModel.confirmRemoval("backup")
        advanceUntilIdle()
        assertEquals(RemoveAuthenticatorAppError.GENERIC, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isCompleted)

        coEvery { remove(any(), any(), any()) } returns RemoveAuthenticatorAppResult.Success
        viewModel.code.setTextAndPlaceCursorAtEnd("111111")
        viewModel.confirmRemoval("backup")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isCompleted)
    }

    @Test
    fun `Kalium rejecting the last authenticator keeps confirmation open`() = runTest {
        val remove = mockk<RemoveAuthenticatorAppUseCase>()
        coEvery { remove(any(), any(), any()) } returns RemoveAuthenticatorAppResult.Failure(MFAFailure.LAST_APP)
        val viewModel = createViewModel(remove)
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.code.setTextAndPlaceCursorAtEnd("111111")
        viewModel.confirmRemoval("last")
        advanceUntilIdle()
        assertEquals(RemoveAuthenticatorAppError.LAST_APP, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isCompleted)
    }

    @Test
    fun `cancelling or disposing a pending operation clears input without validating later`() = runTest {
        val viewModel = createViewModel()
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.code.setTextAndPlaceCursorAtEnd("111111")
        viewModel.confirmRemoval("backup")
        viewModel.cancel()
        advanceUntilIdle()
        assertEquals(MFARemoveAuthenticatorAppState(), viewModel.state.value)
        viewModel.password.setTextAndPlaceCursorAtEnd("password")
        viewModel.code.setTextAndPlaceCursorAtEnd("111111")
        viewModel.confirmRemoval("backup")
        owner.clear()
        advanceUntilIdle()
        assertEquals(MFARemoveAuthenticatorAppState(), viewModel.state.value)
        assertTrue(viewModel.password.text.isEmpty())
        assertTrue(viewModel.code.text.isEmpty())
    }

    private fun createViewModel(
        remove: RemoveAuthenticatorAppUseCase = MFATestFixture().remove,
    ): MFARemoveAuthenticatorAppViewModel = ViewModelProvider.create(
        owner,
        viewModelFactory { initializer { MFARemoveAuthenticatorAppViewModel(remove) } },
    )[MFARemoveAuthenticatorAppViewModel::class]
}
