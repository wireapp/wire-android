/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.details

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
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateAccountDetailsViewModelTest {

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
    fun `initial state keeps custom links optional and selects effective links`() = runTest(dispatcher) {
        val custom = TestLinks("custom")
        val withCustom = arrange(customServerConfig = custom)
        val withDefault = arrange()

        assertSame(custom, withCustom.customServerConfig)
        assertSame(custom, withCustom.serverConfig)
        assertNull(withDefault.customServerConfig)
        assertEquals(TestLinks("default"), withDefault.serverConfig)
        assertEquals(CreateAccountDetailsViewState<TestFailure>(), withDefault.detailsState)
    }

    @Test
    fun `personal account enables continue when required personal fields are filled`() = runTest(dispatcher) {
        val viewModel = arrange(requiresTeamName = false)

        fillRequiredFields(viewModel)
        advanceUntilIdle()

        assertTrue(viewModel.detailsState.continueEnabled)
    }

    @Test
    fun `team account remains disabled until team name is filled`() = runTest(dispatcher) {
        val viewModel = arrange(requiresTeamName = true)

        fillRequiredFields(viewModel)
        advanceUntilIdle()
        assertFalse(viewModel.detailsState.continueEnabled)

        viewModel.teamNameTextState.setTextAndPlaceCursorAtEnd("Wire Team")
        advanceUntilIdle()
        assertTrue(viewModel.detailsState.continueEnabled)
    }

    @Test
    fun `editing any input clears validation error`() = runTest(dispatcher) {
        val gateway = FakeGateway(isValid = false)
        val viewModel = arrange(gateway = gateway)
        fillRequiredFields(viewModel)
        advanceUntilIdle()
        viewModel.onDetailsContinue()
        advanceUntilIdle()
        assertEquals(invalidPassword, viewModel.detailsState.error)

        viewModel.firstNameTextState.setTextAndPlaceCursorAtEnd("Grace")
        advanceUntilIdle()
        assertEquals(CreateAccountDetailsViewState.DetailsError.None, viewModel.detailsState.error)
    }

    @Test
    fun `invalid password resets loading and exposes password error`() = runTest(dispatcher) {
        val gateway = FakeGateway(isValid = false)
        val viewModel = arrange(gateway = gateway)
        fillRequiredFields(viewModel, password = "bad")
        advanceUntilIdle()

        viewModel.onDetailsContinue()
        assertTrue(viewModel.detailsState.loading)
        assertFalse(viewModel.detailsState.continueEnabled)
        advanceUntilIdle()

        assertEquals(listOf("bad"), gateway.passwords)
        assertEquals(invalidPassword, viewModel.detailsState.error)
        assertFalse(viewModel.detailsState.loading)
        assertTrue(viewModel.detailsState.continueEnabled)
        assertFalse(viewModel.detailsState.success)
    }

    @Test
    fun `valid but mismatched passwords expose matching error`() = runTest(dispatcher) {
        val viewModel = arrange()
        fillRequiredFields(viewModel, password = "Valid1!", confirmation = "Different1!")
        advanceUntilIdle()

        viewModel.onDetailsContinue()
        advanceUntilIdle()

        assertEquals(passwordMismatch, viewModel.detailsState.error)
        assertFalse(viewModel.detailsState.success)
    }

    @Test
    fun `valid matching passwords complete successfully`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val viewModel = arrange(gateway = gateway)
        fillRequiredFields(viewModel, password = "Valid1!")
        advanceUntilIdle()

        viewModel.onDetailsContinue()
        advanceUntilIdle()

        assertEquals(listOf("Valid1!"), gateway.passwords)
        assertEquals(CreateAccountDetailsViewState.DetailsError.None, viewModel.detailsState.error)
        assertFalse(viewModel.detailsState.loading)
        assertTrue(viewModel.detailsState.continueEnabled)
        assertTrue(viewModel.detailsState.success)
    }

    private fun arrange(
        customServerConfig: TestLinks? = null,
        requiresTeamName: Boolean = false,
        gateway: FakeGateway = FakeGateway(),
    ): CreateAccountDetailsViewModel<TestLinks, TestFailure> = CreateAccountDetailsViewModel(
        customServerConfig = customServerConfig,
        defaultServerConfig = TestLinks("default"),
        requiresTeamName = requiresTeamName,
        gateway = gateway,
    )

    private fun fillRequiredFields(
        viewModel: CreateAccountDetailsViewModel<TestLinks, TestFailure>,
        password: String = "Valid1!",
        confirmation: String = password,
    ) {
        viewModel.firstNameTextState.setTextAndPlaceCursorAtEnd("Ada")
        viewModel.lastNameTextState.setTextAndPlaceCursorAtEnd("Lovelace")
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)
        viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd(confirmation)
    }

    private data class TestLinks(val name: String)
    private data object TestFailure

    private class FakeGateway(private val isValid: Boolean = true) : CreateAccountDetailsGateway {
        val passwords = mutableListOf<String>()
        override suspend fun isPasswordValid(password: String): Boolean = isValid.also { passwords += password }
    }

    private companion object {
        val invalidPassword = CreateAccountDetailsViewState.DetailsError.TextFieldError.InvalidPasswordError
        val passwordMismatch = CreateAccountDetailsViewState.DetailsError.TextFieldError.PasswordsNotMatchingError
    }
}
