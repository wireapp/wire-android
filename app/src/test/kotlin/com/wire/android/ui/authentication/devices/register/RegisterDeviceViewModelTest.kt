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

package com.wire.android.ui.authentication.devices.register

import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.mockUri
import com.wire.android.datastore.UserDataStore
import com.wire.android.framework.TestClient
import com.wire.android.framework.TestUser
import com.wire.android.util.EMPTY
import com.wire.android.util.ui.CountdownTimer
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.data.auth.verification.VerifiableAction
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class RegisterDeviceViewModelTest {

    @Test
    fun `given empty string, when entering the password to register, then button is disabled`() = runTest {
        val (arrangement, registerDeviceViewModel) = Arrangement().arrange()
        registerDeviceViewModel.passwordTextState.setTextAndPlaceCursorAtEnd(String.EMPTY)
        registerDeviceViewModel.state.continueEnabled shouldBeEqualTo false
        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.Default::class
    }

    @Test
    fun `given non-empty string, when entering the password to register, then button is disabled`() = runTest {
        val (arrangement, registerDeviceViewModel) = Arrangement().arrange()
        registerDeviceViewModel.passwordTextState.setTextAndPlaceCursorAtEnd("abc")
        registerDeviceViewModel.state.continueEnabled shouldBeEqualTo true
        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.Default::class
    }

    @Test
    fun `given button is clicked, when request returns Success, then navigateToHomeScreen is called`() = runTest {
        val password = "abc"
        val (arrangement, registerDeviceViewModel) = Arrangement()
            .withRegisterClientReturning(RegisterClientResult.Success(CLIENT))
            .arrange()
        registerDeviceViewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.registerClientUseCase(any())
        }
        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.Success::class
    }

    @Test
    fun `given button is clicked, when request returns TooManyClients Error, then navigateToRemoveDevicesScreen is called`() = runTest {
        val password = "abc"
        val (arrangement, registerDeviceViewModel) = Arrangement()
            .withRegisterClientReturning(RegisterClientResult.Failure.TooManyClients)
            .arrange()
        registerDeviceViewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.registerClientUseCase(any())
        }
        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.TooManyDevices::class
    }

    @Test
    fun `given button is clicked, when password is invalid, then UsernameInvalidError is passed`() = runTest {
        val (arrangement, registerDeviceViewModel) = Arrangement()
            .withRegisterClientReturning(RegisterClientResult.Failure.InvalidCredentials.InvalidPassword)
            .arrange()

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()

        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.Error::class
    }

    @Test
    fun `given button is clicked, when request returns Generic error, then GenericError is passed`() = runTest {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        val (arrangement, registerDeviceViewModel) = Arrangement()
            .withRegisterClientReturning(RegisterClientResult.Failure.Generic(networkFailure))
            .arrange()

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()

        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.Error.GenericError::class
        val error = registerDeviceViewModel.state.flowState as RegisterDeviceFlowState.Error.GenericError
        error.coreFailure shouldBe networkFailure
    }

    @Test
    fun `given dialog is dismissed, when state error is DialogError, then hide error`() = runTest {
        val networkFailure = NetworkFailure.NoNetworkConnection(null)
        val (arrangement, registerDeviceViewModel) = Arrangement()
            .withRegisterClientReturning(RegisterClientResult.Failure.Generic(networkFailure))
            .arrange()

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()

        registerDeviceViewModel.state.flowState shouldBeInstanceOf RegisterDeviceFlowState.Error.GenericError::class
        registerDeviceViewModel.onErrorDismiss()
        registerDeviceViewModel.state.flowState shouldBe RegisterDeviceFlowState.Default
    }

    @Test
    fun `given missing 2fa, when registering, then request 2fa code`() = runTest {
        val email = "user@email.com"
        val (arrangement, registerDeviceViewModel) = Arrangement()
            .withRegisterClientReturning(RegisterClientResult.Failure.InvalidCredentials.Missing2FA)
            .withGetSelfReturning(SELF_USER.copy(email = email))
            .withRequestSecondFactorVerificationCodeReturning(RequestSecondFactorVerificationCodeUseCase.Result.Success)
            .arrange()

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()

        assertEquals(true, registerDeviceViewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        coVerify(exactly = 1) {
            arrangement.requestSecondFactorVerificationCodeUseCase(email, VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION)
        }
    }

    @Test
    fun `given invalid 2fa, when registering with reused code, then request 2fa code and do not show invalid code`() = runTest {
        val email = "user@email.com"
        val (arrangement, registerDeviceViewModel) = Arrangement()
            .withRegisterClientReturning(RegisterClientResult.Failure.InvalidCredentials.Invalid2FA)
            .withGetSelfReturning(SELF_USER.copy(email = email))
            .withRequestSecondFactorVerificationCodeReturning(RequestSecondFactorVerificationCodeUseCase.Result.Success)
            .arrange()
        registerDeviceViewModel.secondFactorVerificationCodeTextState.clearText() // no code yet entered

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()

        assertEquals(true, registerDeviceViewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        assertEquals(false, registerDeviceViewModel.secondFactorVerificationCodeState.isCurrentCodeInvalid)
        coVerify(exactly = 1) {
            arrangement.requestSecondFactorVerificationCodeUseCase(email, VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION)
        }
    }

    @Test
    fun `given invalid 2fa, when registering with entered code by user, then request 2fa code and show invalid code`() = runTest {
        val email = "user@email.com"
        val (arrangement, registerDeviceViewModel) = Arrangement()
            .withRegisterClientReturning(RegisterClientResult.Failure.InvalidCredentials.Invalid2FA)
            .withGetSelfReturning(SELF_USER.copy(email = email))
            .withRequestSecondFactorVerificationCodeReturning(RequestSecondFactorVerificationCodeUseCase.Result.Success)
            .arrange()
        registerDeviceViewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd("123456") // code entered by user

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()

        assertEquals(true, registerDeviceViewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        assertEquals(true, registerDeviceViewModel.secondFactorVerificationCodeState.isCurrentCodeInvalid)
        coVerify(exactly = 1) {
            arrangement.requestSecondFactorVerificationCodeUseCase(email, VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION)
        }
    }

    @Test
    fun `given success, when requesting 2fa code, then require code input`() = runTest {
        val email = "user@email.com"
        val (arrangement, registerDeviceViewModel) = Arrangement()
            .withRegisterClientReturning(RegisterClientResult.Failure.InvalidCredentials.Invalid2FA)
            .withGetSelfReturning(SELF_USER.copy(email = email))
            .withRequestSecondFactorVerificationCodeReturning(RequestSecondFactorVerificationCodeUseCase.Result.Success)
            .arrange()

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()

        assertEquals(true, registerDeviceViewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
    }

    @Test
    fun `given too many requests failure, when requesting 2fa code, then require code input`() = runTest {
        val email = "user@email.com"
        val (arrangement, registerDeviceViewModel) = Arrangement()
            .withRegisterClientReturning(RegisterClientResult.Failure.InvalidCredentials.Invalid2FA)
            .withGetSelfReturning(SELF_USER.copy(email = email))
            .withRequestSecondFactorVerificationCodeReturning(RequestSecondFactorVerificationCodeUseCase.Result.Failure.TooManyRequests)
            .arrange()

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()

        assertEquals(true, registerDeviceViewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
    }

    @Test
    fun `given other failure, when requesting 2fa code, then show error and do not require code input`() = runTest {
        val email = "user@email.com"
        val failure = NetworkFailure.NoNetworkConnection(null)
        val (arrangement, registerDeviceViewModel) = Arrangement()
            .withRegisterClientReturning(RegisterClientResult.Failure.InvalidCredentials.Invalid2FA)
            .withGetSelfReturning(SELF_USER.copy(email = email))
            .withRequestSecondFactorVerificationCodeReturning(RequestSecondFactorVerificationCodeUseCase.Result.Failure.Generic(failure))
            .arrange()

        registerDeviceViewModel.onContinue()
        advanceUntilIdle()

        assertEquals(false, registerDeviceViewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        assertInstanceOf<RegisterDeviceFlowState.Error.GenericError>(registerDeviceViewModel.state.flowState).let {
            assertEquals(failure, it.coreFailure)
        }
    }

    @Test
    fun `given 2fa code fully entered, when requesting 2fa code, then execute registration with given code`() = runTest {
        val email = "user@email.com"
        val (arrangement, registerDeviceViewModel) = Arrangement()
            .withRegisterClientReturning(RegisterClientResult.Failure.InvalidCredentials.Missing2FA)
            .withGetSelfReturning(SELF_USER.copy(email = email))
            .withRequestSecondFactorVerificationCodeReturning(RequestSecondFactorVerificationCodeUseCase.Result.Success)
            .arrange()
        registerDeviceViewModel.onContinue()
        advanceUntilIdle()

        registerDeviceViewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd("12345") // not fully entered
        advanceUntilIdle()
        coVerify(exactly = 0) {
            arrangement.registerClientUseCase(match { it.secondFactorVerificationCode == "12345" })
        }

        registerDeviceViewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd("123456") // fully entered
        advanceUntilIdle()
        coVerify(exactly = 1) {
            arrangement.registerClientUseCase(match { it.secondFactorVerificationCode == "123456" })
        }
    }

    inner class Arrangement {

        @MockK
        internal lateinit var registerClientUseCase: GetOrRegisterClientUseCase

        @MockK
        internal lateinit var isPasswordRequiredUseCase: IsPasswordRequiredUseCase

        @MockK
        internal lateinit var getSelfUserUseCase: GetSelfUserUseCase

        @MockK
        internal lateinit var requestSecondFactorVerificationCodeUseCase: RequestSecondFactorVerificationCodeUseCase

        @MockK
        internal lateinit var userDataStore: UserDataStore

        @MockK
        internal lateinit var countdownTimer: CountdownTimer

        init {
            MockKAnnotations.init(this)
            mockUri()
            coEvery { isPasswordRequiredUseCase() } returns IsPasswordRequiredUseCase.Result.Success(true)
            every { userDataStore.initialSyncCompleted } returns flowOf(true)
            coEvery { getSelfUserUseCase() } returns SELF_USER
            coEvery { countdownTimer.start(any(), any(), any()) } returns Unit
        }

        fun arrange() = this to RegisterDeviceViewModel(
            registerClientUseCase,
            isPasswordRequiredUseCase,
            userDataStore,
            getSelfUserUseCase,
            requestSecondFactorVerificationCodeUseCase,
            countdownTimer,
        )

        fun withRegisterClientReturning(result: RegisterClientResult) = apply {
            coEvery { registerClientUseCase(any()) } returns result
        }

        fun withGetSelfReturning(result: SelfUser) = apply {
            coEvery { getSelfUserUseCase() } returns result
        }

        fun withRequestSecondFactorVerificationCodeReturning(result: RequestSecondFactorVerificationCodeUseCase.Result) = apply {
            coEvery {
                requestSecondFactorVerificationCodeUseCase(any(), any())
            } returns result
        }
    }

    companion object {
        val CLIENT = TestClient.CLIENT
        val SELF_USER = TestUser.SELF_USER
    }
}
