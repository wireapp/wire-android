/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.email

import androidx.compose.foundation.text.input.clearText
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
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateAccountEmailViewModelTest {

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
    fun `initial state preserves flow flags effective links and tos`() = runTest(dispatcher) {
        val custom = TestLinks("custom", "custom-tos")
        val withCustom = arrange(customServerConfig = custom)
        val withDefault = arrange()

        assertEquals(TestFlow.Personal, withCustom.flowType)
        assertSame(custom, withCustom.customServerConfig)
        assertSame(custom, withCustom.serverConfig)
        assertEquals("custom-tos", withCustom.tosUrl())
        assertEquals(TestLinks("default", "default-tos"), withDefault.serverConfig)
        assertEquals("default-tos", withDefault.tosUrl())
        assertFalse(withDefault.emailState.showClientUpdateDialog)
        assertFalse(withDefault.emailState.showServerVersionNotSupportedDialog)
    }

    @Test
    fun `input clears errors and enables only non-empty input when not loading`() = runTest(dispatcher) {
        val gateway = FakeGateway(valid = false)
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.emailTextState.setTextAndPlaceCursorAtEnd("invalid")
        advanceUntilIdle()
        viewModel.onEmailContinue()
        advanceUntilIdle()
        assertEquals(invalidEmail, viewModel.emailState.error)

        viewModel.emailTextState.setTextAndPlaceCursorAtEnd("next")
        advanceUntilIdle()
        assertEquals(CreateAccountEmailViewState.EmailError.None, viewModel.emailState.error)
        assertTrue(viewModel.emailState.continueEnabled)

        viewModel.emailTextState.clearText()
        advanceUntilIdle()
        assertFalse(viewModel.emailState.continueEnabled)
    }

    @Test
    fun `continue normalizes locally validates and shows terms only for valid unaccepted email`() = runTest(dispatcher) {
        val gateway = FakeGateway(valid = true)
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.emailTextState.setTextAndPlaceCursorAtEnd("  Alice@Example.COM  ")
        advanceUntilIdle()

        viewModel.onEmailContinue()
        assertTrue(viewModel.emailState.loading)
        assertFalse(viewModel.emailState.continueEnabled)
        advanceUntilIdle()

        assertEquals(listOf("alice@example.com"), gateway.validatedEmails)
        assertTrue(viewModel.emailState.termsDialogVisible)
        assertFalse(viewModel.emailState.loading)
        assertTrue(viewModel.emailState.continueEnabled)
        assertTrue(gateway.requests.isEmpty())
    }

    @Test
    fun `invalid unaccepted email shows local error without requesting activation`() = runTest(dispatcher) {
        val gateway = FakeGateway(valid = false)
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.emailTextState.setTextAndPlaceCursorAtEnd("bad")
        advanceUntilIdle()

        viewModel.onEmailContinue()
        advanceUntilIdle()

        assertEquals(invalidEmail, viewModel.emailState.error)
        assertFalse(viewModel.emailState.termsDialogVisible)
        assertTrue(gateway.requests.isEmpty())
    }

    @Test
    fun `accepted invalid email still invokes activation request exactly as predecessor`() = runTest(dispatcher) {
        val gateway = FakeGateway(valid = false, activationResult = ActivationCodeResult.Sent)
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.emailTextState.setTextAndPlaceCursorAtEnd("bad")
        advanceUntilIdle()
        viewModel.onTermsAccept()
        advanceUntilIdle()
        gateway.requests.clear()

        viewModel.onEmailContinue()
        advanceUntilIdle()

        assertEquals("bad", gateway.validatedEmails.last())
        assertEquals(listOf(TestLinks("default", "default-tos") to "bad"), gateway.requests)
        assertEquals(CreateAccountEmailViewState.EmailError.None, viewModel.emailState.error)
    }

    @Test
    fun `terms acceptance requests normalized email with effective server config`() = runTest(dispatcher) {
        val custom = TestLinks("custom", "tos")
        val gateway = FakeGateway(activationResult = ActivationCodeResult.Sent)
        val viewModel = arrange(customServerConfig = custom, gateway = gateway)
        advanceUntilIdle()
        viewModel.emailTextState.setTextAndPlaceCursorAtEnd("  Alice@Example.COM ")
        advanceUntilIdle()

        viewModel.onTermsAccept()
        advanceUntilIdle()

        assertEquals(listOf(custom to "alice@example.com"), gateway.requests)
        assertTrue(viewModel.emailState.termsAccepted)
        assertFalse(viewModel.emailState.termsDialogVisible)
        assertTrue(viewModel.emailState.success)
    }

    @Test
    fun `all activation results map exactly and generic preserves failure identity`() = runTest(dispatcher) {
        val failure = TestFailure("offline")
        listOf(
            ActivationCodeResult.AlreadyInUse to alreadyInUse,
            ActivationCodeResult.Blacklisted to blacklisted,
            ActivationCodeResult.DomainBlocked to domainBlocked,
            ActivationCodeResult.InvalidEmail to invalidEmail,
            ActivationCodeResult.Generic(failure) to CreateAccountEmailViewState.EmailError.DialogError.GenericError(failure),
            ActivationCodeResult.Sent to CreateAccountEmailViewState.EmailError.None,
        ).forEach { (result, expected) ->
            val viewModel = arrange(gateway = FakeGateway(activationResult = result))
            advanceUntilIdle()
            viewModel.onTermsAccept()
            advanceUntilIdle()

            assertEquals(expected, viewModel.emailState.error)
            val actual = viewModel.emailState.error
            if (actual is CreateAccountEmailViewState.EmailError.DialogError.GenericError) {
                assertSame(failure, actual.coreFailure)
            }
            assertEquals(result === ActivationCodeResult.Sent, viewModel.emailState.success)
            assertFalse(viewModel.emailState.loading)
            assertTrue(viewModel.emailState.continueEnabled)
        }
    }

    @Test
    fun `auth scope unavailable keeps direct accept loading but outer continue completion resets loading`() = runTest(dispatcher) {
        val gateway = FakeGateway(activationResult = ActivationCodeResult.AuthScopeUnavailable)
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.emailTextState.setTextAndPlaceCursorAtEnd("alice@example.com")
        advanceUntilIdle()

        viewModel.onTermsAccept()
        advanceUntilIdle()
        assertTrue(viewModel.emailState.loading)
        assertTrue(viewModel.emailState.termsAccepted)

        viewModel.onEmailContinue()
        advanceUntilIdle()
        assertFalse(viewModel.emailState.loading)
        assertEquals(2, gateway.requests.size)
    }

    @Test
    fun `dismiss methods clear only their owned visibility or error`() = runTest(dispatcher) {
        val viewModel = arrange(gateway = FakeGateway(valid = true, activationResult = ActivationCodeResult.AlreadyInUse))
        advanceUntilIdle()
        viewModel.emailTextState.setTextAndPlaceCursorAtEnd("alice@example.com")
        advanceUntilIdle()
        viewModel.onEmailContinue()
        advanceUntilIdle()
        assertTrue(viewModel.emailState.termsDialogVisible)

        viewModel.onTermsDialogDismiss()
        assertFalse(viewModel.emailState.termsDialogVisible)
        viewModel.onTermsAccept()
        advanceUntilIdle()
        val before = viewModel.emailState
        viewModel.onEmailErrorDismiss()
        assertEquals(before.copy(error = CreateAccountEmailViewState.EmailError.None), viewModel.emailState)
    }

    private fun arrange(
        customServerConfig: TestLinks? = null,
        gateway: FakeGateway = FakeGateway(),
    ): CreateAccountEmailViewModel<TestFlow, TestLinks, TestFailure> = CreateAccountEmailViewModel(
        flowType = TestFlow.Personal,
        customServerConfig = customServerConfig,
        defaultServerConfig = TestLinks("default", "default-tos"),
        tosUrlFor = { it.tos },
        gateway = gateway,
    )

    private enum class TestFlow { Personal }
    private data class TestLinks(val name: String, val tos: String)
    private data class TestFailure(val message: String)

    private class FakeGateway(
        var valid: Boolean = true,
        var activationResult: ActivationCodeResult<TestFailure> = ActivationCodeResult.Sent,
    ) : CreateAccountEmailGateway<TestLinks, TestFailure> {
        val validatedEmails = mutableListOf<String>()
        val requests = mutableListOf<Pair<TestLinks, String>>()

        override fun isEmailValid(email: String): Boolean = valid.also { validatedEmails += email }

        override suspend fun requestActivationCode(
            serverConfig: TestLinks,
            email: String,
        ): ActivationCodeResult<TestFailure> = activationResult.also { requests += serverConfig to email }
    }

    private companion object {
        val invalidEmail = CreateAccountEmailViewState.EmailError.TextFieldError.InvalidEmailError
        val alreadyInUse = CreateAccountEmailViewState.EmailError.TextFieldError.AlreadyInUseError
        val blacklisted = CreateAccountEmailViewState.EmailError.TextFieldError.BlacklistedEmailError
        val domainBlocked = CreateAccountEmailViewState.EmailError.TextFieldError.DomainBlockedError
    }
}
