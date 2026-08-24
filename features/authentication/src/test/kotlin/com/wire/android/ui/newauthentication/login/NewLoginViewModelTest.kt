/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login

import app.cash.turbine.test
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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

private typealias TestViewModel = NewLoginViewModel<TestLinks, TestFailure, String, String, TestSession, String>

@OptIn(ExperimentalCoroutinesApi::class)
class NewLoginViewModelTest {
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
    fun `identifier precedence is prefill then managed code then saved value and edits persist distinctly`() = runTest(dispatcher) {
        val saved = FakeStore("saved")
        val prefilled = arrange(store = saved, prefill = "prefilled", managedCode = "managed")
        advanceUntilIdle()
        assertEquals("prefilled", prefilled.userIdentifierTextState.text.toString())

        val managed = arrange(store = FakeStore("saved"), managedCode = "managed")
        advanceUntilIdle()
        assertEquals("managed", managed.userIdentifierTextState.text.toString())

        val custom = arrange(store = FakeStore("saved"), managedCode = "managed", custom = TestLinks("custom"))
        advanceUntilIdle()
        assertEquals("saved", custom.userIdentifierTextState.text.toString())
        custom.userIdentifierTextState.setTextAndPlaceCursorAtEnd("changed")
        advanceUntilIdle()
        assertEquals("changed", (custom.savedStore() as FakeStore).userIdentifier)
    }

    @Test
    fun `missing backend blocks login and edits preserve backend states while clearing text errors when usable`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val viewModel = arrange(gateway = gateway, defaultConfigured = false)
        advanceUntilIdle()
        assertEquals(NewLoginFlowState.MissingBackendConfig, viewModel.state.flowState)
        viewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd("email@example.com")
        advanceUntilIdle()
        viewModel.onLoginStarted()
        advanceUntilIdle()
        assertEquals(NewLoginFlowState.MissingBackendConfig, viewModel.state.flowState)
        assertEquals(0, gateway.validationCalls)

        viewModel.onNavigationArgumentsChanged(
            NewLoginNavigationInput(TestLinks("custom"), true, false, null, null, null)
        )
        gateway.validation = NewLoginIdentifierValidation.Invalid
        viewModel.onLoginStarted()
        advanceUntilIdle()
        assertEquals(NewLoginFlowState.Error.TextFieldError.InvalidValue, viewModel.state.flowState)
        viewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd("other")
        advanceUntilIdle()
        assertEquals(NewLoginFlowState.Default, viewModel.state.flowState)
    }

    @Test
    fun `navigation updates prefill pending values and current backend without fetching defaults`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val backend = FakeBackend()
        val viewModel = arrange(gateway = gateway, backend = backend, saved = "already")
        advanceUntilIdle()
        gateway.fetchCalls = 0
        val updated = TestLinks("updated")
        viewModel.onNavigationArgumentsChanged(
            NewLoginNavigationInput(updated, true, true, "new@example.com", "nomad", "cookie-label")
        )
        advanceUntilIdle()
        assertEquals(updated, viewModel.serverConfig)
        assertEquals("new@example.com", viewModel.userIdentifierTextState.text.toString())
        assertEquals(listOf(updated), backend.selections)
        assertEquals(NewLoginFlowState.BackendConfigSuccess, viewModel.state.flowState)
        assertEquals(0, gateway.fetchCalls)
    }

    @Test
    fun `backend parsing precedes loading and success publishes configured links`() = runTest(dispatcher) {
        val backend = FakeBackend().apply {
            parsed = "request"
            result = NewLoginBackendResult.Success(TestLinks("loaded"))
        }
        val viewModel = arrange(backend = backend, defaultConfigured = false)
        viewModel.onBackendConfigLinkEntered("wire://config")
        advanceUntilIdle()
        assertEquals(listOf("parse:wire://config", "configure:request"), backend.events)
        assertEquals(TestLinks("loaded"), viewModel.serverConfig)
        assertEquals(NewLoginFlowState.BackendConfigSuccess, viewModel.state.flowState)
        viewModel.onNoBackendSelected()
        advanceUntilIdle()
        assertEquals(TestLinks("empty"), viewModel.serverConfig)
        assertTrue(backend.cleared)
        assertEquals(NewLoginFlowState.MissingBackendConfig, viewModel.state.flowState)
    }

    @Test
    fun `login trims once validates and password action preserves current server and claimed domain`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply {
            validation = NewLoginIdentifierValidation.Email
            enterprise = NewLoginEnterpriseResult.Password(false, "wire.example")
        }
        val viewModel = arrange(gateway = gateway)
        viewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd("  user@example.com  ")
        advanceUntilIdle()
        viewModel.actions.test {
            viewModel.onLoginStarted()
            advanceUntilIdle()
            assertEquals(
                NewLoginAction.EmailPassword("user@example.com", TestLinks("default"), false, "wire.example"),
                awaitItem(),
            )
        }
        assertEquals("user@example.com", gateway.validated)
        assertEquals("user@example.com", gateway.enterpriseEmail)
    }

    @Test
    fun `enterprise results preserve not-supported custom SSO and failure semantics`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val viewModel = arrange(gateway = gateway, saved = "email@example.com")
        advanceUntilIdle()
        viewModel.actions.test {
            gateway.enterprise = NewLoginEnterpriseResult.NotSupported
            viewModel.getEnterpriseLoginFlow("email@example.com")
            assertEquals(NewLoginAction.EnterpriseLoginNotSupported("email@example.com"), awaitItem())

            gateway.enterprise = NewLoginEnterpriseResult.CustomBackend(TestLinks("custom"))
            viewModel.getEnterpriseLoginFlow("email@example.com")
            advanceUntilIdle()
            assertEquals(NewLoginFlowState.CustomConfigDialog(TestLinks("custom")), viewModel.state.flowState)

            val failure = TestFailure("enterprise")
            gateway.enterprise = NewLoginEnterpriseResult.Failure(NewLoginFailure.Generic(failure))
            viewModel.getEnterpriseLoginFlow("email@example.com")
            advanceUntilIdle()
            assertSame(failure, (viewModel.state.flowState as NewLoginFlowState.Error.DialogError.GenericError).failure)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SSO initiation removes old identity then stores new identity and emits latest identifier`() = runTest(dispatcher) {
        val store = FakeStore("first").apply { pendingSsoIdentityProviderId = "old" }
        val gateway = FakeGateway()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        gateway.beforeInitiate = { entered.complete(Unit); release.await() }
        gateway.initiation = NewLoginSsoInitiationResult.Success("https://sso")
        val viewModel = arrange(store = store, gateway = gateway)
        advanceUntilIdle()

        viewModel.actions.test {
            val job = backgroundScope.launch { viewModel.initiateSSO(TestLinks("server"), "wire-code", "idp") }
            entered.await()
            assertNull(store.pendingSsoIdentityProviderId)
            viewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd("latest")
            release.complete(Unit)
            job.join()
            advanceUntilIdle()
            assertEquals("idp", store.pendingSsoIdentityProviderId)
            assertEquals(NewLoginAction.SSO("https://sso", "latest"), awaitItem())
        }
    }

    @Test
    fun `SSO initiation maps invalid format invalid code and failure exactly`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        gateway.initiation = NewLoginSsoInitiationResult.InvalidCodeFormat
        viewModel.initiateSSO(TestLinks("server"), "code")
        advanceUntilIdle()
        assertEquals(NewLoginFlowState.Error.TextFieldError.InvalidValue, viewModel.state.flowState)
        gateway.initiation = NewLoginSsoInitiationResult.InvalidCode
        viewModel.initiateSSO(TestLinks("server"), "code")
        advanceUntilIdle()
        assertEquals(NewLoginFlowState.Error.DialogError.InvalidSSOCode, viewModel.state.flowState)
        val failure = TestFailure("init")
        gateway.initiation = NewLoginSsoInitiationResult.Failure(NewLoginFailure.Generic(failure))
        viewModel.initiateSSO(TestLinks("server"), "code")
        advanceUntilIdle()
        assertSame(failure, (viewModel.state.flowState as NewLoginFlowState.Error.DialogError.GenericError).failure)
    }

    @Test
    fun `delayed default SSO fetch never overwrites input and initialization persists immediate success`() = runTest(dispatcher) {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val gateway = FakeGateway().apply {
            beforeFetch = { entered.complete(Unit); release.await() }
            defaultCode = NewLoginDefaultSsoCodeResult.Success("wire-default")
        }
        val store = FakeStore()
        val viewModel = arrange(gateway = gateway, store = store)
        entered.await()
        viewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd("typed")
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals("typed", viewModel.userIdentifierTextState.text.toString())

        val immediateStore = FakeStore()
        val immediate = arrange(gateway = FakeGateway().apply {
            defaultCode = NewLoginDefaultSsoCodeResult.Success("wire-immediate")
        }, store = immediateStore)
        advanceUntilIdle()
        assertEquals("wire-immediate", immediate.userIdentifierTextState.text.toString())
        assertEquals("wire-immediate", immediateStore.userIdentifier)
    }

    @Test
    fun `custom dialog fetch failure is visible and no-code action reads latest identifier`() = runTest(dispatcher) {
        val failure = TestFailure("fetch")
        val gateway = FakeGateway().apply {
            defaultCode = NewLoginDefaultSsoCodeResult.Failure(NewLoginFailure.Generic(failure))
        }
        val viewModel = arrange(gateway = gateway, saved = "first")
        advanceUntilIdle()
        viewModel.onCustomServerDialogConfirm(TestLinks("custom"))
        advanceUntilIdle()
        assertSame(failure, (viewModel.state.flowState as NewLoginFlowState.Error.DialogError.GenericError).failure)

        gateway.defaultCode = NewLoginDefaultSsoCodeResult.Success(null)
        viewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd("latest")
        viewModel.actions.test {
            viewModel.onCustomServerDialogConfirm(TestLinks("custom"))
            advanceUntilIdle()
            assertEquals(NewLoginAction.CustomConfig("latest", TestLinks("custom")), awaitItem())
        }
    }

    @Test
    fun `callback consumes identity before establish and Nomad and cookie providers only once`() = runTest(dispatcher) {
        val store = FakeStore().apply { pendingSsoIdentityProviderId = "idp" }
        val gateway = FakeGateway().apply {
            session = NewLoginSessionResult.Success("user")
            register = NewLoginRegisterClientResult.Success(true)
            consumeSessionProviders = true
        }
        val viewModel = arrange(store = store, gateway = gateway, nomad = "nomad", cookieLabel = "label")
        viewModel.actions.test {
            viewModel.handleSSOResult(NewLoginSsoCallback.Success("cookie", "config"))
            advanceUntilIdle()
            awaitItem()
        }
        assertEquals("idp", gateway.identityProviderId)
        assertNull(store.pendingSsoIdentityProviderId)
        assertEquals(listOf("nomad"), gateway.nomadValues)
        assertEquals(listOf("label"), gateway.cookieValues)

        gateway.session = NewLoginSessionResult.Failure(NewLoginFailure.ServerVersionNotSupported)
        viewModel.handleSSOResult(NewLoginSsoCallback.Success("cookie", "config"))
        advanceUntilIdle()
        assertEquals(listOf("nomad", null), gateway.nomadValues)
        assertEquals(listOf("label", null), gateway.cookieValues)
    }

    @Test
    fun `deep link failure and session failures map exactly`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val viewModel = arrange(gateway = gateway)
        viewModel.handleSSOResult(NewLoginSsoCallback.Failure("not-found"))
        advanceUntilIdle()
        assertEquals(
            NewLoginFlowState.Error.DialogError.SSOResultFailure("not-found"),
            viewModel.state.flowState,
        )
        gateway.session = NewLoginSessionResult.InvalidCookie
        viewModel.handleSSOResult(NewLoginSsoCallback.Success("cookie", "config"))
        advanceUntilIdle()
        assertEquals(NewLoginFlowState.Error.DialogError.InvalidSSOCookie, viewModel.state.flowState)
        gateway.session = NewLoginSessionResult.UserAlreadyExists
        viewModel.handleSSOResult(NewLoginSsoCallback.Success("cookie", "config"))
        advanceUntilIdle()
        assertEquals(NewLoginFlowState.Error.DialogError.UserAlreadyExists, viewModel.state.flowState)
    }

    @Test
    fun `identity change can dismiss or confirm and retained Nomad classification controls restore`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply { session = NewLoginSessionResult.IdentityChanged(TestSession("retained"), true) }
        val viewModel = arrange(gateway = gateway)
        viewModel.handleSSOResult(NewLoginSsoCallback.Success("cookie", "config"))
        advanceUntilIdle()
        assertEquals(NewLoginFlowState.SsoIdentityChanged, viewModel.state.flowState)
        viewModel.onSsoIdentityChangeDismissed()
        advanceUntilIdle()
        assertEquals(NewLoginFlowState.Default, viewModel.state.flowState)

        viewModel.handleSSOResult(NewLoginSsoCallback.Success("cookie", "config"))
        advanceUntilIdle()
        gateway.replacement = NewLoginReplaceSessionResult.Success("user")
        gateway.restore = NewLoginRestoreResult.Success(true)
        viewModel.actions.test {
            viewModel.onSsoIdentityChangeConfirmed()
            advanceUntilIdle()
            assertEquals(NewLoginAction.Success(NewLoginAction.Success.NextStep.None("user")), awaitItem())
        }
        assertEquals(listOf(TestSession("retained")), gateway.replaced)
        assertEquals(listOf("user"), gateway.restored)
    }

    @Test
    fun `regular registration maps all results and never reverts failures`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply { session = NewLoginSessionResult.Success("user") }
        val viewModel = arrange(gateway = gateway)
        viewModel.actions.test {
            gateway.register = NewLoginRegisterClientResult.Success(false)
            viewModel.handleSSOResult(NewLoginSsoCallback.Success("cookie", "config"))
            advanceUntilIdle()
            assertEquals(NewLoginAction.Success(NewLoginAction.Success.NextStep.InitialSync("user")), awaitItem())

            gateway.register = NewLoginRegisterClientResult.E2EICertificateRequired
            viewModel.handleSSOResult(NewLoginSsoCallback.Success("cookie", "config"))
            advanceUntilIdle()
            assertEquals(NewLoginAction.Success(NewLoginAction.Success.NextStep.E2EIEnrollment("user")), awaitItem())

            gateway.register = NewLoginRegisterClientResult.TooManyDevices
            viewModel.handleSSOResult(NewLoginSsoCallback.Success("cookie", "config"))
            advanceUntilIdle()
            assertEquals(NewLoginAction.Success(NewLoginAction.Success.NextStep.TooManyDevices("user")), awaitItem())
        }
        val failure = TestFailure("client")
        gateway.register = NewLoginRegisterClientResult.Failure(failure)
        viewModel.handleSSOResult(NewLoginSsoCallback.Success("cookie", "config"))
        advanceUntilIdle()
        assertSame(failure, (viewModel.state.flowState as NewLoginFlowState.Error.DialogError.GenericError).failure)
        assertTrue(gateway.reverted.isEmpty())
        assertEquals(listOf(false, false, false, false), gateway.setLastDeviceFlags)
    }

    @Test
    fun `Nomad restore success no-backup unavailable and failure preserve exact continuation`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply {
            session = NewLoginSessionResult.Success("user")
            consumeSessionProviders = true
        }
        val viewModel = arrange(gateway = gateway, nomad = "nomad")
        viewModel.actions.test {
            gateway.restore = NewLoginRestoreResult.Success(false)
            viewModel.handleSSOResult(NewLoginSsoCallback.Success("cookie", "config"))
            advanceUntilIdle()
            assertEquals(NewLoginAction.Success(NewLoginAction.Success.NextStep.InitialSync("user")), awaitItem())
        }

        viewModel.onNavigationArgumentsChanged(
            NewLoginNavigationInput(null, false, false, null, "nomad-2", null)
        )
        gateway.restore = NewLoginRestoreResult.NoBackupAvailable
        gateway.register = NewLoginRegisterClientResult.Success(true)
        viewModel.actions.test {
            viewModel.handleSSOResult(NewLoginSsoCallback.Success("cookie", "config"))
            advanceUntilIdle()
            assertEquals(NewLoginAction.Success(NewLoginAction.Success.NextStep.None("user")), awaitItem())
        }
        assertTrue(gateway.setLastDeviceFlags.last())

        viewModel.onNavigationArgumentsChanged(
            NewLoginNavigationInput(null, false, false, null, "nomad-3", null)
        )
        gateway.restore = NewLoginRestoreResult.SessionUnavailable
        viewModel.handleSSOResult(NewLoginSsoCallback.Success("cookie", "config"))
        advanceUntilIdle()
        assertEquals(NewLoginFlowState.Loading, viewModel.state.flowState)

        viewModel.onNavigationArgumentsChanged(
            NewLoginNavigationInput(null, false, false, null, "nomad-4", null)
        )
        val failure = TestFailure("restore")
        gateway.restore = NewLoginRestoreResult.Failure(failure)
        viewModel.handleSSOResult(NewLoginSsoCallback.Success("cookie", "config"))
        advanceUntilIdle()
        assertEquals(listOf("user"), gateway.reverted)
        assertSame(failure, (viewModel.state.flowState as NewLoginFlowState.Error.DialogError.GenericError).failure)
    }

    @Test
    fun `next enablement keeps predecessor backend and loading exclusions`() = runTest(dispatcher) {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val gateway = FakeGateway().apply {
            validation = NewLoginIdentifierValidation.Email
            beforeEnterprise = { entered.complete(Unit); release.await() }
        }
        val viewModel = arrange(gateway = gateway, saved = "value")
        advanceUntilIdle()
        assertTrue(viewModel.state.nextEnabled)
        viewModel.onLoginStarted()
        entered.await()
        assertFalse(viewModel.state.nextEnabled)
        release.complete(Unit)
        advanceUntilIdle()
        assertTrue(viewModel.state.nextEnabled)
    }

    private fun arrange(
        gateway: FakeGateway = FakeGateway(),
        backend: FakeBackend = FakeBackend(),
        store: FakeStore = FakeStore(),
        saved: String? = null,
        prefill: String? = null,
        managedCode: String? = null,
        custom: TestLinks? = null,
        defaultConfigured: Boolean = true,
        nomad: String? = null,
        cookieLabel: String? = null,
    ): TestViewModel {
        if (saved != null) store.userIdentifier = saved
        return NewLoginViewModel<TestLinks, TestFailure, String, String, TestSession, String>(
            NewLoginInput(
                defaultServerConfig = TestLinks("default"),
                initialCustomServerConfig = custom,
                emptyServerConfig = TestLinks("empty"),
                isDefaultServerConfigured = defaultConfigured,
                isInitialCustomServerConfigured = custom != null,
                showBackendConfigSuccess = false,
                preFilledIdentifier = prefill,
                managedSsoCode = managedCode,
                pendingNomadServiceUrl = nomad,
                pendingCookieLabel = cookieLabel,
            ),
            gateway,
            backend,
            store,
        ).also { stores[it] = store }
    }

    private val stores = mutableMapOf<TestViewModel, NewLoginSavedStateStore>()
    private fun TestViewModel.savedStore() = stores.getValue(this)
}

private data class TestLinks(val value: String)
private data class TestFailure(val value: String)
private data class TestSession(val value: String)

private class FakeStore(initial: String? = null) : NewLoginSavedStateStore {
    override var userIdentifier: String? = initial
    override var pendingSsoIdentityProviderId: String? = null
}

private class FakeBackend : NewLoginBackendGateway<TestLinks, String> {
    var parsed: String? = null
    var result: NewLoginBackendResult<TestLinks> = NewLoginBackendResult.Failure
    val events = mutableListOf<String>()
    val selections = mutableListOf<TestLinks>()
    var cleared = false
    override suspend fun parse(input: String): String? = parsed.also { events += "parse:$input" }
    override suspend fun configure(request: String): NewLoginBackendResult<TestLinks> = result.also { events += "configure:$request" }
    override fun select(serverConfig: TestLinks) { selections += serverConfig }
    override fun clear() { cleared = true }
}

private class FakeGateway : NewLoginGateway<TestLinks, TestFailure, String, TestSession> {
    var validation = NewLoginIdentifierValidation.Email
    var validationCalls = 0
    var validated: String? = null
    var enterpriseEmail: String? = null
    var enterprise: NewLoginEnterpriseResult<TestLinks, TestFailure> = NewLoginEnterpriseResult.Password(true)
    var initiation: NewLoginSsoInitiationResult<TestFailure> = NewLoginSsoInitiationResult.Success("url")
    var defaultCode: NewLoginDefaultSsoCodeResult<TestFailure> = NewLoginDefaultSsoCodeResult.Success(null)
    var session: NewLoginSessionResult<TestFailure, String, TestSession> = NewLoginSessionResult.Success("user")
    var replacement: NewLoginReplaceSessionResult<TestFailure, String> = NewLoginReplaceSessionResult.Success("user")
    var register: NewLoginRegisterClientResult<TestFailure> = NewLoginRegisterClientResult.Success(true)
    var restore: NewLoginRestoreResult<TestFailure> = NewLoginRestoreResult.Success(true)
    var beforeInitiate: suspend () -> Unit = {}
    var beforeEnterprise: suspend () -> Unit = {}
    var beforeFetch: suspend () -> Unit = {}
    var fetchCalls = 0
    var consumeSessionProviders = false
    var identityProviderId: String? = null
    val nomadValues = mutableListOf<String?>()
    val cookieValues = mutableListOf<String?>()
    val replaced = mutableListOf<TestSession>()
    val restored = mutableListOf<String>()
    val reverted = mutableListOf<String>()
    val setLastDeviceFlags = mutableListOf<Boolean>()

    override fun validateIdentifier(input: String): NewLoginIdentifierValidation {
        validationCalls++
        validated = input
        return validation
    }
    override suspend fun enterpriseLogin(serverConfig: TestLinks, email: String): NewLoginEnterpriseResult<TestLinks, TestFailure> {
        enterpriseEmail = email
        beforeEnterprise()
        return enterprise
    }
    override suspend fun initiateSso(serverConfig: TestLinks, code: String, cookieLabel: String?): NewLoginSsoInitiationResult<TestFailure> {
        beforeInitiate()
        return initiation
    }
    override suspend fun fetchDefaultSsoCode(serverConfig: TestLinks): NewLoginDefaultSsoCodeResult<TestFailure> {
        fetchCalls++
        beforeFetch()
        return defaultCode
    }
    override suspend fun establishSession(
        cookie: String,
        serverConfigId: String,
        ssoIdentityProviderId: String?,
        consumeNomadServiceUrl: () -> String?,
        consumeCookieLabel: () -> String?,
    ): NewLoginSessionResult<TestFailure, String, TestSession> {
        identityProviderId = ssoIdentityProviderId
        if (consumeSessionProviders) {
            nomadValues += consumeNomadServiceUrl()
            cookieValues += consumeCookieLabel()
        }
        return session
    }
    override suspend fun replaceRetainedSession(session: TestSession) = replacement.also { replaced += session }
    override suspend fun registerClient(userId: String, setLastDeviceIdOnSuccess: Boolean) =
        register.also { setLastDeviceFlags += setLastDeviceIdOnSuccess }
    override suspend fun restoreCryptoState(userId: String) = restore.also { restored += userId }
    override suspend fun revertSession(userId: String) { reverted += userId }
    override fun logSessionContinuation(isNomadFlow: Boolean) = Unit
}
