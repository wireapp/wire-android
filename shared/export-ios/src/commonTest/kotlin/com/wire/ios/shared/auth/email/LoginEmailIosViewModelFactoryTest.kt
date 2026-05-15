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
package com.wire.ios.shared.auth.email

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoginEmailIosViewModelFactoryTest {
    @Test
    fun givenCredentials_whenSubmitting_thenLoginSucceededEffectIsEmitted() = runTest {
        val viewModel = LoginEmailIosViewModelFactory(SuccessBackend).create()
        viewModel.sendIntent(LoginEmailIntent.UserIdentifierChanged("user@example.com"))
        viewModel.sendIntent(LoginEmailIntent.PasswordChanged("password"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.first()
        }

        viewModel.sendIntent(LoginEmailIntent.SubmitLogin())

        assertIs<LoginEmailEffect.LoginSucceeded>(effect.await())
        assertEquals(LoginEmailFlowState.Success(initialSyncCompleted = false, isE2EIRequired = false), viewModel.currentState.flowState)
    }

    @Test
    fun givenSecondFactorRequired_whenSubmitting_thenVerificationCodeStateIsShown() = runTest {
        val viewModel = LoginEmailIosViewModelFactory(SecondFactorBackend).create()
        viewModel.sendIntent(LoginEmailIntent.UserIdentifierChanged("user@example.com"))
        viewModel.sendIntent(LoginEmailIntent.PasswordChanged("password"))

        viewModel.sendIntent(LoginEmailIntent.SubmitLogin())

        assertEquals(true, viewModel.currentState.secondFactorVerificationCode.isCodeInputNecessary)
        assertEquals("user@example.com", viewModel.currentState.secondFactorVerificationCode.emailUsed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun givenInvalidCredentials_whenSubmitting_thenErrorStateIsShownWithoutEffect() = runTest {
        val viewModel = LoginEmailIosViewModelFactory(InvalidCredentialsBackend).create()
        val effects = mutableListOf<LoginEmailEffect>()
        val effectCollection = backgroundScope.launch {
            viewModel.effects.toList(effects)
        }
        viewModel.sendIntent(LoginEmailIntent.UserIdentifierChanged("user@example.com"))
        viewModel.sendIntent(LoginEmailIntent.PasswordChanged("wrong-password"))

        viewModel.sendIntent(LoginEmailIntent.SubmitLogin())
        runCurrent()

        assertEquals(LoginEmailFlowState.Error(LoginEmailError.InvalidCredentials), viewModel.currentState.flowState)
        assertEquals(true, viewModel.currentState.loginEnabled)
        assertTrue(effects.isEmpty())
        effectCollection.cancel()
    }

    private object SuccessBackend : LoginEmailBackend {
        override suspend fun login(
            userIdentifier: String,
            password: String,
            secondFactorVerificationCode: String?,
            usernameAllowed: Boolean,
        ): LoginEmailBackendResult =
            LoginEmailBackendResult.Success(initialSyncCompleted = false, isE2EIRequired = false)

        override suspend fun requestSecondFactorCode(userIdentifier: String): LoginEmailBackendResult =
            LoginEmailBackendResult.SecondFactorRequired(userIdentifier)
    }

    private object SecondFactorBackend : LoginEmailBackend {
        override suspend fun login(
            userIdentifier: String,
            password: String,
            secondFactorVerificationCode: String?,
            usernameAllowed: Boolean,
        ): LoginEmailBackendResult =
            LoginEmailBackendResult.SecondFactorRequired(userIdentifier)

        override suspend fun requestSecondFactorCode(userIdentifier: String): LoginEmailBackendResult =
            LoginEmailBackendResult.SecondFactorRequired(userIdentifier)
    }

    private object InvalidCredentialsBackend : LoginEmailBackend {
        override suspend fun login(
            userIdentifier: String,
            password: String,
            secondFactorVerificationCode: String?,
            usernameAllowed: Boolean,
        ): LoginEmailBackendResult =
            LoginEmailBackendResult.Failure(LoginEmailError.InvalidCredentials)

        override suspend fun requestSecondFactorCode(userIdentifier: String): LoginEmailBackendResult =
            LoginEmailBackendResult.SecondFactorRequired(userIdentifier)
    }
}
