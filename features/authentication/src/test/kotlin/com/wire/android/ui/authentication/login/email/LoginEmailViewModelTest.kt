/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.login.email

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import com.wire.android.ui.authentication.login.LoginSavedInputStore
import com.wire.android.ui.authentication.login.LoginState
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private typealias TestViewModel = LoginEmailViewModel<String, TestFailure, String, String, String, String, String>

@OptIn(ExperimentalCoroutinesApi::class)
class LoginEmailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var snapshotObserver: ObserverHandle

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        snapshotObserver = Snapshot.registerGlobalWriteObserver { Snapshot.sendApplyNotifications() }
    }

    @AfterEach
    fun tearDown() {
        snapshotObserver.dispose()
        Dispatchers.resetMain()
    }

    @Test
    fun `prefill wins over saved input and exposes neutral domain claim`() = runTest(dispatcher) {
        val store = FakeStore("saved")
        val viewModel = arrange(store, prefilled = "prefilled", editable = false, domainClaim = "claimed")
        advanceUntilIdle()
        assertEquals("prefilled", viewModel.userIdentifierTextState.text.toString())
        assertFalse(viewModel.loginState.userIdentifierEnabled)
        assertEquals("claimed", viewModel.domainClaimedByOrg)
        viewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd("changed")
        advanceUntilIdle()
        assertEquals("changed", store.userIdentifier)
    }

    @Test
    fun `credentials enable login and clear resettable errors but not terminal states`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply { authentication = LoginEmailAuthenticationResult.InvalidCredentials }
        val viewModel = arrange(gateway = gateway)
        enterCredentials(viewModel)
        assertTrue(viewModel.loginState.loginEnabled)
        viewModel.login()
        advanceUntilIdle()
        assertTrue(viewModel.loginState.showInvalidCredentialsError)
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("new")
        advanceUntilIdle()
        assertTrue(viewModel.loginState.flowState is LoginState.Default)
        assertFalse(viewModel.loginState.showInvalidCredentialsError)

        gateway.authentication = LoginEmailAuthenticationResult.Success("session")
        gateway.client = LoginEmailClientResult.TooManyDevices
        viewModel.login()
        advanceUntilIdle()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("again")
        advanceUntilIdle()
        assertTrue(viewModel.loginState.flowState is LoginState.Error.TooManyDevicesError<*>)
    }

    @Test
    fun `login reads credentials lazily after scope and preserves exact success order`() = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>()
        val scopeStarted = CompletableDeferred<Unit>()
        val gateway = FakeGateway().apply {
            beforeScopeReturn = { scopeStarted.complete(Unit); release.await() }
            previousSession = "previous"
        }
        val viewModel = arrange(gateway = gateway)
        enterCredentials(viewModel, "first@example.com", "first-password")
        viewModel.login()
        dispatcher.scheduler.runCurrent()
        scopeStarted.await()
        enterCredentials(viewModel, "latest@example.com", "latest-password")
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals("latest@example.com", gateway.authIdentifiers.single())
        assertEquals("latest-password", gateway.authPasswords.single())
        assertEquals("latest@example.com", gateway.persistEmails.single())
        assertEquals("latest-password", gateway.clientPasswords.single())
        assertEquals(listOf("current", "scope", "auth", "store", "persist", "client"), gateway.events)
        assertEquals(LoginState.Success(true, false, "user"), viewModel.loginState.flowState)
    }

    @Test
    fun `only one prior job cancellation happens before the replacement job`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val firstStarted = CompletableDeferred<Unit>()
        val holdFirst = CompletableDeferred<Unit>()
        gateway.beforeAuthenticate = {
            if (gateway.authenticateCalls == 1) {
                firstStarted.complete(Unit)
                try { holdFirst.await() } finally { gateway.cancelledAuthentications++ }
            }
        }
        val viewModel = arrange(gateway = gateway)
        enterCredentials(viewModel)
        viewModel.login()
        runCurrent()
        firstStarted.await()
        viewModel.login()
        advanceUntilIdle()
        assertEquals(1, gateway.cancelledAuthentications)
        assertEquals(2, gateway.authenticateCalls)
    }

    @Test
    fun `username rejection happens before scope and all scope failures map`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply { emailValid = false }
        val viewModel = arrange(gateway = gateway)
        enterCredentials(viewModel, "handle", "password")
        viewModel.login(usernameAllowed = false)
        advanceUntilIdle()
        assertTrue(viewModel.loginState.flowState is LoginState.Error.TextFieldError.InvalidValue)
        assertEquals(0, gateway.scopeCalls)

        listOf(
            LoginEmailScopeResult.UnknownServerVersion to LoginState.Error.DialogError.ServerVersionNotSupported,
            LoginEmailScopeResult.ClientUpdateRequired to LoginState.Error.DialogError.ClientUpdateRequired,
            LoginEmailScopeResult.Failure(TestFailure("scope")) to LoginState.Error.DialogError.GenericError(TestFailure("scope")),
        ).forEach { (result, expected) ->
            gateway.emailValid = true
            gateway.scopeResult = result
            viewModel.login()
            advanceUntilIdle()
            assertEquals(expected, viewModel.loginState.flowState)
        }
    }

    @Test
    fun `authentication failures map and missing second factor reuses resolved scope`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val viewModel = arrange(gateway = gateway)
        enterCredentials(viewModel)
        val mappings = listOf(
            LoginEmailAuthenticationResult.ProxyError to LoginState.Error.DialogError.ProxyError,
            LoginEmailAuthenticationResult.InvalidCredentials to LoginState.Error.DialogError.InvalidCredentialsError,
            LoginEmailAuthenticationResult.InvalidIdentifier to LoginState.Error.TextFieldError.InvalidValue,
            LoginEmailAuthenticationResult.AccountSuspended to LoginState.Error.DialogError.AccountSuspended,
            LoginEmailAuthenticationResult.AccountPendingActivation to LoginState.Error.DialogError.AccountPendingActivation,
            LoginEmailAuthenticationResult.Failure(TestFailure("auth")) to LoginState.Error.DialogError.GenericError(TestFailure("auth")),
        )
        mappings.forEach { (result, expected) ->
            gateway.authentication = result
            viewModel.login()
            advanceUntilIdle()
            assertEquals(expected, viewModel.loginState.flowState)
        }
        gateway.authentication = LoginEmailAuthenticationResult.MissingSecondFactor
        viewModel.login()
        advanceUntilIdle()
        assertEquals(listOf("scope"), gateway.verificationScopes.takeLast(1))
        assertTrue(viewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
        assertEquals(300L, gateway.timerSeconds.single())
    }

    @Test
    fun `missing second factor rejects handles while too many requests still shows code`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply { authentication = LoginEmailAuthenticationResult.MissingSecondFactor }
        val viewModel = arrange(gateway = gateway)
        enterCredentials(viewModel, "handle", "password")
        viewModel.login()
        advanceUntilIdle()
        assertTrue(viewModel.loginState.flowState is LoginState.Error.DialogError.Request2FAWithHandle)
        assertTrue(gateway.verificationEmails.isEmpty())

        enterCredentials(viewModel, "  email@example.com  ", "password")
        gateway.verification = LoginEmailVerificationResult.TooManyRequests
        viewModel.login()
        advanceUntilIdle()
        assertEquals("email@example.com", gateway.verificationEmails.single())
        assertTrue(viewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
    }

    @Test
    fun `full six digit code auto submits invalid code marks state and back clears`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply { authentication = LoginEmailAuthenticationResult.InvalidSecondFactor }
        val viewModel = arrange(gateway = gateway)
        enterCredentials(viewModel)
        viewModel.secondFactorVerificationCodeTextState.setTextAndPlaceCursorAtEnd("123456")
        advanceUntilIdle()
        assertEquals(1, gateway.authenticateCalls)
        assertTrue(viewModel.secondFactorVerificationCodeState.isCurrentCodeInvalid)
        viewModel.onCodeVerificationBackPress()
        assertEquals("", viewModel.secondFactorVerificationCodeTextState.text.toString())
        assertFalse(viewModel.secondFactorVerificationCodeState.isCodeInputNecessary)
    }

    @Test
    fun `post-store failures rollback new session then restore previous while retained results do not`() = runTest(dispatcher) {
        val failure = TestFailure("persist")
        val gateway = FakeGateway().apply { previousSession = "previous"; persist = LoginEmailPersistResult.Failure(failure) }
        val viewModel = arrange(gateway = gateway)
        enterCredentials(viewModel)
        viewModel.login()
        advanceUntilIdle()
        assertEquals(listOf("user" to "previous"), gateway.reverts)
        assertSame(failure, (viewModel.loginState.flowState as LoginState.Error.DialogError.GenericError).coreFailure)

        listOf(
            LoginEmailClientResult.Success(false),
            LoginEmailClientResult.E2EICertificateRequired(false),
            LoginEmailClientResult.TooManyDevices,
        ).forEach { result ->
            val retained = FakeGateway().apply { client = result }
            val retainedViewModel = arrange(gateway = retained)
            enterCredentials(retainedViewModel)
            retainedViewModel.login()
            advanceUntilIdle()
            assertTrue(retained.reverts.isEmpty())
        }
    }

    @Test
    fun `resend resolves current proxy aware scope and timer updates then clears`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val viewModel = arrange(gateway = gateway)
        enterCredentials(viewModel)
        viewModel.proxyIdentifierTextState.setTextAndPlaceCursorAtEnd("proxy-user")
        viewModel.proxyPasswordTextState.setTextAndPlaceCursorAtEnd("proxy-pass")
        viewModel.onCodeResend()
        advanceUntilIdle()
        assertEquals(LoginEmailProxyCredentials("proxy-user", "proxy-pass"), gateway.proxyInputs.single())
        assertNull(viewModel.secondFactorVerificationCodeState.remainingTimerText)
        assertEquals(listOf("05:00"), gateway.timerUpdates)
    }

    @Test
    fun `backend invalid skips load while success updates config only after host side effects`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply { parsedBackend = null }
        val viewModel = arrange(gateway = gateway)
        viewModel.onBackendConfigLinkEntered("invalid")
        advanceUntilIdle()
        assertEquals(LoginEmailState.BackendConfigState.Error, viewModel.loginState.backendConfigState)
        assertEquals(0, gateway.backendLoads)

        gateway.parsedBackend = "request"
        gateway.backendResult = LoginEmailBackendResult.Success("custom")
        viewModel.onBackendConfigLinkEntered("valid")
        advanceUntilIdle()
        assertEquals("custom", viewModel.serverConfig)
        assertTrue(viewModel.isBackendConfigured)
        assertEquals(LoginEmailState.BackendConfigState.Success, viewModel.loginState.backendConfigState)
        viewModel.onBackendConfigSuccessContinue()
        assertEquals(LoginEmailState.BackendConfigState.Missing, viewModel.loginState.backendConfigState)
    }

    private fun arrange(
        store: FakeStore = FakeStore(),
        gateway: FakeGateway = FakeGateway(),
        prefilled: String? = null,
        editable: Boolean = true,
        domainClaim: String? = null,
    ) = LoginEmailViewModel(
        LoginEmailInput("default", true, prefilled, editable, domainClaim),
        store,
        gateway,
        FakeTimer(gateway),
    )

    private suspend fun enterCredentials(
        viewModel: TestViewModel,
        identifier: String = "email@example.com",
        password: String = "password",
    ) {
        viewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(identifier)
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)
        Snapshot.sendApplyNotifications()
        dispatcher.scheduler.runCurrent()
    }

    private class FakeGateway : LoginEmailGateway<String, TestFailure, String, String, String, String> {
        var emailValid = true
        var previousSession: String? = null
        var scopeResult: LoginEmailScopeResult<TestFailure, String> = LoginEmailScopeResult.Success("scope")
        var authentication: LoginEmailAuthenticationResult<TestFailure, String> = LoginEmailAuthenticationResult.Success("session")
        var store: LoginEmailStoreResult<TestFailure, String> = LoginEmailStoreResult.Success("user")
        var persist: LoginEmailPersistResult<TestFailure> = LoginEmailPersistResult.Success
        var client: LoginEmailClientResult<TestFailure> = LoginEmailClientResult.Success(true)
        var verification: LoginEmailVerificationResult<TestFailure> = LoginEmailVerificationResult.Sent
        var parsedBackend: String? = "request"
        var backendResult: LoginEmailBackendResult<String> = LoginEmailBackendResult.Success("custom")
        var beforeScopeReturn: suspend () -> Unit = {}
        var beforeAuthenticate: suspend () -> Unit = {}
        var scopeCalls = 0
        var authenticateCalls = 0
        var cancelledAuthentications = 0
        var backendLoads = 0
        val events = mutableListOf<String>()
        val authIdentifiers = mutableListOf<String>()
        val authPasswords = mutableListOf<String>()
        val persistEmails = mutableListOf<String>()
        val clientPasswords = mutableListOf<String>()
        val proxyInputs = mutableListOf<LoginEmailProxyCredentials?>()
        val verificationScopes = mutableListOf<String>()
        val verificationEmails = mutableListOf<String>()
        val reverts = mutableListOf<Pair<String?, String?>>()
        val timerSeconds = mutableListOf<Long>()
        val timerUpdates = mutableListOf<String>()

        override fun isProxyAuthRequired(serverConfig: String) = false
        override fun isEmail(value: String) = emailValid
        override suspend fun currentValidSession() = previousSession.also { events += "current" }
        override suspend fun resolveScope(serverConfig: String, proxyCredentials: () -> LoginEmailProxyCredentials?) =
            scopeResult.also {
                scopeCalls++
                events += "scope"
                proxyInputs += proxyCredentials()
                beforeScopeReturn()
            }
        override suspend fun authenticate(scope: String, identifier: () -> String, password: () -> String, secondFactorCode: String) =
            authentication.also {
                authenticateCalls++
                events += "auth"
                beforeAuthenticate()
                authIdentifiers += identifier()
                authPasswords += password()
            }
        override suspend fun storeSession(session: String) = store.also { events += "store" }
        override suspend fun persistEmailIfNeeded(userId: String, identifier: () -> String) = persist.also {
            events += "persist"; persistEmails += identifier()
        }
        override suspend fun registerClient(userId: String, password: () -> String) = client.also {
            events += "client"; clientPasswords += password()
        }
        override suspend fun requestSecondFactorCode(scope: String, email: String) = verification.also {
            verificationScopes += scope; verificationEmails += email
        }
        override suspend fun revertSession(newSessionUserId: String?, previousSessionUserId: String?) {
            reverts += newSessionUserId to previousSessionUserId
        }
        override suspend fun parseBackendConfig(input: String) = parsedBackend
        override suspend fun configureBackend(request: String) = backendResult.also { backendLoads++ }
    }

    private class FakeTimer(private val gateway: FakeGateway) : LoginEmailTimer {
        override suspend fun start(seconds: Long, onUpdate: (String) -> Unit, onFinish: () -> Unit) {
            gateway.timerSeconds += seconds
            gateway.timerUpdates += "05:00"
            onUpdate("05:00")
            onFinish()
        }
    }

    private class FakeStore(override var userIdentifier: String? = null) : LoginSavedInputStore {
        override var ssoCode: String? = null
    }

}

private data class TestFailure(val value: String)
