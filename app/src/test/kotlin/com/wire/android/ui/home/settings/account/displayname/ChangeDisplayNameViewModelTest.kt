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

package com.wire.android.ui.home.settings.account.displayname

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestUser
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.feature.user.DisplayNameUpdateResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UpdateDisplayNameUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ChangeDisplayNameViewModelTest {

    @Test
    fun `given useCase runs successfully, when saveDisplayName is invoked, then onSuccess callback is invoked`() =
        runTest {
            var isSuccess = false
            var isFailed = false
            val (_, viewModel) = Arrangement()
                .withUserSaveNameResult(DisplayNameUpdateResult.Success)
                .arrange()

            viewModel.saveDisplayName(
                onFailure = { isFailed = true },
                onSuccess = { isSuccess = true }
            )

            assertEquals(false, isFailed)
            assertEquals(true, isSuccess)
        }

    @Test
    fun `given useCase fails, when saveDisplayName is invoked, then onFailure callback is invoked`() =
        runTest {
            var isSuccess = false
            var isFailed = false
            val (_, viewModel) = Arrangement()
                .withUserSaveNameResult(DisplayNameUpdateResult.Failure(CoreFailure.Unknown(Error())))
                .arrange()

            viewModel.saveDisplayName(
                onFailure = { isFailed = true },
                onSuccess = { isSuccess = true }
            )

            assertEquals(true, isFailed)
            assertEquals(false, isSuccess)
        }

    @Test
    fun `when validating new name, and we have an empty value, then should propagate NameEmptyError`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        val newValue = TextFieldValue(" ")
        viewModel.onNameChange(newValue)

        assertEquals(DisplayNameState.NameError.TextFieldError.NameEmptyError, viewModel.displayNameState.error)
        assertTrue(viewModel.displayNameState.animatedNameError)
        assertFalse(viewModel.displayNameState.continueEnabled)
    }

    @Test
    fun `when validating new name, and the value exceeds 64 chars, then should propagate NameExceedLimitError`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        val over64CharString = TextFieldValue("a9p8fIRG12wvOJ8AKH77UqwHt8lzTTOBlSdIlq1N6xxYBsEIUomLKoRY2IZ1hClOM")
        viewModel.onNameChange(over64CharString)

        assertEquals(DisplayNameState.NameError.TextFieldError.NameExceedLimitError, viewModel.displayNameState.error)
        assertTrue(viewModel.displayNameState.animatedNameError)
        assertFalse(viewModel.displayNameState.continueEnabled)
    }

    @Test
    fun `when validating new name, and the value is the same, then should propagate None`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        viewModel.onNameChange(TextFieldValue("username "))

        assertEquals(DisplayNameState.NameError.None, viewModel.displayNameState.error)
        assertFalse(viewModel.displayNameState.animatedNameError)
        assertFalse(viewModel.displayNameState.continueEnabled)
    }

    @Test
    fun `when validating new name, and the value is valid, then should propagate None and enable 'continue'`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        viewModel.onNameChange(TextFieldValue("valid new name"))

        assertEquals(DisplayNameState.NameError.None, viewModel.displayNameState.error)
        assertFalse(viewModel.displayNameState.animatedNameError)
        assertTrue(viewModel.displayNameState.continueEnabled)
    }

    @Test
    fun `when calling onAnimatedError, should emit animatedNameError false to clean state`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        viewModel.onNameErrorAnimated()

        assertFalse(viewModel.displayNameState.animatedNameError)
    }

    private class Arrangement {

        @MockK
        lateinit var getSelfUserUseCase: GetSelfUserUseCase

        @MockK
        lateinit var updateDisplayNameUseCase: UpdateDisplayNameUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { getSelfUserUseCase() } returns flowOf(TestUser.SELF_USER)
        }

        fun withUserSaveNameResult(result: DisplayNameUpdateResult) = apply {
            coEvery { updateDisplayNameUseCase(any()) } returns result
        }

        fun arrange() = this to ChangeDisplayNameViewModel(
            getSelfUserUseCase,
            updateDisplayNameUseCase,
            TestDispatcherProvider()
        )
    }
}
