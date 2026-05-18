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
package com.wire.shared.auth.email

import com.wire.shared.auth.AuthLoginSuccessPayload
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

@OptIn(ExperimentalCoroutinesApi::class)
class LoginEmailViewModelFactoryTest {
    @Test
    fun givenCredentials_whenSubmitting_thenLoginSucceededEffectIsEmitted() = runTest {
        val viewModel = LoginEmailViewModelFactory(SuccessGateway).create()
        viewModel.sendIntent(LoginEmailIntent.UserIdentifierChanged("user@example.com"))
        viewModel.sendIntent(LoginEmailIntent.PasswordChanged("password"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.first()
        }

        viewModel.sendIntent(LoginEmailIntent.SubmitLogin())
        runCurrent()

        assertIs<LoginEmailEffect.LoginSucceeded>(effect.await())
        assertEquals(LoginEmailFlowState.Success(initialSyncCompleted = false, isE2EIRequired = false), viewModel.currentState.flowState)
    }

    @Test
    fun givenSecondFactorRequired_whenSubmitting_thenVerificationCodeStateIsShown() = runTest {
        val viewModel = LoginEmailViewModelFactory(SecondFactorGateway).create()
        viewModel.sendIntent(LoginEmailIntent.UserIdentifierChanged("user@example.com"))
        viewModel.sendIntent(LoginEmailIntent.PasswordChanged("password"))

        viewModel.sendIntent(LoginEmailIntent.SubmitLogin())
        runCurrent()

        assertEquals(true, viewModel.currentState.secondFactorVerificationCode.isCodeInputNecessary)
        assertEquals("user@example.com", viewModel.currentState.secondFactorVerificationCode.emailUsed)
    }

    @Test
    fun givenSecondFactorRequired_whenSubmittingCode_thenLoginSucceededEffectIsEmitted() = runTest {
        SecondFactorThenSuccessGateway.receivedSecondFactorVerificationCode = null
        val viewModel = LoginEmailViewModelFactory(SecondFactorThenSuccessGateway).create()
        viewModel.sendIntent(LoginEmailIntent.UserIdentifierChanged("user@example.com"))
        viewModel.sendIntent(LoginEmailIntent.PasswordChanged("password"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.first()
        }

        viewModel.sendIntent(LoginEmailIntent.SubmitLogin())
        runCurrent()

        assertTrue(viewModel.currentState.isSecondFactorRequired)
        assertEquals("user@example.com", viewModel.currentState.secondFactorEmail)

        viewModel.sendIntent(LoginEmailIntent.SecondFactorCodeChanged("123456"))

        assertEquals("123456", viewModel.currentState.secondFactorCode)
        assertTrue(viewModel.currentState.isSecondFactorCodeComplete)

        viewModel.sendIntent(LoginEmailIntent.SubmitLogin())
        runCurrent()

        assertIs<LoginEmailEffect.LoginSucceeded>(effect.await())
        assertEquals(LoginEmailFlowState.Success(initialSyncCompleted = true, isE2EIRequired = false), viewModel.currentState.flowState)
        assertTrue(viewModel.currentState.isSuccess)
        assertEquals("123456", SecondFactorThenSuccessGateway.receivedSecondFactorVerificationCode)
    }

    @Test
    fun givenInvalidSecondFactorCode_whenSubmitting_thenInvalidSecondFactorStateIsShownWithoutEffect() = runTest {
        val viewModel = LoginEmailViewModelFactory(InvalidSecondFactorGateway).create()
        val effects = mutableListOf<LoginEmailEffect>()
        val effectCollection = backgroundScope.launch {
            viewModel.effects.toList(effects)
        }
        viewModel.sendIntent(LoginEmailIntent.UserIdentifierChanged("user@example.com"))
        viewModel.sendIntent(LoginEmailIntent.PasswordChanged("password"))

        viewModel.sendIntent(LoginEmailIntent.SubmitLogin())
        runCurrent()
        viewModel.sendIntent(LoginEmailIntent.SecondFactorCodeChanged("000000"))
        viewModel.sendIntent(LoginEmailIntent.SubmitLogin())
        runCurrent()

        assertTrue(viewModel.currentState.isSecondFactorRequired)
        assertTrue(viewModel.currentState.isSecondFactorInvalid)
        assertEquals("000000", viewModel.currentState.secondFactorCode)
        assertEquals("user@example.com", viewModel.currentState.secondFactorEmail)
        assertEquals(LoginEmailFlowState.Default, viewModel.currentState.flowState)
        assertTrue(effects.isEmpty())
        effectCollection.cancel()
    }

    @Test
    fun givenInvalidSecondFactorCode_whenCodeChanges_thenInvalidSecondFactorStateIsCleared() = runTest {
        val viewModel = LoginEmailViewModelFactory(InvalidSecondFactorGateway).create()
        viewModel.sendIntent(LoginEmailIntent.UserIdentifierChanged("user@example.com"))
        viewModel.sendIntent(LoginEmailIntent.PasswordChanged("password"))

        viewModel.sendIntent(LoginEmailIntent.SubmitLogin())
        runCurrent()
        viewModel.sendIntent(LoginEmailIntent.SecondFactorCodeChanged("000000"))
        viewModel.sendIntent(LoginEmailIntent.SubmitLogin())
        runCurrent()

        assertTrue(viewModel.currentState.isSecondFactorInvalid)

        viewModel.sendIntent(LoginEmailIntent.SecondFactorCodeChanged("123456"))

        assertEquals("123456", viewModel.currentState.secondFactorCode)
        assertTrue(viewModel.currentState.isSecondFactorCodeComplete)
        assertEquals(false, viewModel.currentState.isSecondFactorInvalid)
    }

    @Test
    fun givenInvalidCredentials_whenSubmitting_thenErrorStateIsShownWithoutEffect() = runTest {
        val viewModel = LoginEmailViewModelFactory(InvalidCredentialsGateway).create()
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

    private object SuccessGateway : LoginEmailGateway {
        override suspend fun login(
            userIdentifier: String,
            password: String,
            secondFactorVerificationCode: String?,
            usernameAllowed: Boolean,
        ): LoginEmailGatewayResult =
            LoginEmailGatewayResult.Success(
                initialSyncCompleted = false,
                isE2EIRequired = false,
                payload = successPayload(
                    userIdentifier = userIdentifier,
                    password = password,
                    secondFactorVerificationCode = secondFactorVerificationCode,
                    initialSyncCompleted = false,
                    isE2EIRequired = false,
                ),
            )

        override suspend fun requestSecondFactorCode(userIdentifier: String): LoginEmailGatewayResult =
            LoginEmailGatewayResult.SecondFactorRequired(userIdentifier)
    }

    private object SecondFactorGateway : LoginEmailGateway {
        override suspend fun login(
            userIdentifier: String,
            password: String,
            secondFactorVerificationCode: String?,
            usernameAllowed: Boolean,
        ): LoginEmailGatewayResult =
            LoginEmailGatewayResult.SecondFactorRequired(userIdentifier)

        override suspend fun requestSecondFactorCode(userIdentifier: String): LoginEmailGatewayResult =
            LoginEmailGatewayResult.SecondFactorRequired(userIdentifier)
    }

    private object SecondFactorThenSuccessGateway : LoginEmailGateway {
        var receivedSecondFactorVerificationCode: String? = null

        override suspend fun login(
            userIdentifier: String,
            password: String,
            secondFactorVerificationCode: String?,
            usernameAllowed: Boolean,
        ): LoginEmailGatewayResult =
            if (secondFactorVerificationCode == null) {
                LoginEmailGatewayResult.SecondFactorRequired(userIdentifier)
            } else {
                receivedSecondFactorVerificationCode = secondFactorVerificationCode
                LoginEmailGatewayResult.Success(
                    initialSyncCompleted = true,
                    isE2EIRequired = false,
                    payload = successPayload(
                        userIdentifier = userIdentifier,
                        password = password,
                        secondFactorVerificationCode = secondFactorVerificationCode,
                        initialSyncCompleted = true,
                        isE2EIRequired = false,
                    ),
                )
            }

        override suspend fun requestSecondFactorCode(userIdentifier: String): LoginEmailGatewayResult =
            LoginEmailGatewayResult.SecondFactorRequired(userIdentifier)
    }

    private object InvalidSecondFactorGateway : LoginEmailGateway {
        override suspend fun login(
            userIdentifier: String,
            password: String,
            secondFactorVerificationCode: String?,
            usernameAllowed: Boolean,
        ): LoginEmailGatewayResult =
            LoginEmailGatewayResult.SecondFactorRequired(
                email = userIdentifier,
                isCurrentCodeInvalid = secondFactorVerificationCode != null,
            )

        override suspend fun requestSecondFactorCode(userIdentifier: String): LoginEmailGatewayResult =
            LoginEmailGatewayResult.SecondFactorRequired(userIdentifier)
    }

    private object InvalidCredentialsGateway : LoginEmailGateway {
        override suspend fun login(
            userIdentifier: String,
            password: String,
            secondFactorVerificationCode: String?,
            usernameAllowed: Boolean,
        ): LoginEmailGatewayResult =
            LoginEmailGatewayResult.Failure(LoginEmailError.InvalidCredentials)

        override suspend fun requestSecondFactorCode(userIdentifier: String): LoginEmailGatewayResult =
            LoginEmailGatewayResult.SecondFactorRequired(userIdentifier)
    }

    private companion object {
        fun successPayload(
            userIdentifier: String,
            password: String,
            secondFactorVerificationCode: String?,
            initialSyncCompleted: Boolean,
            isE2EIRequired: Boolean,
        ): AuthLoginSuccessPayload =
            AuthLoginSuccessPayload(
                userIdValue = "user-id",
                userIdDomain = "wire.com",
                accessTokenValue = "access-token",
                accessTokenType = "Bearer",
                accessTokenExpiresInSeconds = null,
                refreshTokenValue = "refresh-token",
                refreshTokenCookieDomain = "wire.com",
                email = userIdentifier,
                password = password,
                secondFactorCode = secondFactorVerificationCode,
                initialSyncCompleted = initialSyncCompleted,
                isE2EIRequired = isE2EIRequired,
                clientId = "client-id",
            )
    }
}
