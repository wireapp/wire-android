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

import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.analytics.FinalizeRegistrationAnalyticsMetadataUseCase
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.mockUri
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.ui.authentication.create.common.handle.HandleUpdateErrorState
import com.wire.android.util.EMPTY
import com.wire.kalium.common.error.NetworkFailure
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
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class CreateAccountUsernameViewModelTest {

    @Test
    fun `given empty string, when entering username, then button is disabled`() = runTest {
        val username = String.EMPTY
        val (_, createAccountUsernameViewModel) = Arrangement()
            .withValidateHandleResult(ValidateUserHandleResult.Invalid.TooShort(username))
            .arrange()
        createAccountUsernameViewModel.textState.setTextAndPlaceCursorAtEnd(username)
        createAccountUsernameViewModel.state.continueEnabled shouldBeEqualTo false
        createAccountUsernameViewModel.state.loading shouldBeEqualTo false
    }

    @Test
    fun `given non-empty string, when entering username, then button is disabled`() = runTest {
        val username = "abc"
        val (_, createAccountUsernameViewModel) = Arrangement()
            .withValidateHandleResult(ValidateUserHandleResult.Valid(username))
            .arrange()
        createAccountUsernameViewModel.textState.setTextAndPlaceCursorAtEnd(username)
        createAccountUsernameViewModel.state.continueEnabled shouldBeEqualTo true
        createAccountUsernameViewModel.state.loading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when setting the username, then show loading`() = runTest {
        val username = "abc"
        val (arrangement, createAccountUsernameViewModel) = Arrangement()
            .withValidateHandleResult(ValidateUserHandleResult.Valid(username))
            .withSetUserHandle(SetUserHandleResult.Success)
            .arrange()

        createAccountUsernameViewModel.textState.setTextAndPlaceCursorAtEnd("abc")
        createAccountUsernameViewModel.state.continueEnabled shouldBeEqualTo true
        createAccountUsernameViewModel.state.loading shouldBeEqualTo false
        createAccountUsernameViewModel.onContinue()
        advanceUntilIdle()
        createAccountUsernameViewModel.state.continueEnabled shouldBeEqualTo true
        createAccountUsernameViewModel.state.loading shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when request returns Success, then success is passed`() = runTest {
        val username = "abc"
        val (arrangement, createAccountUsernameViewModel) = Arrangement()
            .withValidateHandleResult(ValidateUserHandleResult.Valid(username))
            .withSetUserHandle(SetUserHandleResult.Success)
            .arrange()

        createAccountUsernameViewModel.textState.setTextAndPlaceCursorAtEnd(username)
        createAccountUsernameViewModel.onContinue()
        advanceUntilIdle()
        verify(exactly = 1) { arrangement.validateUserHandleUseCase.invoke(username) }
        coVerify(exactly = 1) { arrangement.setUserHandleUseCase.invoke(username) }
        coVerify(exactly = 1) { arrangement.finalizeRegistrationAnalyticsMetadataUseCase.invoke() }
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsManager.sendEvent(eq(AnalyticsEvent.RegistrationPersonalAccount.CreationCompleted))
        }
        createAccountUsernameViewModel.state.success shouldBeEqualTo true
    }

    @Test
    fun `given button is clicked, when username is invalid, then UsernameInvalidError is passed`() = runTest {
        val username = "a"
        val (arrangement, createAccountUsernameViewModel) = Arrangement()
            .withValidateHandleResult(ValidateUserHandleResult.Invalid.TooShort(username))
            .withSetUserHandle(SetUserHandleResult.Failure.InvalidHandle)
            .arrange()

        createAccountUsernameViewModel.onContinue()
        advanceUntilIdle()
        createAccountUsernameViewModel.state.error shouldBeInstanceOf
                HandleUpdateErrorState.TextFieldError.UsernameInvalidError::class
        createAccountUsernameViewModel.state.success shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when request returns HandleExists error, then UsernameTakenError is passed`() = runTest {
        val username = "abc"
        val (arrangement, createAccountUsernameViewModel) = Arrangement()
            .withValidateHandleResult(ValidateUserHandleResult.Valid(username))
            .withSetUserHandle(SetUserHandleResult.Failure.HandleExists)
            .arrange()

        createAccountUsernameViewModel.onContinue()
        advanceUntilIdle()
        createAccountUsernameViewModel.state.error shouldBeInstanceOf
                HandleUpdateErrorState.TextFieldError.UsernameTakenError::class
        createAccountUsernameViewModel.state.success shouldBeEqualTo false
    }

    @Test
    fun `given button is clicked, when request returns Generic error, then GenericError is passed`() = runTest {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        val username = "abc"
        val (arrangement, createAccountUsernameViewModel) = Arrangement()
            .withValidateHandleResult(ValidateUserHandleResult.Valid(username))
            .withSetUserHandle(SetUserHandleResult.Failure.Generic(networkFailure))
            .arrange()

        createAccountUsernameViewModel.onContinue()
        advanceUntilIdle()
        createAccountUsernameViewModel.state.error shouldBeInstanceOf
                HandleUpdateErrorState.DialogError.GenericError::class
        val error = createAccountUsernameViewModel.state.error as HandleUpdateErrorState.DialogError.GenericError
        error.coreFailure shouldBe networkFailure
        createAccountUsernameViewModel.state.success shouldBeEqualTo false
    }

    @Test
    fun `given dialog is dismissed, when state error is DialogError, then hide error`() = runTest {
        val username = "abc"
        val (arrangement, createAccountUsernameViewModel) = Arrangement()
            .withValidateHandleResult(ValidateUserHandleResult.Valid(username))
            .withSetUserHandle(SetUserHandleResult.Failure.Generic(NetworkFailure.NoNetworkConnection(null)))
            .arrange()

        createAccountUsernameViewModel.onContinue()
        advanceUntilIdle()
        createAccountUsernameViewModel.state.error shouldBeInstanceOf
                HandleUpdateErrorState.DialogError.GenericError::class
        createAccountUsernameViewModel.onErrorDismiss()
        createAccountUsernameViewModel.state.error shouldBe HandleUpdateErrorState.None
    }

    @Test
    fun `given account name, when creating group, then do not show NameEmptyError until name is entered and cleared`() = runTest {
        val (_, viewModel) = Arrangement()
            .withValidateHandleResult(ValidateUserHandleResult.Invalid.TooShort(String.EMPTY), String.EMPTY)
            .withValidateHandleResult(ValidateUserHandleResult.Valid("name"), "name")
            .withSetUserHandle(SetUserHandleResult.Failure.HandleExists)
            .arrange()
        viewModel.textState.setTextAndPlaceCursorAtEnd(String.EMPTY)
        advanceUntilIdle()
        assertEquals(HandleUpdateErrorState.None, viewModel.state.error)

        viewModel.textState.setTextAndPlaceCursorAtEnd("name")
        advanceUntilIdle()
        assertEquals(HandleUpdateErrorState.None, viewModel.state.error)

        viewModel.textState.clearText()
        advanceUntilIdle()
        assertEquals(HandleUpdateErrorState.TextFieldError.UsernameInvalidError, viewModel.state.error)
    }

    private class Arrangement {
        @MockK
        lateinit var validateUserHandleUseCase: ValidateUserHandleUseCase

        @MockK
        lateinit var setUserHandleUseCase: SetUserHandleUseCase

        @MockK
        lateinit var anonymousAnalyticsManager: AnonymousAnalyticsManager

        @MockK
        lateinit var finalizeRegistrationAnalyticsMetadataUseCase: FinalizeRegistrationAnalyticsMetadataUseCase

        private val viewModel by lazy {
            CreateAccountUsernameViewModel(
                validateUserHandleUseCase,
                setUserHandleUseCase,
                anonymousAnalyticsManager,
                finalizeRegistrationAnalyticsMetadataUseCase
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockUri()
            coEvery { finalizeRegistrationAnalyticsMetadataUseCase() } returns Unit
            every { anonymousAnalyticsManager.sendEvent(any()) } returns Unit
        }

        fun withValidateHandleResult(result: ValidateUserHandleResult, forSpecificHandle: String? = null) = apply {
            coEvery { validateUserHandleUseCase(forSpecificHandle?.let { eq(it) } ?: any()) } returns result
        }

        fun withSetUserHandle(result: SetUserHandleResult) = apply {
            coEvery { setUserHandleUseCase.invoke(any()) } returns result
        }

        fun arrange() = this to viewModel
    }
}
