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

package com.wire.android.ui.authentication.create.username

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.mockUri
import com.wire.android.ui.authentication.create.common.handle.HandleUpdateErrorState
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.feature.auth.ValidateUserHandleResult
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleResult
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class CreateAccountUsernameViewModelTest {

    @MockK
    private lateinit var validateUserHandleUseCase: ValidateUserHandleUseCase

    @MockK
    private lateinit var setUserHandleUseCase: SetUserHandleUseCase

    @MockK(relaxed = true)
    private lateinit var onSuccess: () -> Unit

    private lateinit var createAccountUsernameViewModel: CreateAccountUsernameViewModel

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        mockUri()
        createAccountUsernameViewModel = CreateAccountUsernameViewModel(validateUserHandleUseCase, setUserHandleUseCase)
    }

    @Test
    fun `given empty string, when entering username, then button is disabled`() {
        every { validateUserHandleUseCase.invoke(String.EMPTY) } returns ValidateUserHandleResult.Invalid.TooShort(String.EMPTY)
        createAccountUsernameViewModel.onUsernameChange(TextFieldValue(String.EMPTY))
        createAccountUsernameViewModel.state.continueEnabled shouldBeEqualTo false
        createAccountUsernameViewModel.state.loading shouldBeEqualTo false
    }

    @Test
    fun `given non-empty string, when entering username, then button is disabled`() {
        every { validateUserHandleUseCase.invoke("abc") } returns ValidateUserHandleResult.Valid("abc")
        createAccountUsernameViewModel.onUsernameChange(TextFieldValue("abc"))
        createAccountUsernameViewModel.state.continueEnabled shouldBeEqualTo true
        createAccountUsernameViewModel.state.loading shouldBeEqualTo false
    }

    @Test
    fun `given forbidden character, when entering username, then forbidden character is ignored`() {
        every { validateUserHandleUseCase.invoke("a1_") } returns ValidateUserHandleResult.Valid("a1_")
        every { validateUserHandleUseCase.invoke("a1_$") } returns
                ValidateUserHandleResult.Invalid.InvalidCharacters("a1_", listOf())
        every { validateUserHandleUseCase.invoke("a1_$") } returns ValidateUserHandleResult.Invalid.InvalidCharacters(
            "a1_",
            "@".toList()
        )
        createAccountUsernameViewModel.onUsernameChange(TextFieldValue("a1_"))
        createAccountUsernameViewModel.state.username.text shouldBeEqualTo "a1_"
        createAccountUsernameViewModel.onUsernameChange(TextFieldValue("a1_$"))
        createAccountUsernameViewModel.state.username.text shouldBeEqualTo "a1_"
    }

    @Test
    fun `given button is clicked, when setting the username, then show loading`() = runTest {
        every { validateUserHandleUseCase.invoke(any()) } returns ValidateUserHandleResult.Valid("abc")
        coEvery { setUserHandleUseCase.invoke(any()) } returns SetUserHandleResult.Success

        createAccountUsernameViewModel.onUsernameChange(TextFieldValue("abc"))
        createAccountUsernameViewModel.state.continueEnabled shouldBeEqualTo true
        createAccountUsernameViewModel.state.loading shouldBeEqualTo false
        createAccountUsernameViewModel.onContinue(onSuccess)
        advanceUntilIdle()
        createAccountUsernameViewModel.state.continueEnabled shouldBeEqualTo true
        createAccountUsernameViewModel.state.loading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when request returns Success, then navigate to initial sync screen`() = runTest {
        val username = "abc"
        every { validateUserHandleUseCase.invoke(any()) } returns ValidateUserHandleResult.Valid(username)
        coEvery { setUserHandleUseCase.invoke(any()) } returns SetUserHandleResult.Success
        createAccountUsernameViewModel.onUsernameChange(TextFieldValue(username))

        createAccountUsernameViewModel.onContinue(onSuccess)
        advanceUntilIdle()

        // FIXME: change to 1 once the viewModel is fixed
        verify(exactly = 2) { validateUserHandleUseCase.invoke(username) }
        coVerify(exactly = 1) { setUserHandleUseCase.invoke(username) }
        verify(exactly = 1) { onSuccess() }
    }

    @Test
    fun `given button is clicked, when username is invalid, then UsernameInvalidError is passed`() = runTest {
        every { validateUserHandleUseCase.invoke(any()) } returns ValidateUserHandleResult.Invalid.TooShort("a")
        coEvery { setUserHandleUseCase.invoke(any()) } returns SetUserHandleResult.Failure.InvalidHandle

        createAccountUsernameViewModel.onContinue(onSuccess)
        advanceUntilIdle()
        createAccountUsernameViewModel.state.error shouldBeInstanceOf
                HandleUpdateErrorState.TextFieldError.UsernameInvalidError::class
    }

    @Test
    fun `given button is clicked, when request returns HandleExists error, then UsernameTakenError is passed`() = runTest {
        every { validateUserHandleUseCase.invoke(any()) } returns ValidateUserHandleResult.Valid("abc")
        coEvery { setUserHandleUseCase.invoke(any()) } returns SetUserHandleResult.Failure.HandleExists

        createAccountUsernameViewModel.onContinue(onSuccess)
        advanceUntilIdle()
        createAccountUsernameViewModel.state.error shouldBeInstanceOf
                HandleUpdateErrorState.TextFieldError.UsernameTakenError::class
    }

    @Test
    fun `given button is clicked, when request returns Generic error, then GenericError is passed`() = runTest {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        every { validateUserHandleUseCase.invoke(any()) } returns ValidateUserHandleResult.Valid("abc")
        coEvery { setUserHandleUseCase.invoke(any()) } returns SetUserHandleResult.Failure.Generic(networkFailure)

        createAccountUsernameViewModel.onContinue(onSuccess)
        advanceUntilIdle()
        createAccountUsernameViewModel.state.error shouldBeInstanceOf
                HandleUpdateErrorState.DialogError.GenericError::class
        val error = createAccountUsernameViewModel.state.error as HandleUpdateErrorState.DialogError.GenericError
        error.coreFailure shouldBe networkFailure
    }

    @Test
    fun `given dialog is dismissed, when state error is DialogError, then hide error`() = runTest {
        every { validateUserHandleUseCase.invoke(any()) } returns ValidateUserHandleResult.Valid("abc")
        coEvery { setUserHandleUseCase.invoke(any()) } returns SetUserHandleResult.Failure.Generic(NetworkFailure.NoNetworkConnection(null))

        createAccountUsernameViewModel.onContinue(onSuccess)
        advanceUntilIdle()
        createAccountUsernameViewModel.state.error shouldBeInstanceOf
                HandleUpdateErrorState.DialogError.GenericError::class
        createAccountUsernameViewModel.onErrorDismiss()
        createAccountUsernameViewModel.state.error shouldBe HandleUpdateErrorState.None
    }
}
