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
package com.wire.android.ui.authentication.devices.remove

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import com.wire.android.ui.authentication.devices.register.AuthenticationFailure
import com.wire.android.ui.authentication.devices.register.PasswordRequirement
import com.wire.android.ui.authentication.devices.register.RegisterDeviceRequest
import com.wire.android.ui.authentication.devices.register.RegisterDeviceResult
import com.wire.android.ui.authentication.devices.register.RequestVerificationCodeResult
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoveDeviceViewModelTest {

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
    fun `initial fetch and retry preserve loading list and init error semantics`() = runTest(dispatcher) {
        val firstDevice = TestDevice("first")
        val secondDevice = TestDevice("second")
        val (gateway, viewModel) = arrange(
            fetchResults = listOf(
                FetchPermanentDevicesResult.Failure(AuthenticationFailure.NoNetwork),
                FetchPermanentDevicesResult.Success(listOf(firstDevice, secondDevice)),
            )
        )

        advanceUntilIdle()
        assertFalse(viewModel.state.isLoadingClientsList)
        assertEquals(RemoveDeviceAuthenticationError.InitError, viewModel.state.error)
        assertEquals(emptyList<TestDevice>(), viewModel.state.deviceList)

        viewModel.retryFetch()
        assertTrue(viewModel.state.isLoadingClientsList)
        assertEquals(RemoveDeviceAuthenticationError.None, viewModel.state.error)
        advanceUntilIdle()

        assertEquals(2, gateway.fetchCount)
        assertFalse(viewModel.state.isLoadingClientsList)
        assertEquals(listOf(firstDevice, secondDevice), viewModel.state.deviceList)
        assertEquals(RemoveDeviceAuthenticationDialogState.Hidden, viewModel.state.removeDeviceDialogState)
    }

    @Test
    fun `password requirement failure is visible even while password dialog is hidden`() = runTest(dispatcher) {
        AuthenticationFailure.entries.forEach { failure ->
            val (_, viewModel) = arrange(passwordRequirements = listOf(PasswordRequirement.Failure(failure)))
            advanceUntilIdle()

            viewModel.onItemClicked(device)
            advanceUntilIdle()

            assertEquals(RemoveDeviceAuthenticationDialogState.Hidden, viewModel.state.removeDeviceDialogState)
            assertEquals(RemoveDeviceAuthenticationError.GenericError(failure), viewModel.state.error)
            viewModel.clearDeleteClientError()
            assertEquals(RemoveDeviceAuthenticationError.None, viewModel.state.error)
        }
    }

    @Test
    fun `required password opens a clean dialog while passwordless removal deletes directly`() = runTest(dispatcher) {
        val (requiredGateway, requiredViewModel) = arrange()
        advanceUntilIdle()
        requiredViewModel.passwordTextState.setTextAndPlaceCursorAtEnd("stale")
        advanceUntilIdle()

        requiredViewModel.onItemClicked(device)
        advanceUntilIdle()

        assertEquals("", requiredViewModel.passwordTextState.text.toString())
        assertEquals(device, visible(requiredViewModel).device)
        assertTrue(requiredGateway.deleteRequests.isEmpty())

        val (passwordlessGateway, passwordlessViewModel) = arrange(
            passwordRequirements = listOf(PasswordRequirement.NotRequired),
            deleteResults = listOf(DeleteDeviceResult.InvalidCredentials),
        )
        advanceUntilIdle()
        passwordlessViewModel.onItemClicked(device)
        advanceUntilIdle()

        assertEquals(listOf(DeleteRequest(null, device)), passwordlessGateway.deleteRequests)
        assertEquals(
            RemoveDeviceAuthenticationError.InvalidCredentialsError,
            passwordlessViewModel.state.error,
        )
    }

    @Test
    fun `password edits enable removal and clear invalid credentials only while dialog is visible`() = runTest(dispatcher) {
        val (_, viewModel) = arrange(deleteResults = listOf(DeleteDeviceResult.InvalidCredentials))
        advanceUntilIdle()
        viewModel.onItemClicked(device)
        advanceUntilIdle()

        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("secret")
        advanceUntilIdle()
        assertTrue(visible(viewModel).removeEnabled)

        viewModel.onRemoveConfirmed()
        advanceUntilIdle()
        assertEquals(RemoveDeviceAuthenticationError.InvalidCredentialsError, viewModel.state.error)
        assertFalse(visible(viewModel).loading)

        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("corrected")
        advanceUntilIdle()
        assertEquals(RemoveDeviceAuthenticationError.None, viewModel.state.error)
        assertTrue(visible(viewModel).removeEnabled)

        viewModel.onDialogDismissed()
        advanceUntilIdle()
        assertEquals("", viewModel.passwordTextState.text.toString())
        assertEquals(RemoveDeviceAuthenticationDialogState.Hidden, viewModel.state.removeDeviceDialogState)

        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("ignored")
        advanceUntilIdle()
        assertEquals(RemoveDeviceAuthenticationDialogState.Hidden, viewModel.state.removeDeviceDialogState)
    }

    @Test
    fun `successful delete waits exactly two seconds before registering with the same password`() = runTest(dispatcher) {
        val (gateway, viewModel) = arrange(
            deleteResults = listOf(DeleteDeviceResult.Success),
            registerResults = listOf(RegisterDeviceResult.PasswordRequired),
        )
        advanceUntilIdle()
        viewModel.onItemClicked(device)
        advanceUntilIdle()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("secret")
        advanceUntilIdle()

        viewModel.onRemoveConfirmed()
        runCurrent()
        assertEquals(listOf("delete"), gateway.events)
        assertTrue(visible(viewModel).loading)
        assertFalse(visible(viewModel).removeEnabled)

        advanceTimeBy(RemoveDeviceViewModel.REGISTER_CLIENT_AFTER_DELETE_DELAY_MILLIS - 1)
        runCurrent()
        assertTrue(gateway.registerRequests.isEmpty())

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("delete", "register"), gateway.events)
        assertEquals(listOf(RegisterDeviceRequest("secret", null)), gateway.registerRequests)
        assertFalse(visible(viewModel).loading)
    }

    @Test
    fun `delete failures preserve invalid password required and generic branches`() = runTest(dispatcher) {
        val (_, invalidViewModel) = arrange(deleteResults = listOf(DeleteDeviceResult.InvalidCredentials))
        advanceUntilIdle()
        invalidViewModel.onItemClicked(device)
        advanceUntilIdle()
        invalidViewModel.onRemoveConfirmed()
        advanceUntilIdle()
        assertEquals(RemoveDeviceAuthenticationError.InvalidCredentialsError, invalidViewModel.state.error)

        val (_, requiredViewModel) = arrange(
            passwordRequirements = listOf(PasswordRequirement.NotRequired),
            deleteResults = listOf(DeleteDeviceResult.PasswordRequired),
        )
        advanceUntilIdle()
        requiredViewModel.onItemClicked(device)
        advanceUntilIdle()
        assertEquals(device, visible(requiredViewModel).device)
        assertFalse(visible(requiredViewModel).loading)

        AuthenticationFailure.entries.forEach { failure ->
            val (_, genericViewModel) = arrange(deleteResults = listOf(DeleteDeviceResult.Failure(failure)))
            advanceUntilIdle()
            genericViewModel.onItemClicked(device)
            advanceUntilIdle()
            genericViewModel.onRemoveConfirmed()
            advanceUntilIdle()
            assertEquals(RemoveDeviceAuthenticationError.GenericError(failure), genericViewModel.state.error)
            assertFalse(visible(genericViewModel).loading)
        }
    }

    @Test
    fun `registration success emits exact completion values after delete`() = runTest(dispatcher) {
        listOf(
            RegisterDeviceResult.Success<Nothing>(initialSyncCompleted = true, isE2EIRequired = false),
            RegisterDeviceResult.Success<Nothing>(initialSyncCompleted = false, isE2EIRequired = true),
        ).forEach { result ->
            val (_, viewModel) = arrange(
                passwordRequirements = listOf(PasswordRequirement.NotRequired),
                deleteResults = listOf(DeleteDeviceResult.Success),
                registerResults = listOf(result),
            )
            advanceUntilIdle()
            val action = async(start = CoroutineStart.UNDISPATCHED) { viewModel.actions.first() }

            viewModel.onItemClicked(device)
            advanceUntilIdle()

            assertEquals(OnComplete(result.initialSyncCompleted, result.isE2EIRequired), action.await())
        }
    }

    @Test
    fun `too many devices reloads the list and hides the dialog`() = runTest(dispatcher) {
        val replacement = TestDevice("replacement")
        val (gateway, viewModel) = arrange(
            fetchResults = listOf(
                FetchPermanentDevicesResult.Success(listOf(device)),
                FetchPermanentDevicesResult.Success(listOf(replacement)),
            ),
            deleteResults = listOf(DeleteDeviceResult.Success),
            registerResults = listOf(RegisterDeviceResult.TooManyDevices),
        )
        advanceUntilIdle()
        viewModel.onItemClicked(device)
        advanceUntilIdle()
        viewModel.onRemoveConfirmed()
        advanceUntilIdle()

        assertEquals(2, gateway.fetchCount)
        assertEquals(listOf(replacement), viewModel.state.deviceList)
        assertEquals(RemoveDeviceAuthenticationDialogState.Hidden, viewModel.state.removeDeviceDialogState)
    }

    @Test
    fun `missing second factor requests code and sent or throttled responses allow input`() = runTest(dispatcher) {
        listOf(
            RequestVerificationCodeResult.Sent("member@example.com"),
            RequestVerificationCodeResult.TooManyRequests("member@example.com"),
        ).forEach { verificationResult ->
            val (gateway, viewModel) = arrange(
                deleteResults = listOf(DeleteDeviceResult.Success),
                registerResults = listOf(RegisterDeviceResult.MissingSecondFactor),
                verificationResults = listOf(verificationResult),
            )
            advanceUntilIdle()
            viewModel.onItemClicked(device)
            advanceUntilIdle()
            viewModel.onRemoveConfirmed()
            advanceUntilIdle()

            assertEquals(1, gateway.verificationRequestCount)
            assertTrue(viewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
            assertEquals("member@example.com", viewModel.secondFactorVerificationCodeState.emailUsed)
        }
    }

    @Test
    fun `full verification code auto-submits and invalid code stays visible and marked invalid`() = runTest(dispatcher) {
        val (gateway, viewModel) = arrange(registerResults = listOf(RegisterDeviceResult.InvalidSecondFactor))
        advanceUntilIdle()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("secret")
        viewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd("12345")
        advanceUntilIdle()
        assertTrue(gateway.registerRequests.isEmpty())

        viewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd("123456")
        advanceUntilIdle()

        assertEquals(listOf(RegisterDeviceRequest("secret", "123456")), gateway.registerRequests)
        assertFalse(viewModel.state.is2FAInProgress)
        assertTrue(viewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        assertTrue(viewModel.secondFactorVerificationCodeState.isCurrentCodeInvalid)
        assertEquals("123456", viewModel.secondFactorVerificationCodeTextState.text.toString())
    }

    @Test
    fun `registration invalid credentials and generic failures preserve exact errors`() = runTest(dispatcher) {
        val (_, invalidViewModel) = arrange(registerResults = listOf(RegisterDeviceResult.InvalidCredentials))
        advanceUntilIdle()
        invalidViewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd("123456")
        advanceUntilIdle()
        assertEquals(RemoveDeviceAuthenticationError.InvalidCredentialsError, invalidViewModel.state.error)

        AuthenticationFailure.entries.forEach { failure ->
            val (_, genericViewModel) = arrange(registerResults = listOf(RegisterDeviceResult.Failure(failure)))
            advanceUntilIdle()
            genericViewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd("123456")
            advanceUntilIdle()
            assertEquals(RemoveDeviceAuthenticationError.GenericError(failure), genericViewModel.state.error)
        }
    }

    @Test
    fun `resend handles sent throttled missing email and classified failure`() = runTest(dispatcher) {
        listOf(
            RequestVerificationCodeResult.Sent("sent@example.com") to "sent@example.com",
            RequestVerificationCodeResult.TooManyRequests("throttled@example.com") to "throttled@example.com",
        ).forEach { (result, email) ->
            val (_, viewModel) = arrange(verificationResults = listOf(result))
            advanceUntilIdle()
            viewModel.onCodeResend()
            advanceUntilIdle()
            assertTrue(viewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
            assertEquals(email, viewModel.secondFactorVerificationCodeState.emailUsed)
        }

        val (_, missingEmailViewModel) = arrange(
            verificationResults = listOf(RequestVerificationCodeResult.MissingEmail)
        )
        advanceUntilIdle()
        missingEmailViewModel.onCodeResend()
        advanceUntilIdle()
        assertFalse(missingEmailViewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        assertEquals(RemoveDeviceAuthenticationError.None, missingEmailViewModel.state.error)

        AuthenticationFailure.entries.forEach { failure ->
            val (_, failureViewModel) = arrange(
                verificationResults = listOf(RequestVerificationCodeResult.Failure(failure))
            )
            advanceUntilIdle()
            failureViewModel.onCodeResend()
            advanceUntilIdle()
            assertEquals(RemoveDeviceAuthenticationError.GenericError(failure), failureViewModel.state.error)
            assertFalse(failureViewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        }
    }

    @Test
    fun `verification back clears and hides code without changing the device flow`() = runTest(dispatcher) {
        val (_, viewModel) = arrange(
            verificationResults = listOf(RequestVerificationCodeResult.Sent("member@example.com"))
        )
        advanceUntilIdle()
        viewModel.onCodeResend()
        advanceUntilIdle()
        viewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd("123")
        advanceUntilIdle()

        viewModel.onCodeVerificationBackPress()
        advanceUntilIdle()

        assertEquals("", viewModel.secondFactorVerificationCodeTextState.text.toString())
        assertFalse(viewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        assertEquals("", viewModel.secondFactorVerificationCodeState.emailUsed)
        assertEquals(listOf(device), viewModel.state.deviceList)
    }

    private fun arrange(
        fetchResults: List<FetchPermanentDevicesResult<TestDevice>> = listOf(
            FetchPermanentDevicesResult.Success(listOf(device))
        ),
        passwordRequirements: List<PasswordRequirement> = listOf(PasswordRequirement.Required),
        deleteResults: List<DeleteDeviceResult> = emptyList(),
        registerResults: List<RegisterDeviceResult<Nothing>> = emptyList(),
        verificationResults: List<RequestVerificationCodeResult> = emptyList(),
    ): Pair<FakeRemoveDeviceGateway, RemoveDeviceViewModel<TestDevice>> {
        val gateway = FakeRemoveDeviceGateway(
            fetchResults,
            passwordRequirements,
            deleteResults,
            registerResults,
            verificationResults,
        )
        return gateway to RemoveDeviceViewModel(gateway)
    }

    private fun visible(
        viewModel: RemoveDeviceViewModel<TestDevice>
    ): RemoveDeviceAuthenticationDialogState.Visible<TestDevice> =
        viewModel.state.removeDeviceDialogState as RemoveDeviceAuthenticationDialogState.Visible<TestDevice>

    private data class TestDevice(val id: String)
    private data class DeleteRequest(val password: String?, val device: TestDevice)

    private class FakeRemoveDeviceGateway(
        fetchResults: List<FetchPermanentDevicesResult<TestDevice>>,
        passwordRequirements: List<PasswordRequirement>,
        deleteResults: List<DeleteDeviceResult>,
        registerResults: List<RegisterDeviceResult<Nothing>>,
        verificationResults: List<RequestVerificationCodeResult>,
    ) : RemoveDeviceGateway<TestDevice> {
        private val remainingFetchResults = ArrayDeque(fetchResults)
        private val remainingPasswordRequirements = ArrayDeque(passwordRequirements)
        private val remainingDeleteResults = ArrayDeque(deleteResults)
        private val remainingRegisterResults = ArrayDeque(registerResults)
        private val remainingVerificationResults = ArrayDeque(verificationResults)

        var fetchCount: Int = 0
            private set
        var verificationRequestCount: Int = 0
            private set
        val deleteRequests = mutableListOf<DeleteRequest>()
        val registerRequests = mutableListOf<RegisterDeviceRequest>()
        val events = mutableListOf<String>()

        override suspend fun fetchPermanentDevices(): FetchPermanentDevicesResult<TestDevice> {
            fetchCount++
            return remainingFetchResults.removeFirst()
        }

        override suspend fun passwordRequirement(): PasswordRequirement =
            if (remainingPasswordRequirements.isEmpty()) PasswordRequirement.Required else remainingPasswordRequirements.removeFirst()

        override suspend fun deleteDevice(password: String?, device: TestDevice): DeleteDeviceResult {
            events += "delete"
            deleteRequests += DeleteRequest(password, device)
            return if (remainingDeleteResults.isEmpty()) {
                DeleteDeviceResult.InvalidCredentials
            } else {
                remainingDeleteResults.removeFirst()
            }
        }

        override suspend fun registerClient(request: RegisterDeviceRequest): RegisterDeviceResult<Nothing> {
            events += "register"
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

    private companion object {
        val device = TestDevice("device")
    }
}
