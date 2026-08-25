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

package com.wire.android.ui.authentication.devices.register

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterDeviceViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var snapshotWriteObserver: ObserverHandle

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        snapshotWriteObserver = Snapshot.registerGlobalWriteObserver {
            Snapshot.sendApplyNotifications()
        }
    }

    @AfterEach
    fun tearDown() {
        snapshotWriteObserver.dispose()
        Dispatchers.resetMain()
    }

    @Test
    fun `password-required init waits for user input and password text controls continue`() = runTest(dispatcher) {
        val (gateway, _, viewModel) = arrange()

        assertEquals(RegisterDeviceFlowState.Default, viewModel.state.flowState)
        assertTrue(gateway.registerRequests.isEmpty())
        assertFalse(viewModel.state.continueEnabled)

        advanceUntilIdle()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")
        advanceUntilIdle()
        assertTrue(viewModel.state.continueEnabled)

        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("")
        advanceUntilIdle()
        assertFalse(viewModel.state.continueEnabled)
    }

    @Test
    fun `passwordless init automatically registers with a null password`() = runTest(dispatcher) {
        val success = RegisterDeviceResult.Success<String>(initialSyncCompleted = true, isE2EIRequired = false)
        val (gateway, _, viewModel) = arrange(
            passwordRequirement = PasswordRequirement.NotRequired,
            registerResults = listOf(success),
        )

        assertEquals(listOf(RegisterDeviceRequest(null, null)), gateway.registerRequests)
        assertEquals(
            RegisterDeviceFlowState.Success<String>(initialSyncCompleted = true, isE2EIRequired = false),
            viewModel.state.flowState,
        )
    }

    @Test
    fun `password requirement failure preserves every semantic classification`() = runTest(dispatcher) {
        AuthenticationFailure.entries.forEach { failure ->
            val (_, _, viewModel) = arrange(passwordRequirement = PasswordRequirement.Failure(failure))

            assertEquals(RegisterDeviceFlowState.Error.GenericError(failure), viewModel.state.flowState)
        }
    }

    @Test
    fun `successful registrations preserve initial sync and E2EI session values`() = runTest(dispatcher) {
        val sessionId = "user@domain"
        listOf(
            RegisterDeviceResult.Success<String>(initialSyncCompleted = true, isE2EIRequired = false),
            RegisterDeviceResult.Success<String>(
                initialSyncCompleted = false,
                isE2EIRequired = true,
                e2eiSessionId = sessionId,
            ),
        ).forEach { result ->
            val (_, _, viewModel) = arrange(registerResults = listOf(result))

            advanceUntilIdle()
            viewModel.onContinue()
            advanceUntilIdle()

            assertEquals(
                RegisterDeviceFlowState.Success(
                    initialSyncCompleted = result.initialSyncCompleted,
                    isE2EIRequired = result.isE2EIRequired,
                    e2eiSessionId = result.e2eiSessionId,
                ),
                viewModel.state.flowState,
            )
        }
    }

    @Test
    fun `too many devices and invalid password preserve terminal and field error states`() = runTest(dispatcher) {
        val (_, _, tooManyViewModel) = arrange(registerResults = listOf(RegisterDeviceResult.TooManyDevices))
        advanceUntilIdle()
        tooManyViewModel.onContinue()
        advanceUntilIdle()
        assertEquals(RegisterDeviceFlowState.TooManyDevices, tooManyViewModel.state.flowState)

        val (_, _, invalidPasswordViewModel) = arrange(
            registerResults = listOf(RegisterDeviceResult.InvalidCredentials)
        )
        advanceUntilIdle()
        invalidPasswordViewModel.onContinue()
        advanceUntilIdle()
        assertEquals(
            RegisterDeviceFlowState.Error.InvalidCredentialsError,
            invalidPasswordViewModel.state.flowState,
        )
        assertTrue(invalidPasswordViewModel.state.continueEnabled)
    }

    @Test
    fun `generic registration failures preserve every semantic classification and dismiss`() = runTest(dispatcher) {
        AuthenticationFailure.entries.forEach { failure ->
            val (_, _, viewModel) = arrange(registerResults = listOf(RegisterDeviceResult.Failure(failure)))

            advanceUntilIdle()
            viewModel.onContinue()
            advanceUntilIdle()

            assertEquals(RegisterDeviceFlowState.Error.GenericError(failure), viewModel.state.flowState)
            assertTrue(viewModel.state.continueEnabled)
            viewModel.onErrorDismiss()
            assertEquals(RegisterDeviceFlowState.Default, viewModel.state.flowState)
        }
    }

    @Test
    fun `missing second factor requests a code and starts the exact resend duration`() = runTest(dispatcher) {
        val (gateway, timer, viewModel) = arrange(
            registerResults = listOf(RegisterDeviceResult.MissingSecondFactor),
            verificationResults = listOf(RequestVerificationCodeResult.Sent("member@example.com")),
        )

        advanceUntilIdle()
        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(1, gateway.verificationRequestCount)
        assertTrue(viewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        assertEquals("member@example.com", viewModel.secondFactorVerificationCodeState.emailUsed)
        assertEquals(RegisterDeviceFlowState.Default, viewModel.state.flowState)
        assertEquals(listOf(300L), timer.startedSeconds)
    }

    @Test
    fun `invalid reused second factor requests a new code without showing invalid state`() = runTest(dispatcher) {
        val (gateway, _, viewModel) = arrange(
            registerResults = listOf(RegisterDeviceResult.InvalidSecondFactor),
            verificationResults = listOf(RequestVerificationCodeResult.Sent("member@example.com")),
        )

        advanceUntilIdle()
        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(1, gateway.verificationRequestCount)
        assertTrue(viewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        assertFalse(viewModel.secondFactorVerificationCodeState.isCurrentCodeInvalid)
    }

    @Test
    fun `invalid entered second factor is marked invalid without requesting another code`() = runTest(dispatcher) {
        val (gateway, _, viewModel) = arrange(
            registerResults = listOf(RegisterDeviceResult.InvalidSecondFactor),
        )

        advanceUntilIdle()
        viewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd("123456")
        advanceUntilIdle()

        assertEquals(RegisterDeviceRequest("", "123456"), gateway.registerRequests.single())
        assertEquals(0, gateway.verificationRequestCount)
        assertTrue(viewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        assertTrue(viewModel.secondFactorVerificationCodeState.isCurrentCodeInvalid)
    }

    @Test
    fun `resend success and too-many responses both allow code input and start timer`() = runTest(dispatcher) {
        listOf(
            RequestVerificationCodeResult.Sent("member@example.com"),
            RequestVerificationCodeResult.TooManyRequests("member@example.com"),
        ).forEach { result ->
            val (_, timer, viewModel) = arrange(verificationResults = listOf(result))

            advanceUntilIdle()
            viewModel.onCodeResend()
            advanceUntilIdle()

            assertTrue(viewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
            assertEquals("member@example.com", viewModel.secondFactorVerificationCodeState.emailUsed)
            assertEquals(listOf(300L), timer.startedSeconds)
        }
    }

    @Test
    fun `resend generic failure shows classified error without code input`() = runTest(dispatcher) {
        val (_, timer, viewModel) = arrange(
            verificationResults = listOf(RequestVerificationCodeResult.Failure(AuthenticationFailure.NoNetwork))
        )

        advanceUntilIdle()
        viewModel.onCodeResend()
        advanceUntilIdle()

        assertFalse(viewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        assertEquals(
            RegisterDeviceFlowState.Error.GenericError(AuthenticationFailure.NoNetwork),
            viewModel.state.flowState,
        )
        assertTrue(timer.startedSeconds.isEmpty())
    }

    @Test
    fun `missing email preserves the current loading semantics`() = runTest(dispatcher) {
        val (_, timer, viewModel) = arrange(
            registerResults = listOf(RegisterDeviceResult.MissingSecondFactor),
            verificationResults = listOf(RequestVerificationCodeResult.MissingEmail),
        )

        advanceUntilIdle()
        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(RegisterDeviceFlowState.Loading, viewModel.state.flowState)
        assertFalse(viewModel.state.continueEnabled)
        assertFalse(viewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        assertTrue(timer.startedSeconds.isEmpty())
    }

    @Test
    fun `only a full six-digit code auto-submits with the current password`() = runTest(dispatcher) {
        val (gateway, _, viewModel) = arrange(registerResults = listOf(RegisterDeviceResult.PasswordRequired))
        advanceUntilIdle()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")

        viewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd("12345")
        advanceUntilIdle()
        assertTrue(gateway.registerRequests.isEmpty())

        viewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd("123456")
        advanceUntilIdle()
        assertEquals(RegisterDeviceRequest("password", "123456"), gateway.registerRequests.single())
    }

    @Test
    fun `timer updates and finish are reflected while back resets only code-entry fields`() = runTest(dispatcher) {
        val (_, timer, viewModel) = arrange(
            verificationResults = listOf(RequestVerificationCodeResult.Sent("member@example.com"))
        )
        advanceUntilIdle()
        viewModel.onCodeResend()
        advanceUntilIdle()

        timer.update("04:59")
        assertEquals("04:59", viewModel.secondFactorVerificationCodeState.remainingTimerText)

        viewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd("123")
        advanceUntilIdle()
        viewModel.onCodeVerificationBackPress()
        advanceUntilIdle()

        assertEquals("", viewModel.secondFactorVerificationCodeTextState.text.toString())
        assertFalse(viewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        assertFalse(viewModel.secondFactorVerificationCodeState.isCurrentCodeInvalid)
        assertEquals("", viewModel.secondFactorVerificationCodeState.emailUsed)
        assertEquals("04:59", viewModel.secondFactorVerificationCodeState.remainingTimerText)

        timer.finish()
        assertNull(viewModel.secondFactorVerificationCodeState.remainingTimerText)
    }

    private fun arrange(
        passwordRequirement: PasswordRequirement = PasswordRequirement.Required,
        registerResults: List<RegisterDeviceResult<String>> = emptyList(),
        verificationResults: List<RequestVerificationCodeResult> = emptyList(),
    ): Triple<FakeRegisterDeviceGateway, FakeRegisterDeviceResendTimer, RegisterDeviceViewModel<String>> {
        val gateway = FakeRegisterDeviceGateway(passwordRequirement, registerResults, verificationResults)
        val timer = FakeRegisterDeviceResendTimer()
        return Triple(gateway, timer, RegisterDeviceViewModel(gateway, timer))
    }

    private class FakeRegisterDeviceGateway(
        private val passwordRequirementResult: PasswordRequirement,
        registerResults: List<RegisterDeviceResult<String>>,
        verificationResults: List<RequestVerificationCodeResult>,
    ) : RegisterDeviceGateway<String> {
        private val remainingRegisterResults = ArrayDeque(registerResults)
        private val remainingVerificationResults = ArrayDeque(verificationResults)

        val registerRequests = mutableListOf<RegisterDeviceRequest>()
        var verificationRequestCount = 0
            private set

        override suspend fun passwordRequirement(): PasswordRequirement = passwordRequirementResult

        override suspend fun registerClient(request: RegisterDeviceRequest): RegisterDeviceResult<String> {
            registerRequests += request
            return if (remainingRegisterResults.isEmpty()) {
                RegisterDeviceResult.PasswordRequired
            } else {
                remainingRegisterResults.removeFirst()
            }
        }

        override suspend fun requestVerificationCode(): RequestVerificationCodeResult {
            verificationRequestCount++
            return if (remainingVerificationResults.isEmpty()) {
                RequestVerificationCodeResult.MissingEmail
            } else {
                remainingVerificationResults.removeFirst()
            }
        }
    }

    private class FakeRegisterDeviceResendTimer : RegisterDeviceResendTimer {
        val startedSeconds = mutableListOf<Long>()
        private var onUpdate: ((String) -> Unit)? = null
        private var onFinish: (() -> Unit)? = null

        override suspend fun start(seconds: Long, onUpdate: (String) -> Unit, onFinish: () -> Unit) {
            startedSeconds += seconds
            this.onUpdate = onUpdate
            this.onFinish = onFinish
        }

        fun update(value: String) {
            requireNotNull(onUpdate).invoke(value)
        }

        fun finish() {
            requireNotNull(onFinish).invoke()
        }
    }
}
