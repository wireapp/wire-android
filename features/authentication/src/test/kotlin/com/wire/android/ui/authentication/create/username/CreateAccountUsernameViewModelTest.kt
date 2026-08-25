/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.username

import androidx.compose.foundation.text.input.clearText
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
class CreateAccountUsernameViewModelTest {

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
    fun `init reports screen before starting validation and ignores initial empty value`() = runTest(dispatcher) {
        val events = mutableListOf<String>()
        val gateway = FakeGateway(events = events)
        arrange(gateway = gateway, analytics = FakeAnalytics(events))

        advanceUntilIdle()

        assertEquals(listOf("screen"), events)
        assertTrue(gateway.validatedUsernames.isEmpty())
    }

    @Test
    fun `after first non-empty value clearing username validates and shows invalid`() = runTest(dispatcher) {
        val gateway = FakeGateway(validation = { if (it.isEmpty()) UsernameValidation.Invalid else UsernameValidation.Valid })
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()

        viewModel.textState.setTextAndPlaceCursorAtEnd("alice")
        advanceUntilIdle()
        assertEquals(CreateAccountUsernameError.None, viewModel.state.error)
        assertTrue(viewModel.state.continueEnabled)

        viewModel.textState.clearText()
        advanceUntilIdle()
        assertEquals(listOf("alice", ""), gateway.validatedUsernames)
        assertEquals(CreateAccountUsernameError.UsernameInvalid, viewModel.state.error)
        assertFalse(viewModel.state.continueEnabled)
    }

    @Test
    fun `valid and invalid edits clear and set errors while respecting loading`() = runTest(dispatcher) {
        val gateway = FakeGateway(validation = { if (it == "valid") UsernameValidation.Valid else UsernameValidation.Invalid })
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()

        viewModel.textState.setTextAndPlaceCursorAtEnd("invalid")
        advanceUntilIdle()
        assertEquals(CreateAccountUsernameError.UsernameInvalid, viewModel.state.error)
        assertFalse(viewModel.state.continueEnabled)

        viewModel.textState.setTextAndPlaceCursorAtEnd("valid")
        advanceUntilIdle()
        assertEquals(CreateAccountUsernameError.None, viewModel.state.error)
        assertTrue(viewModel.state.continueEnabled)
    }

    @Test
    fun `valid edit while submission is loading clears error but stays disabled`() = runTest(dispatcher) {
        val releaseSubmission = CompletableDeferred<Unit>()
        val gateway = FakeGateway(beforeSetResult = { releaseSubmission.await() })
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.textState.setTextAndPlaceCursorAtEnd("alice")
        advanceUntilIdle()

        viewModel.onContinue()
        runCurrent()
        viewModel.textState.setTextAndPlaceCursorAtEnd("alice-2")
        runCurrent()

        assertTrue(viewModel.state.loading)
        assertEquals(CreateAccountUsernameError.None, viewModel.state.error)
        assertFalse(viewModel.state.continueEnabled)

        releaseSubmission.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `continue synchronously loads and submits trimmed username`() = runTest(dispatcher) {
        val gateway = FakeGateway(setResult = SetUsernameResult.UsernameTaken)
        val viewModel = arrange(gateway = gateway)
        advanceUntilIdle()
        viewModel.textState.setTextAndPlaceCursorAtEnd("  alice  ")
        advanceUntilIdle()

        viewModel.onContinue()

        assertTrue(viewModel.state.loading)
        assertFalse(viewModel.state.continueEnabled)
        advanceUntilIdle()
        assertEquals(listOf("alice"), gateway.submittedUsernames)
        assertFalse(viewModel.state.loading)
        assertTrue(viewModel.state.continueEnabled)
    }

    @Test
    fun `success completes analytics before exposing success`() = runTest(dispatcher) {
        val events = mutableListOf<String>()
        lateinit var viewModel: CreateAccountUsernameViewModel<TestFailure>
        val analytics = FakeAnalytics(events, onCompletion = {
            assertFalse(viewModel.state.success)
            events += "completion-observed-before-success"
        })
        viewModel = arrange(
            gateway = FakeGateway(setResult = SetUsernameResult.Success, events = events),
            analytics = analytics,
        )
        advanceUntilIdle()

        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(listOf("screen", "set", "completion", "completion-observed-before-success"), events)
        assertEquals(CreateAccountUsernameError.None, viewModel.state.error)
        assertTrue(viewModel.state.success)
        assertFalse(viewModel.state.loading)
        assertTrue(viewModel.state.continueEnabled)
    }

    @Test
    fun `taken invalid and generic results preserve exact errors and failure identity`() = runTest(dispatcher) {
        val failure = TestFailure("offline")
        listOf(
            SetUsernameResult.UsernameTaken to CreateAccountUsernameError.UsernameTaken,
            SetUsernameResult.UsernameInvalid to CreateAccountUsernameError.UsernameInvalid,
            SetUsernameResult.Failure(failure) to CreateAccountUsernameError.Generic(failure),
        ).forEach { (result, expected) ->
            val viewModel = arrange(gateway = FakeGateway(setResult = result))
            advanceUntilIdle()
            viewModel.onContinue()
            advanceUntilIdle()

            assertEquals(expected, viewModel.state.error)
            if (viewModel.state.error is CreateAccountUsernameError.Generic) {
                assertSame(failure, (viewModel.state.error as CreateAccountUsernameError.Generic<TestFailure>).failure)
            }
            assertFalse(viewModel.state.success)
            assertFalse(viewModel.state.loading)
            assertTrue(viewModel.state.continueEnabled)
        }
    }

    @Test
    fun `dismiss clears only error`() = runTest(dispatcher) {
        val viewModel = arrange(gateway = FakeGateway(setResult = SetUsernameResult.UsernameTaken))
        advanceUntilIdle()
        viewModel.onContinue()
        advanceUntilIdle()
        val before = viewModel.state

        viewModel.onErrorDismiss()

        assertEquals(before.copy(error = CreateAccountUsernameError.None), viewModel.state)
    }

    private fun arrange(
        gateway: FakeGateway = FakeGateway(),
        analytics: FakeAnalytics = FakeAnalytics(),
    ): CreateAccountUsernameViewModel<TestFailure> = CreateAccountUsernameViewModel(gateway, analytics)

    private data class TestFailure(val message: String)

    private class FakeGateway(
        private val validation: (String) -> UsernameValidation = { UsernameValidation.Valid },
        private val setResult: SetUsernameResult<TestFailure> = SetUsernameResult.Success,
        private val events: MutableList<String>? = null,
        private val beforeSetResult: suspend () -> Unit = {},
    ) : CreateAccountUsernameGateway<TestFailure> {
        val validatedUsernames = mutableListOf<String>()
        val submittedUsernames = mutableListOf<String>()

        override fun validateUsername(username: String): UsernameValidation {
            validatedUsernames += username
            events?.add("validate")
            return validation(username)
        }

        override suspend fun setUsername(username: String): SetUsernameResult<TestFailure> {
            submittedUsernames += username
            events?.add("set")
            beforeSetResult()
            return setResult
        }
    }

    private class FakeAnalytics(
        private val events: MutableList<String> = mutableListOf(),
        private val onCompletion: () -> Unit = {},
    ) : CreateAccountUsernameAnalytics {
        override suspend fun usernameScreenShown() {
            events += "screen"
        }

        override suspend fun accountCreationCompleted() {
            events += "completion"
            onCompletion()
        }
    }
}
