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
package com.wire.android.ui.home.settings.account.handle

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.framework.TestUser
import com.wire.android.ui.authentication.create.common.handle.HandleUpdateErrorState
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.feature.auth.ValidateUserHandleResult
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleResult
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class ChangeHandleViewModelTest {

    @Test
    fun `given updateHandle returns Success, when onHandleChanged is called, then navigate back`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidateHandleResult(ValidateUserHandleResult.Valid("handle"))
            .withHandle("handle")
            .withUpdateHandleResult(SetUserHandleResult.Success)
            .arrange()

        viewModel.onSaveClicked()

        coVerify(exactly = 1) {
            arrangement.setHandle("handle")
        }
        assertEquals(true, viewModel.state.isSuccess)
    }

    @Test
    fun `given updateHandle returns HandleExists Error, when onSaveClicked is called, then update error state`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidateHandleResult(ValidateUserHandleResult.Valid("handle"))
            .withHandle("handle")
            .withUpdateHandleResult(SetUserHandleResult.Failure.HandleExists)
            .arrange()

        viewModel.onSaveClicked()

        assertEquals(viewModel.state.error, HandleUpdateErrorState.TextFieldError.UsernameTakenError)
        assertEquals(false, viewModel.state.isSuccess)
    }

    @Test
    fun `given updateHandle returns InvalidHandle Error, when onSaveClicked is called, then update error state`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidateHandleResult(ValidateUserHandleResult.Valid("handle"))
            .withHandle("handle")
            .withUpdateHandleResult(SetUserHandleResult.Failure.InvalidHandle)
            .arrange()

        viewModel.onSaveClicked()

        assertEquals(viewModel.state.error, HandleUpdateErrorState.TextFieldError.UsernameInvalidError)
        assertEquals(false, viewModel.state.isSuccess)
        coVerify(exactly = 1) {
            arrangement.validateHandle("handle")
        }
    }

    @Test
    fun `given updateHandle returns generic Error, when onSaveClicked is called, then update error state`() = runTest {
        val expectedError = NetworkFailure.NoNetworkConnection(IOException())
        val (arrangement, viewModel) = Arrangement()
            .withValidateHandleResult(ValidateUserHandleResult.Valid("handle"))
            .withHandle("handle")
            .withUpdateHandleResult(SetUserHandleResult.Failure.Generic(expectedError))
            .arrange()

        viewModel.onSaveClicked()

        assertEquals(
            viewModel.state.error,
            HandleUpdateErrorState.DialogError.GenericError(expectedError)
        )
        assertEquals(false, viewModel.state.isSuccess)
    }

    @Test
    fun `given validateHandle returns Success, when onHandleChanged is called, then updateState`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidateHandleResult(ValidateUserHandleResult.Valid("handle"))
            .arrange()

        viewModel.textState.setTextAndPlaceCursorAtEnd("handle")

        assertEquals(viewModel.textState.text.toString(), "handle")
        assertEquals(viewModel.state.error, HandleUpdateErrorState.None)

        coVerify(exactly = 1) {
            arrangement.validateHandle("handle")
        }
    }

    @Test
    fun `given validateHandle returns Invalid, when onHandleChanged is called, then updateState`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidateHandleResult(
                ValidateUserHandleResult.Invalid.InvalidCharacters(
                    "handle",
                    listOf('@')
                )
            )
            .arrange()

        viewModel.textState.setTextAndPlaceCursorAtEnd("@handle")

        assertEquals(viewModel.textState.text.toString(), "@handle")
        assertEquals(viewModel.state.error, HandleUpdateErrorState.TextFieldError.UsernameInvalidError)

        coVerify(exactly = 1) {
            arrangement.validateHandle("@handle")
        }
    }

    private class Arrangement {
        @MockK
        lateinit var setHandle: SetUserHandleUseCase

        @MockK
        lateinit var validateHandle: ValidateUserHandleUseCase

        @MockK
        lateinit var getSelf: GetSelfUserUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { getSelf() } returns TestUser.SELF_USER
        }

        private val viewModel by lazy { ChangeHandleViewModel(setHandle, validateHandle, getSelf) }

        fun withHandle(handle: String) = apply {
            viewModel.textState.setTextAndPlaceCursorAtEnd(handle)
        }

        fun withUpdateHandleResult(result: SetUserHandleResult) = apply {
            coEvery { setHandle(any()) } returns result
        }

        fun withValidateHandleResult(result: ValidateUserHandleResult) = apply {
            coEvery { validateHandle(any()) } returns result
        }

        fun arrange() = this to viewModel
    }
}
