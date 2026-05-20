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
package com.wire.shared.auth.flow

import com.wire.shared.auth.AuthLoginSuccessPayload
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthLoginFlowViewModelFactoryTest {
    @Test
    fun givenEmailFlow_whenPasswordAndSecondFactorAreAccepted_thenSuccessIsShown() = runTest {
        val backend = TwoFactorBackend()
        val viewModel = AuthLoginFlowViewModelFactory(backend).create()

        viewModel.sendIntent(AuthLoginFlowIntent.IdentifierChanged("user@example.com"))
        viewModel.sendIntent(AuthLoginFlowIntent.SubmitIdentifier)
        assertEquals(AuthLoginFlowStep.EmailCredentialsEntry, viewModel.currentState.step)

        viewModel.sendIntent(AuthLoginFlowIntent.PasswordChanged("password"))
        viewModel.sendIntent(AuthLoginFlowIntent.SubmitCredentials())
        assertEquals(AuthLoginFlowStep.SecondFactorEntry, viewModel.currentState.step)
        assertEquals("user@example.com", viewModel.currentState.secondFactorEmail)

        viewModel.sendIntent(AuthLoginFlowIntent.SecondFactorCodeChanged("123456"))
        val successEffect = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.first()
        }
        viewModel.sendIntent(AuthLoginFlowIntent.SubmitSecondFactor())

        val success = assertIs<AuthLoginFlowStep.Success>(viewModel.currentState.step)
        assertEquals(false, success.initialSyncCompleted)
        assertEquals(false, success.isE2EIRequired)
        val loginSucceeded = assertIs<AuthLoginFlowEffect.LoginSucceeded>(successEffect.await())
        assertEquals("user@example.com", loginSucceeded.payload.email)
        assertEquals("password", loginSucceeded.payload.password)
        assertEquals("123456", loginSucceeded.payload.secondFactorCode)
        assertTrue(viewModel.currentState.isSuccess)
        assertEquals(
            listOf(
                LoginCall(
                    identifier = "user@example.com",
                    password = "password",
                    secondFactorCode = null,
                ),
                LoginCall(
                    identifier = "user@example.com",
                    password = "password",
                    secondFactorCode = "123456",
                ),
            ),
            backend.loginCalls,
        )
    }

    @Test
    fun givenSsoCode_whenSubmitting_thenBackendIsDispatchedAndOpenSsoEffectIsEmitted() = runTest {
        val backend = SsoBackend()
        val viewModel = AuthLoginFlowViewModelFactory(backend).create()
        val effect = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.first()
        }

        viewModel.sendIntent(AuthLoginFlowIntent.SsoCodeChanged("wire-123"))
        viewModel.sendIntent(AuthLoginFlowIntent.SubmitSsoCode)

        val openSso = assertIs<AuthLoginFlowEffect.OpenSsoUrl>(effect.await())
        assertEquals("https://sso.example.com/login", openSso.url)
        assertEquals("wire-123", openSso.userIdentifier)
        assertEquals(listOf("wire-123"), backend.ssoCodes)
        assertEquals(AuthLoginFlowStep.IdentifierEntry, viewModel.currentState.step)
    }

    private class TwoFactorBackend : AuthLoginFlowBackend {
        val loginCalls = mutableListOf<LoginCall>()

        override suspend fun resolveIdentifier(identifier: String): AuthLoginFlowIdentifierResult =
            AuthLoginFlowIdentifierResult.EmailCredentialsRequired(identifier)

        override suspend fun initiateSso(ssoCode: String): AuthLoginFlowIdentifierResult =
            error("SSO should not be used by the email path")

        override suspend fun loginWithEmail(
            identifier: String,
            password: String,
            secondFactorCode: String?,
            usernameAllowed: Boolean,
        ): AuthLoginFlowLoginResult {
            loginCalls += LoginCall(identifier, password, secondFactorCode)
            return if (secondFactorCode == null) {
                AuthLoginFlowLoginResult.SecondFactorRequired(email = identifier)
            } else {
                AuthLoginFlowLoginResult.Success(
                    initialSyncCompleted = false,
                    isE2EIRequired = false,
                    payload = AuthLoginSuccessPayload(
                        userIdValue = "user-id",
                        userIdDomain = "wire.com",
                        accessTokenValue = "access-token",
                        accessTokenType = "Bearer",
                        accessTokenExpiresInSeconds = null,
                        refreshTokenValue = "refresh-token",
                        refreshTokenCookieDomain = "wire.com",
                        email = identifier,
                        password = password,
                        secondFactorCode = secondFactorCode,
                        initialSyncCompleted = false,
                        isE2EIRequired = false,
                        clientId = "client-id",
                    ),
                )
            }
        }
    }

    private class SsoBackend : AuthLoginFlowBackend {
        val ssoCodes = mutableListOf<String>()

        override suspend fun resolveIdentifier(identifier: String): AuthLoginFlowIdentifierResult =
            error("Email resolution should not be used by the SSO code path")

        override suspend fun initiateSso(ssoCode: String): AuthLoginFlowIdentifierResult {
            ssoCodes += ssoCode
            return AuthLoginFlowIdentifierResult.OpenSso(
                url = "https://sso.example.com/login",
                userIdentifier = ssoCode,
            )
        }

        override suspend fun loginWithEmail(
            identifier: String,
            password: String,
            secondFactorCode: String?,
            usernameAllowed: Boolean,
        ): AuthLoginFlowLoginResult =
            error("Email login should not be used by the SSO code path")
    }

    private data class LoginCall(
        val identifier: String,
        val password: String,
        val secondFactorCode: String?,
    )
}
