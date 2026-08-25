/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.code

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateAccountCodeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var snapshotWriteObserver: ObserverHandle

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        snapshotWriteObserver = Snapshot.registerGlobalWriteObserver { Snapshot.sendApplyNotifications() }
    }

    @AfterEach
    fun tearDown() {
        snapshotWriteObserver.dispose()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state preserves flow defaults and custom or default links`() = runTest(dispatcher) {
        val custom = TestLinks("custom")
        val withCustom = arrange(input = input(custom = custom))
        val withDefault = arrange()

        assertEquals(TestFlow.Personal, withCustom.flowType)
        assertSame(custom, withCustom.customServerConfig)
        assertSame(custom, withCustom.serverConfig)
        assertEquals(TestLinks("default"), withDefault.serverConfig)
        assertEquals(6, withDefault.codeState.codeLength)
        assertEquals("", withDefault.codeState.email)
        assertEquals(CreateAccountCodeResult.None, withDefault.codeState.result)
    }

    @Test
    fun `only exactly six digits auto-submit and loading changes synchronously`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()

        viewModel.codeTextState.setTextAndPlaceCursorAtEnd("12345")
        advanceUntilIdle()
        assertTrue(gateway.calls.isEmpty())

        viewModel.codeTextState.setTextAndPlaceCursorAtEnd("1234567")
        advanceUntilIdle()
        assertTrue(gateway.calls.isEmpty())

        viewModel.codeTextState.setTextAndPlaceCursorAtEnd("123456")
        advanceUntilIdle()
        assertEquals(listOf("register", "store", "client"), gateway.calls)
        assertTrue(viewModel.codeState.loading)
    }

    @Test
    fun `personal and team requests preserve all registration fields password and entered code`() = runTest(dispatcher) {
        val personalGateway = FakeGateway()
        val personal = arrange(gateway = personalGateway)
        advanceUntilIdle()
        personal.codeTextState.setTextAndPlaceCursorAtEnd("123456")
        advanceUntilIdle()
        val personalRequest = personalGateway.registrationRequests.single() as CreateAccountRegistrationRequest.Personal
        assertEquals("Alice", personalRequest.firstName)
        assertEquals("Wire", personalRequest.lastName)
        assertEquals("secret", personalRequest.password)
        assertEquals("alice@example.com", personalRequest.email)
        assertEquals("123456", personalRequest.activationCode())

        val teamGateway = FakeGateway()
        val team = arrange(input = input(flow = TestFlow.Team, isTeam = true), gateway = teamGateway)
        advanceUntilIdle()
        team.codeTextState.setTextAndPlaceCursorAtEnd("654321")
        advanceUntilIdle()
        val teamRequest = teamGateway.registrationRequests.single() as CreateAccountRegistrationRequest.Team
        assertEquals("Alice", teamRequest.firstName)
        assertEquals("Wire", teamRequest.lastName)
        assertEquals("secret", teamRequest.password)
        assertEquals("alice@example.com", teamRequest.email)
        assertEquals("654321", teamRequest.activationCode())
        assertEquals("Wire Team", teamRequest.teamName)
        assertEquals("default", teamRequest.teamIcon)
    }

    @Test
    fun `register store client order and both client successes leave loading true`() = runTest(dispatcher) {
        listOf(CreateAccountClientResult.Success, CreateAccountClientResult.E2EICertificateRequired).forEach { clientResult ->
            val gateway = FakeGateway(clientResult = clientResult)
            val viewModel = arrange(gateway = gateway)
            advanceUntilIdle()
            viewModel.codeTextState.setTextAndPlaceCursorAtEnd("123456")
            advanceUntilIdle()

            assertEquals(listOf("register", "store", "client"), gateway.calls)
            assertEquals(CreateAccountCodeResult.Success("user"), viewModel.codeState.result)
            assertTrue(viewModel.codeState.loading)
            assertEquals("secret", gateway.clientPasswords.single())
        }
    }

    @Test
    fun `auth scope unavailable from submit leaves loading and stops order`() = runTest(dispatcher) {
        val gateway = FakeGateway(registrationResult = AccountRegistrationResult.AuthScopeUnavailable)
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.codeTextState.setTextAndPlaceCursorAtEnd("123456")
        advanceUntilIdle()

        assertEquals(listOf("register"), gateway.calls)
        assertTrue(viewModel.codeState.loading)
        assertEquals(CreateAccountCodeResult.None, viewModel.codeState.result)
    }

    @Test
    fun `every typed registration failure clears loading and maps exactly`() = runTest(dispatcher) {
        val failure = TestFailure("register")
        listOf(
            AccountRegistrationResult.InvalidActivationCode to CreateAccountCodeResult.Error.TextFieldError.InvalidActivationCodeError,
            AccountRegistrationResult.AccountAlreadyExists to CreateAccountCodeResult.Error.DialogError.AccountAlreadyExistsError,
            AccountRegistrationResult.Blacklisted to CreateAccountCodeResult.Error.DialogError.BlackListedError,
            AccountRegistrationResult.DomainBlocked to CreateAccountCodeResult.Error.DialogError.EmailDomainBlockedError,
            AccountRegistrationResult.InvalidEmail to CreateAccountCodeResult.Error.DialogError.InvalidEmailError,
            AccountRegistrationResult.TeamMembersLimitReached to CreateAccountCodeResult.Error.DialogError.TeamMembersLimitError,
            AccountRegistrationResult.UserCreationRestricted to CreateAccountCodeResult.Error.DialogError.CreationRestrictedError,
            AccountRegistrationResult.Generic(failure) to CreateAccountCodeResult.Error.DialogError.GenericError(failure),
        ).forEach { (gatewayResult, expected) ->
            val viewModel = arrange(gateway = FakeGateway(registrationResult = gatewayResult))
            advanceUntilIdle()
            viewModel.codeTextState.setTextAndPlaceCursorAtEnd("123456")
            advanceUntilIdle()
            assertEquals(expected, viewModel.codeState.result)
            assertFalse(viewModel.codeState.loading)
        }
    }

    @Test
    fun `store failures stop client and preserve generic identity`() = runTest(dispatcher) {
        val failure = TestFailure("store")
        listOf(
            StoreAccountSessionResult.UserAlreadyExists to CreateAccountCodeResult.Error.DialogError.UserAlreadyExistsError,
            StoreAccountSessionResult.Generic(failure) to CreateAccountCodeResult.Error.DialogError.GenericError(failure),
        ).forEach { (storeResult, expected) ->
            val gateway = FakeGateway(storeResult = storeResult)
            val viewModel = arrange(gateway = gateway)
            advanceUntilIdle()
            viewModel.codeTextState.setTextAndPlaceCursorAtEnd("123456")
            advanceUntilIdle()
            assertEquals(listOf("register", "store"), gateway.calls)
            assertEquals(expected, viewModel.codeState.result)
            val generic = viewModel.codeState.result as? CreateAccountCodeResult.Error.DialogError.GenericError
            if (generic != null) assertSame(failure, generic.failure)
            assertFalse(viewModel.codeState.loading)
        }
    }

    @Test
    fun `client typed failures map and clear loading`() = runTest(dispatcher) {
        val failure = TestFailure("client")
        listOf(
            CreateAccountClientResult.TooManyDevices to CreateAccountCodeResult.Error.TooManyDevicesError("user"),
            CreateAccountClientResult.Generic(failure) to CreateAccountCodeResult.Error.DialogError.GenericError(failure),
        ).forEach { (clientResult, expected) ->
            val viewModel = arrange(gateway = FakeGateway(clientResult = clientResult))
            advanceUntilIdle()
            viewModel.codeTextState.setTextAndPlaceCursorAtEnd("123456")
            advanceUntilIdle()
            assertEquals(expected, viewModel.codeState.result)
            assertFalse(viewModel.codeState.loading)
        }
    }

    @Test
    fun `activation code is read after gateway suspension`() = runTest(dispatcher) {
        val scopeResolved = CompletableDeferred<Unit>()
        val releaseScope = CompletableDeferred<Unit>()
        val gateway = FakeGateway(
            beforeActivationCodeRead = {
                scopeResolved.complete(Unit)
                releaseScope.await()
            }
        )
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.codeTextState.setTextAndPlaceCursorAtEnd("123456")
        runCurrent()
        assertTrue(scopeResolved.isCompleted)

        viewModel.codeTextState.setTextAndPlaceCursorAtEnd("65432")
        runCurrent()
        releaseScope.complete(Unit)
        advanceUntilIdle()

        assertEquals("65432", gateway.consumedActivationCodes.single())
    }

    @Test
    fun `resend success uses exact email starts 300 second timer and updates then clears text`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val timer = FakeTimer()
        val viewModel = arrange(gateway = gateway, timer = timer)

        viewModel.resendCode()
        assertTrue(viewModel.codeState.loading)
        advanceUntilIdle()

        assertEquals(listOf(TestLinks("default") to "alice@example.com"), gateway.activationRequests)
        assertEquals(300L, timer.seconds)
        assertFalse(viewModel.codeState.loading)
        timer.onUpdate?.invoke("05:00")
        assertEquals("05:00", viewModel.codeState.remainingTimerText)
        timer.onFinish?.invoke()
        assertEquals(null, viewModel.codeState.remainingTimerText)
    }

    @Test
    fun `resend errors map generic identity and unavailable keeps loading`() = runTest(dispatcher) {
        val failure = TestFailure("resend")
        listOf(
            ActivationCodeRequestResult.AlreadyInUse to CreateAccountCodeResult.Error.DialogError.AccountAlreadyExistsError,
            ActivationCodeRequestResult.Blacklisted to CreateAccountCodeResult.Error.DialogError.BlackListedError,
            ActivationCodeRequestResult.DomainBlocked to CreateAccountCodeResult.Error.DialogError.EmailDomainBlockedError,
            ActivationCodeRequestResult.InvalidEmail to CreateAccountCodeResult.Error.DialogError.InvalidEmailError,
            ActivationCodeRequestResult.Generic(failure) to CreateAccountCodeResult.Error.DialogError.GenericError(failure),
        ).forEach { (resendResult, expected) ->
            val viewModel = arrange(gateway = FakeGateway(activationResult = resendResult))
            viewModel.resendCode()
            advanceUntilIdle()
            assertEquals(expected, viewModel.codeState.result)
            assertFalse(viewModel.codeState.loading)
        }

        val unavailable = arrange(gateway = FakeGateway(activationResult = ActivationCodeRequestResult.AuthScopeUnavailable))
        unavailable.resendCode()
        advanceUntilIdle()
        assertTrue(unavailable.codeState.loading)
    }

    @Test
    fun `clear result and field preserve other state`() = runTest(dispatcher) {
        val viewModel = arrange(gateway = FakeGateway(activationResult = ActivationCodeRequestResult.InvalidEmail))
        advanceUntilIdle()
        viewModel.codeTextState.setTextAndPlaceCursorAtEnd("123")
        advanceUntilIdle()
        viewModel.resendCode()
        advanceUntilIdle()

        viewModel.clearCodeError()
        assertEquals(CreateAccountCodeResult.None, viewModel.codeState.result)
        viewModel.clearCodeField()
        assertEquals("", viewModel.codeTextState.text.toString())
    }

    private fun arrange(
        input: CreateAccountCodeInput<TestFlow, TestLinks> = input(),
        gateway: FakeGateway = FakeGateway(),
        timer: FakeTimer = FakeTimer(),
    ) = CreateAccountCodeViewModel(
        input = input,
        defaultServerConfig = TestLinks("default"),
        gateway = gateway,
        resendCodeTimer = timer,
    )

    private fun input(
        flow: TestFlow = TestFlow.Personal,
        custom: TestLinks? = null,
        isTeam: Boolean = false,
    ) = CreateAccountCodeInput(
        flowType = flow,
        customServerConfig = custom,
        email = "alice@example.com",
        firstName = "Alice",
        lastName = "Wire",
        password = "secret",
        teamName = "Wire Team",
        isTeam = isTeam,
    )

    private class FakeGateway(
        var activationResult: ActivationCodeRequestResult<TestFailure> = ActivationCodeRequestResult.Sent,
        var registrationResult: AccountRegistrationResult<TestFailure, TestCredentials> =
            AccountRegistrationResult.Success(TestCredentials),
        var storeResult: StoreAccountSessionResult<TestFailure, String> = StoreAccountSessionResult.Success("user"),
        var clientResult: CreateAccountClientResult<TestFailure> = CreateAccountClientResult.Success,
        var beforeActivationCodeRead: suspend () -> Unit = {},
    ) : CreateAccountCodeGateway<TestLinks, TestFailure, String, TestCredentials> {
        val calls = mutableListOf<String>()
        val registrationRequests = mutableListOf<CreateAccountRegistrationRequest>()
        val activationRequests = mutableListOf<Pair<TestLinks, String>>()
        val clientPasswords = mutableListOf<String>()
        val consumedActivationCodes = mutableListOf<String>()

        override suspend fun requestActivationCode(
            serverConfig: TestLinks,
            email: String,
        ) = activationResult.also { activationRequests += serverConfig to email }

        override suspend fun register(
            serverConfig: TestLinks,
            request: CreateAccountRegistrationRequest,
        ): AccountRegistrationResult<TestFailure, TestCredentials> {
            calls += "register"
            registrationRequests += request
            beforeActivationCodeRead()
            consumedActivationCodes += request.activationCode()
            return registrationResult
        }

        override suspend fun storeSession(credentials: TestCredentials) = storeResult.also { calls += "store" }

        override suspend fun registerClient(userId: String, password: String) = clientResult.also {
            calls += "client"
            clientPasswords += password
        }
    }

    private class FakeTimer : CreateAccountCodeResendTimer {
        var seconds: Long? = null
        var onUpdate: ((String) -> Unit)? = null
        var onFinish: (() -> Unit)? = null
        override suspend fun start(seconds: Long, onUpdate: (String) -> Unit, onFinish: () -> Unit) {
            this.seconds = seconds
            this.onUpdate = onUpdate
            this.onFinish = onFinish
        }
    }

    private enum class TestFlow { Personal, Team }
    private data class TestLinks(val name: String)
    private data class TestFailure(val message: String)
    private data object TestCredentials
}
