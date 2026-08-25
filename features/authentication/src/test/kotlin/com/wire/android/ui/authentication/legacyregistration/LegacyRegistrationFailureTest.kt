/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration

import androidx.compose.runtime.snapshots.Snapshot
import com.wire.android.ui.authentication.legacyregistration.code.LegacyCodeActivationResult
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegisterClientResult
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeGateway
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeInput
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeState
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeViewModel
import com.wire.android.ui.authentication.legacyregistration.code.LegacyPersonalRegistrationRequest
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationResult
import com.wire.android.ui.authentication.legacyregistration.code.LegacyStoreSessionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LegacyRegistrationFailureTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `resend maps a typed email failure without leaving loading`() = runTest(dispatcher) {
        val viewModel = viewModel(gateway = FailureGateway(activation = LegacyCodeActivationResult.InvalidEmail))

        advanceUntilIdle()
        viewModel.resendCode()
        advanceUntilIdle()

        assertEquals(LegacyRegistrationCodeState.Result.InvalidEmail, viewModel.state.result)
        assertEquals(false, viewModel.state.loading)
    }

    @Test
    fun `registration failure preserves generic identity and does not store a session`() = runTest(dispatcher) {
        val gateway = FailureGateway(registration = LegacyRegistrationResult.Generic("registration failed"))
        val viewModel = viewModel(gateway)

        advanceUntilIdle()
        viewModel.codeTextState.edit { append("123456") }
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertEquals(LegacyRegistrationCodeState.Result.Generic("registration failed"), viewModel.state.result)
        assertEquals(listOf("register"), gateway.calls)
        assertEquals(false, viewModel.state.loading)
    }

    private fun viewModel(gateway: FailureGateway) = LegacyRegistrationCodeViewModel(
        input = LegacyRegistrationCodeInput<String>(null, "user@wire.test", "Jane", "password"),
        defaultServerConfig = "default",
        gateway = gateway,
    )

    private class FailureGateway(
        private val activation: LegacyCodeActivationResult<String> = LegacyCodeActivationResult.Sent,
        private val registration: LegacyRegistrationResult<String, String> = LegacyRegistrationResult.Success("credentials"),
    ) : LegacyRegistrationCodeGateway<String, String, String, String> {
        val calls = mutableListOf<String>()

        override suspend fun requestActivationCode(serverConfig: String, email: String) = activation

        override suspend fun register(
            serverConfig: String,
            request: LegacyPersonalRegistrationRequest,
        ): LegacyRegistrationResult<String, String> {
            calls += "register"
            return registration
        }

        override suspend fun storeSession(credentials: String): LegacyStoreSessionResult<String, String> {
            calls += "store"
            return LegacyStoreSessionResult.Success("user-id")
        }

        override suspend fun registerClient(userId: String, password: String): LegacyRegisterClientResult<String> {
            calls += "client"
            return LegacyRegisterClientResult.Success
        }

        override suspend fun onCodeVerificationShown() = Unit

        override suspend fun onCodeVerificationFailed() = Unit
    }
}
