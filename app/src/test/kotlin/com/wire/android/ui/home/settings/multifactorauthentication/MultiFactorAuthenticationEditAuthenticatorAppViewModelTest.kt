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
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MultiFactorAuthenticationEditAuthenticatorAppViewModelTest {
    private val owner = ViewModelStore()
    private val app = AuthenticatorApp("phone", "Android phone", LocalDate(2026, 10, 14))

    @AfterEach
    fun tearDown() {
        owner.clear()
    }

    @Test
    fun `observing the app again does not overwrite the edited draft`() = runTest {
        val viewModel = createViewModel()
        viewModel.initialize(app)
        assertEquals(app.name, viewModel.name.text.toString())
        viewModel.name.setTextAndPlaceCursorAtEnd("My draft")

        viewModel.initialize(app.copy(name = "Refreshed name"))

        assertEquals("My draft", viewModel.name.text.toString())
        assertEquals(app, viewModel.state.value.app)
    }

    @Test
    fun `blank and unchanged names cannot be submitted`() = runTest {
        val rename = mockk<RenameAuthenticatorAppUseCase>()
        val viewModel = createViewModel(rename)
        viewModel.save()
        viewModel.initialize(app)
        listOf("", "   ", app.name, " ${app.name} ").forEach { name ->
            viewModel.name.setTextAndPlaceCursorAtEnd(name)
            viewModel.save()
        }

        assertNull(viewModel.state.value.result)
        assertFalse(viewModel.state.value.isSaving)
        coVerify(exactly = 0) { rename(any(), any()) }
    }

    @Test
    fun `saving trims the new name and ignores repeated submissions`() = runTest {
        val rename = mockk<RenameAuthenticatorAppUseCase>()
        coEvery { rename(any(), any()) } coAnswers { RenameAuthenticatorAppUseCase()(firstArg(), secondArg()) }
        val viewModel = createViewModel(rename)
        viewModel.initialize(app)
        viewModel.name.setTextAndPlaceCursorAtEnd("  Backup phone  ")

        viewModel.save()
        assertTrue(viewModel.state.value.isSaving)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(AuthenticatorAppRenameResult(app.id, "Backup phone"), viewModel.state.value.result)
        assertFalse(viewModel.state.value.isSaving)
        viewModel.save()
        coVerify(exactly = 1) { rename(app.id, "Backup phone") }
    }

    @Test
    fun `a failed rename preserves the draft and can be retried`() = runTest {
        val rename = mockk<RenameAuthenticatorAppUseCase>()
        coEvery { rename(any(), any()) } throws IOException("Offline")
        val viewModel = createViewModel(rename)
        viewModel.initialize(app)
        viewModel.name.setTextAndPlaceCursorAtEnd("Backup phone")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.hasError)
        assertNull(viewModel.state.value.result)
        assertEquals("Backup phone", viewModel.name.text.toString())
        coEvery { rename(any(), any()) } returns AuthenticatorAppRenameResult(app.id, "Backup phone")
        viewModel.save()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.hasError)
        assertEquals(AuthenticatorAppRenameResult(app.id, "Backup phone"), viewModel.state.value.result)
    }

    @Test
    fun `back during saving cancels the pending rename without returning a result`() = runTest {
        val viewModel = createViewModel()
        viewModel.initialize(app)
        viewModel.name.setTextAndPlaceCursorAtEnd("Backup phone")
        viewModel.save()

        viewModel.cancel()
        advanceUntilIdle()

        assertNull(viewModel.state.value.result)
        assertFalse(viewModel.state.value.isSaving)
        assertFalse(viewModel.state.value.hasError)
    }

    private fun createViewModel(
        rename: RenameAuthenticatorAppUseCase = RenameAuthenticatorAppUseCase(),
    ): MultiFactorAuthenticationEditAuthenticatorAppViewModel = ViewModelProvider.create(
        owner,
        viewModelFactory { initializer { MultiFactorAuthenticationEditAuthenticatorAppViewModel(rename) } },
    )[MultiFactorAuthenticationEditAuthenticatorAppViewModel::class]
}
