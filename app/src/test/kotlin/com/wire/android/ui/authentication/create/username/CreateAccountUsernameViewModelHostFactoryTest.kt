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

import com.wire.android.analytics.FinalizeRegistrationAnalyticsMetadataUseCase
import com.wire.android.analytics.RegistrationAnalyticsManagerUseCase
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.mockUri
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.ui.authentication.create.common.handle.HandleUpdateErrorState
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.feature.auth.ValidateUserHandleResult
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleResult
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class CreateAccountUsernameViewModelHostFactoryTest {

    @BeforeEach
    fun setUp() {
        mockUri()
    }

    @Test
    fun `gateway maps valid and every set-handle result preserving failure identity`() = runTest {
        val validate = mockk<ValidateUserHandleUseCase>()
        val set = mockk<SetUserHandleUseCase>()
        val gateway = KaliumCreateAccountUsernameGateway(validate, set)
        val failure = NetworkFailure.NoNetworkConnection(null)
        every { validate("valid") } returns ValidateUserHandleResult.Valid("valid")
        every { validate("x") } returns ValidateUserHandleResult.Invalid.TooShort("x")

        assertEquals(UsernameValidation.Valid, gateway.validateUsername("valid"))
        assertEquals(UsernameValidation.Invalid, gateway.validateUsername("x"))

        listOf(
            SetUserHandleResult.Success to SetUsernameResult.Success,
            SetUserHandleResult.Failure.HandleExists to SetUsernameResult.UsernameTaken,
            SetUserHandleResult.Failure.InvalidHandle to SetUsernameResult.UsernameInvalid,
            SetUserHandleResult.Failure.Generic(failure) to SetUsernameResult.Failure(failure),
        ).forEach { (kaliumResult, expected) ->
            coEvery { set("alice") } returns kaliumResult
            val actual = gateway.setUsername("alice")
            assertEquals(expected, actual)
            if (actual is SetUsernameResult.Failure) assertSame(failure, actual.failure)
        }

        verify(exactly = 1) { validate("valid") }
        verify(exactly = 1) { validate("x") }
        coVerify(exactly = 4) { set("alice") }
    }

    @Test
    fun `analytics maps screen event and completes event before metadata finalization`() = runTest {
        val manager = mockk<RegistrationAnalyticsManagerUseCase>()
        val finalize = mockk<FinalizeRegistrationAnalyticsMetadataUseCase>()
        coEvery { manager.sendEventIfEnabled(any()) } returns Unit
        coEvery { finalize() } returns Unit
        val analytics = AppCreateAccountUsernameAnalytics(manager, finalize)

        analytics.usernameScreenShown()
        analytics.accountCreationCompleted()

        coVerify(exactly = 1) {
            manager.sendEventIfEnabled(AnalyticsEvent.RegistrationPersonalAccount.Username)
        }
        coVerifyOrder {
            manager.sendEventIfEnabled(AnalyticsEvent.RegistrationPersonalAccount.CreationCompleted)
            finalize()
        }
    }

    @Test
    fun `host factory composes feature view model and reports username screen`() = runTest {
        val validate = mockk<ValidateUserHandleUseCase>()
        val set = mockk<SetUserHandleUseCase>()
        val manager = mockk<RegistrationAnalyticsManagerUseCase>()
        val finalize = mockk<FinalizeRegistrationAnalyticsMetadataUseCase>()
        coEvery { manager.sendEventIfEnabled(any()) } returns Unit
        val viewModel = CreateAccountUsernameViewModelHostFactory(validate, set, manager, finalize).create()

        advanceUntilIdle()

        assertFalse(viewModel.state.success)
        coVerify(exactly = 1) {
            manager.sendEventIfEnabled(AnalyticsEvent.RegistrationPersonalAccount.Username)
        }
    }

    @Test
    fun `screen mapper covers every feature error and preserves generic failure identity`() {
        val failure = NetworkFailure.NoNetworkConnection(null)

        assertSame(HandleUpdateErrorState.None, CreateAccountUsernameError.None.toHandleUpdateErrorState())
        assertSame(
            HandleUpdateErrorState.TextFieldError.UsernameInvalidError,
            CreateAccountUsernameError.UsernameInvalid.toHandleUpdateErrorState(),
        )
        assertSame(
            HandleUpdateErrorState.TextFieldError.UsernameTakenError,
            CreateAccountUsernameError.UsernameTaken.toHandleUpdateErrorState(),
        )
        val mapped = CreateAccountUsernameError.Generic(failure).toHandleUpdateErrorState()
        assertTrue(mapped is HandleUpdateErrorState.DialogError.GenericError)
        assertSame(failure, (mapped as HandleUpdateErrorState.DialogError.GenericError).coreFailure)
    }
}
