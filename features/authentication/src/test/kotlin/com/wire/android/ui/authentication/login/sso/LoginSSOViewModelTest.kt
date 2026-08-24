/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.login.sso

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import com.wire.android.ui.authentication.login.LoginSavedInputStore
import com.wire.android.ui.authentication.login.LoginState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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

@OptIn(ExperimentalCoroutinesApi::class)
class LoginSSOViewModelTest {
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
    fun `saved code is restored distinct edits are persisted and enablement follows loading`() = runTest(dispatcher) {
        val store = FakeStore(ssoCode = "saved")
        val viewModel = arrange(store = store)
        advanceUntilIdle()
        assertEquals("saved", viewModel.ssoTextState.text.toString())
        assertTrue(viewModel.loginState.loginEnabled)

        viewModel.ssoTextState.setTextAndPlaceCursorAtEnd("changed")
        advanceUntilIdle()
        assertEquals(listOf("saved", "changed"), store.writes.distinct())

        viewModel.login()
        assertTrue(viewModel.loginState.flowState is LoginState.Loading)
        assertFalse(viewModel.loginState.loginEnabled)
    }

    @Test
    fun `input clears errors outside loading and clearLoginErrors restores enablement`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply { initiationResult = LoginSSOInitiationResult.InvalidCode }
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.ssoTextState.setTextAndPlaceCursorAtEnd("wire-code")
        advanceUntilIdle()
        viewModel.login()
        advanceUntilIdle()
        assertTrue(viewModel.loginState.flowState is LoginState.Error.DialogError.InvalidSSOCodeError)

        viewModel.ssoTextState.setTextAndPlaceCursorAtEnd("wire-other")
        advanceUntilIdle()
        assertTrue(viewModel.loginState.flowState is LoginState.Default)
        assertTrue(viewModel.loginState.loginEnabled)

        viewModel.clearLoginErrors()
        assertTrue(viewModel.loginState.loginEnabled)
    }

    @Test
    fun `login snapshots email decision but domain provider reads latest value after suspension`() = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>()
        val gateway = FakeGateway().apply {
            emailValidation = true
            beforeDomainRead = { release.await() }
            domainResult = LoginSSODomainLookupResult.Success(TestLinks("custom"))
        }
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.ssoTextState.setTextAndPlaceCursorAtEnd("first@example.com")
        advanceUntilIdle()

        viewModel.login()
        runCurrent()
        assertEquals(listOf("first@example.com"), gateway.validatedInputs)
        viewModel.ssoTextState.setTextAndPlaceCursorAtEnd("latest@example.com")
        release.complete(Unit)
        advanceUntilIdle()

        assertEquals("latest@example.com", gateway.domainEmails.single())
        assertEquals(TestLinks("custom"), viewModel.loginState.customServerDialogState?.serverLinks)
        assertTrue(viewModel.loginState.flowState is LoginState.Default)
    }

    @Test
    fun `domain errors preserve generic identity and unavailable maps to server version`() = runTest(dispatcher) {
        val failure = TestFailure("network")
        val gateway = FakeGateway().apply {
            emailValidation = true
            domainResult = LoginSSODomainLookupResult.Failure(failure)
        }
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.domainLookupFlow()
        advanceUntilIdle()
        val generic = viewModel.loginState.flowState as LoginState.Error.DialogError.GenericError
        assertSame(failure, generic.coreFailure)

        gateway.domainResult = LoginSSODomainLookupResult.AuthenticationUnavailable
        viewModel.domainLookupFlow()
        advanceUntilIdle()
        assertTrue(viewModel.loginState.flowState is LoginState.Error.DialogError.ServerVersionNotSupported)
    }

    @Test
    fun `initiation maps all errors and success resets default before emitting url`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.ssoTextState.setTextAndPlaceCursorAtEnd("wire-code")
        advanceUntilIdle()

        gateway.initiationResult = LoginSSOInitiationResult.InvalidCodeFormat
        viewModel.login()
        advanceUntilIdle()
        assertTrue(viewModel.loginState.flowState is LoginState.Error.TextFieldError.InvalidValue)

        gateway.initiationResult = LoginSSOInitiationResult.Failure(LoginSSOFailure.ClientUpdateRequired)
        viewModel.login()
        advanceUntilIdle()
        assertTrue(viewModel.loginState.flowState is LoginState.Error.DialogError.ClientUpdateRequired)

        gateway.initiationResult = LoginSSOInitiationResult.Success("https://sso")
        val request = async { viewModel.openWebUrl.first() }
        viewModel.login()
        advanceUntilIdle()
        assertEquals(LoginSSOWebRequest("https://sso", TestLinks("default")), request.await())
        assertTrue(viewModel.loginState.flowState is LoginState.Default)
    }

    @Test
    fun `custom server confirmation initiates only for code and fetch failure is silent`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply {
            emailValidation = true
            domainResult = LoginSSODomainLookupResult.Success(TestLinks("custom"))
        }
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.domainLookupFlow()
        advanceUntilIdle()

        gateway.defaultCodeResult = LoginSSODefaultCodeResult.Success(null)
        viewModel.onCustomServerDialogConfirm()
        advanceUntilIdle()
        assertTrue(gateway.initiations.isEmpty())

        gateway.defaultCodeResult = LoginSSODefaultCodeResult.Unavailable
        viewModel.onCustomServerDialogConfirm()
        advanceUntilIdle()
        assertTrue(gateway.initiations.isEmpty())

        gateway.defaultCodeResult = LoginSSODefaultCodeResult.Success("wire-default")
        gateway.initiationResult = LoginSSOInitiationResult.InvalidCode
        viewModel.onCustomServerDialogConfirm()
        advanceUntilIdle()
        assertEquals(Triple(TestLinks("custom"), "wire-default", null), gateway.initiations.single())
    }

    @Test
    fun `auto login updates code pending cookie and initiates synchronously`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.handleSSOCodeAutoLogin("wire-auto", true, "nomad", "shared")
        assertTrue(viewModel.loginState.flowState is LoginState.Loading)
        advanceUntilIdle()
        assertEquals(Triple(TestLinks("default"), "wire-auto", "shared"), gateway.initiations.single())
    }

    @Test
    fun `session keeps pending values through unsuccessful host lookup then consumes once after success`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply {
            sessionResult = LoginSSOSessionResult.InvalidCookie
            consumeSessionInputs = false
            restoreResult = LoginSSORestoreResult.Success(true)
        }
        val viewModel = arrange(gateway = gateway, nomad = "nomad", cookieLabel = "shared")
        advanceUntilIdle()
        viewModel.establishSSOSession("cookie", "config")
        advanceUntilIdle()
        assertTrue(gateway.consumedNomad.isEmpty())
        assertTrue(gateway.consumedLabels.isEmpty())

        gateway.sessionResult = LoginSSOSessionResult.Success("user")
        gateway.consumeSessionInputs = true
        viewModel.establishSSOSession("cookie", "config")
        viewModel.establishSSOSession("cookie-2", "config")
        advanceUntilIdle()

        assertEquals(listOf("nomad", null), gateway.consumedNomad)
        assertEquals(listOf("shared", null), gateway.consumedLabels)
        assertEquals(2, gateway.restoreUsers.size)
        assertTrue(viewModel.loginState.flowState is LoginState.Success<*>)
    }

    @Test
    fun `session failures map exactly and deep link failure preserves identity`() = runTest(dispatcher) {
        val failure = TestFailure("failure")
        val gateway = FakeGateway()
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()

        gateway.sessionResult = LoginSSOSessionResult.InvalidCookie
        viewModel.establishSSOSession("cookie", "config")
        advanceUntilIdle()
        assertTrue(viewModel.loginState.flowState is LoginState.Error.DialogError.InvalidSSOCookie)

        gateway.sessionResult = LoginSSOSessionResult.UserAlreadyExists
        viewModel.establishSSOSession("cookie", "config")
        advanceUntilIdle()
        assertTrue(viewModel.loginState.flowState is LoginState.Error.DialogError.UserAlreadyExists)

        gateway.sessionResult = LoginSSOSessionResult.Failure(LoginSSOFailure.Generic(failure))
        viewModel.establishSSOSession("cookie", "config")
        advanceUntilIdle()
        assertSame(failure, (viewModel.loginState.flowState as LoginState.Error.DialogError.GenericError).coreFailure)

        viewModel.handleSSOFailure(TestSsoFailure("deep-link"))
        assertEquals(
            TestSsoFailure("deep-link"),
            (viewModel.loginState.flowState as LoginState.Error.DialogError.SSOResultError).result,
        )
    }

    @Test
    fun `identity dismiss drops session and confirm replaces using session nomad flag`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply {
            sessionResult = LoginSSOSessionResult.IdentityChanged(TestSession("retained"), true)
            replacementResult = LoginSSOReplaceSessionResult.Success("user")
            restoreResult = LoginSSORestoreResult.Success(false)
        }
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.establishSSOSession("cookie", "config")
        advanceUntilIdle()
        assertTrue(viewModel.loginState.showSsoIdentityChangedDialog)
        viewModel.onSsoIdentityChangeDismissed()
        viewModel.onSsoIdentityChangeConfirmed()
        advanceUntilIdle()
        assertTrue(gateway.replacedSessions.isEmpty())

        viewModel.establishSSOSession("cookie", "config")
        advanceUntilIdle()
        viewModel.onSsoIdentityChangeConfirmed()
        assertTrue(viewModel.loginState.flowState is LoginState.Loading)
        assertFalse(viewModel.loginState.showSsoIdentityChangedDialog)
        advanceUntilIdle()
        assertEquals(listOf(TestSession("retained")), gateway.replacedSessions)
        assertEquals(listOf("user"), gateway.restoreUsers)
    }

    @Test
    fun `regular client branches preserve sync e2ei too many and revert rules`() = runTest(dispatcher) {
        val failure = TestFailure("client")
        val gateway = FakeGateway().apply { sessionResult = LoginSSOSessionResult.Success("user") }
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()

        gateway.clientResult = LoginSSORegisterClientResult.Success(false)
        viewModel.establishSSOSession("cookie", "config")
        advanceUntilIdle()
        assertEquals(LoginState.Success(false, false, "user"), viewModel.loginState.flowState)

        gateway.clientResult = LoginSSORegisterClientResult.E2EICertificateRequired(true)
        viewModel.establishSSOSession("cookie", "config")
        advanceUntilIdle()
        assertEquals(LoginState.Success(true, true, "user"), viewModel.loginState.flowState)

        gateway.clientResult = LoginSSORegisterClientResult.TooManyDevices
        viewModel.establishSSOSession("cookie", "config")
        advanceUntilIdle()
        assertTrue(viewModel.loginState.flowState is LoginState.Error.TooManyDevicesError<*>)
        assertTrue(gateway.revertedUsers.isEmpty())

        gateway.clientResult = LoginSSORegisterClientResult.Failure(failure)
        viewModel.establishSSOSession("cookie", "config")
        advanceUntilIdle()
        assertEquals(listOf("user"), gateway.revertedUsers)
        assertSame(failure, (viewModel.loginState.flowState as LoginState.Error.DialogError.GenericError).coreFailure)
    }

    @Test
    fun `nomad restore no backup registers with last device and failure reverts`() = runTest(dispatcher) {
        val failure = TestFailure("restore")
        val gateway = FakeGateway().apply {
            sessionResult = LoginSSOSessionResult.Success("user")
            restoreResult = LoginSSORestoreResult.NoBackupAvailable
            clientResult = LoginSSORegisterClientResult.Success(true)
        }
        val viewModel = arrange(gateway = gateway, nomad = "nomad")
        advanceUntilIdle()
        viewModel.establishSSOSession("cookie", "config")
        advanceUntilIdle()
        assertEquals(listOf("user" to true), gateway.clientCalls)

        val failing = FakeGateway().apply {
            sessionResult = LoginSSOSessionResult.Success("user")
            restoreResult = LoginSSORestoreResult.Failure(failure)
        }
        val failingViewModel = arrange(gateway = failing, nomad = "nomad")
        advanceUntilIdle()
        failingViewModel.establishSSOSession("cookie", "config")
        advanceUntilIdle()
        assertEquals(listOf("user"), failing.revertedUsers)
        assertSame(failure, (failingViewModel.loginState.flowState as LoginState.Error.DialogError.GenericError).coreFailure)
    }

    private fun arrange(
        gateway: FakeGateway = FakeGateway(),
        store: FakeStore = FakeStore(),
        nomad: String? = null,
        cookieLabel: String? = null,
    ): LoginSSOViewModel<TestLinks, TestFailure, String, TestSsoFailure, TestSession> = LoginSSOViewModel(
        input = LoginSSOInput(TestLinks("default"), nomad, cookieLabel),
        savedInputStore = store,
        gateway = gateway,
    )

    private class FakeGateway : LoginSSOGateway<TestLinks, TestFailure, String, TestSession> {
        var emailValidation = false
        var initiationResult: LoginSSOInitiationResult<TestFailure> = LoginSSOInitiationResult.Success("https://sso")
        var domainResult: LoginSSODomainLookupResult<TestLinks, TestFailure> = LoginSSODomainLookupResult.Success(TestLinks("custom"))
        var defaultCodeResult: LoginSSODefaultCodeResult<TestFailure> = LoginSSODefaultCodeResult.Success(null)
        var sessionResult: LoginSSOSessionResult<TestFailure, String, TestSession> = LoginSSOSessionResult.Success("user")
        var replacementResult: LoginSSOReplaceSessionResult<TestFailure, String> = LoginSSOReplaceSessionResult.Success("user")
        var clientResult: LoginSSORegisterClientResult<TestFailure> = LoginSSORegisterClientResult.Success(true)
        var restoreResult: LoginSSORestoreResult<TestFailure> = LoginSSORestoreResult.Success(true)
        var consumeSessionInputs = true
        var beforeDomainRead: suspend () -> Unit = {}
        val validatedInputs = mutableListOf<String>()
        val domainEmails = mutableListOf<String>()
        val initiations = mutableListOf<Triple<TestLinks, String, String?>>()
        val consumedNomad = mutableListOf<String?>()
        val consumedLabels = mutableListOf<String?>()
        val replacedSessions = mutableListOf<TestSession>()
        val clientCalls = mutableListOf<Pair<String, Boolean>>()
        val restoreUsers = mutableListOf<String>()
        val revertedUsers = mutableListOf<String>()

        override fun isEmail(value: String) = emailValidation.also { validatedInputs += value }
        override suspend fun initiateSSO(serverConfig: TestLinks, ssoCode: String, cookieLabel: String?) =
            initiationResult.also { initiations += Triple(serverConfig, ssoCode, cookieLabel) }
        override suspend fun lookupDomain(email: () -> String): LoginSSODomainLookupResult<TestLinks, TestFailure> {
            beforeDomainRead()
            domainEmails += email()
            return domainResult
        }
        override suspend fun fetchDefaultSSOCode(serverConfig: TestLinks) = defaultCodeResult
        override suspend fun establishSession(
            cookie: String,
            serverConfigId: String,
            consumeNomadServiceUrl: () -> String?,
            consumeCookieLabel: () -> String?,
        ) = sessionResult.also {
            if (consumeSessionInputs) {
                consumedNomad += consumeNomadServiceUrl()
                consumedLabels += consumeCookieLabel()
            }
        }
        override suspend fun replaceRetainedSession(session: TestSession) = replacementResult.also {
            replacedSessions += session
        }
        override fun logSessionContinuation(isNomadFlow: Boolean) = Unit
        override suspend fun registerClient(userId: String, setLastDeviceIdOnSuccess: Boolean) = clientResult.also {
            clientCalls += userId to setLastDeviceIdOnSuccess
        }
        override suspend fun restoreCryptoState(userId: String) = restoreResult.also { restoreUsers += userId }
        override suspend fun revertSession(userId: String) {
            revertedUsers += userId
        }
    }

    private class FakeStore(override var userIdentifier: String? = null, ssoCode: String? = null) : LoginSavedInputStore {
        val writes = mutableListOf<String?>()
        override var ssoCode: String? = ssoCode
            set(value) {
                field = value
                writes += value
            }
    }

    private data class TestLinks(val value: String)
    private data class TestFailure(val value: String)
    private data class TestSsoFailure(val value: String)
    private data class TestSession(val value: String)
}
