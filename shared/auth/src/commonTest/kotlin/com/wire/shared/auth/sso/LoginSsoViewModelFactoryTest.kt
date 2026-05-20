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
package com.wire.shared.auth.sso

import com.wire.shared.auth.login.model.LoginServerLinks
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LoginSsoViewModelFactoryTest {
    @Test
    fun givenSsoCodeChanged_whenSendingIntent_thenStateIsUpdated() {
        val viewModel = newViewModel(OpenUrlBackend)

        viewModel.sendIntent(LoginSsoIntent.SsoCodeChanged(validSsoCode))

        assertEquals(validSsoCode, viewModel.currentState.ssoCode)
        assertEquals(true, viewModel.currentState.loginEnabled)
        assertEquals(LoginSsoFlowState.Default, viewModel.currentState.flowState)
    }

    @Test
    fun givenInvalidSsoCode_whenSubmitting_thenErrorStateIsShown() {
        val viewModel = newViewModel(OpenUrlBackend)
        viewModel.sendIntent(LoginSsoIntent.SsoCodeChanged("invalid"))

        viewModel.sendIntent(LoginSsoIntent.SubmitLogin)

        assertEquals(LoginSsoFlowState.Error(LoginSsoError.InvalidSsoCode), viewModel.currentState.flowState)
    }

    @Test
    fun givenValidSsoCode_whenSubmitting_thenOpenUrlEffectIsEmitted() = runTest {
        val viewModel = newViewModel(OpenUrlBackend)
        viewModel.sendIntent(LoginSsoIntent.SsoCodeChanged(validSsoCode))
        val effect = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.first()
        }

        viewModel.sendIntent(LoginSsoIntent.SubmitLogin)

        val openUrl = assertIs<LoginSsoEffect.OpenUrl>(effect.await())
        assertEquals("https://accounts.example.com/sso", openUrl.url)
        assertEquals(serverLinks, openUrl.serverLinks)
        assertEquals(LoginSsoFlowState.Default, viewModel.currentState.flowState)
    }

    @Test
    fun givenSsoCallback_whenCompletingLogin_thenSuccessStateIsShown() {
        val viewModel = newViewModel(SuccessBackend)

        viewModel.sendIntent(LoginSsoIntent.CompleteSsoLogin(cookie = "cookie", serverConfigId = "server"))

        assertEquals(
            LoginSsoFlowState.Success(initialSyncCompleted = false, e2eiRequired = false),
            viewModel.currentState.flowState,
        )
    }

    @Test
    fun givenSsoFailureCallback_whenReported_thenErrorStateKeepsCode() {
        val viewModel = newViewModel(SuccessBackend)

        viewModel.sendIntent(LoginSsoIntent.ReportSsoLoginFailure("access-denied"))

        val error = assertIs<LoginSsoFlowState.Error>(viewModel.currentState.flowState)
        assertEquals(LoginSsoError.SsoResultError("access-denied"), error.reason)
    }

    private companion object {
        fun newViewModel(backend: LoginSsoBackend): com.wire.shared.auth.SharedViewModel<LoginSsoState, LoginSsoEffect, LoginSsoIntent> =
            LoginSsoViewModelFactory(backend).create()

        const val validSsoCode = "wire-123e4567-e89b-12d3-a456-426614174000"

        val serverLinks = LoginServerLinks(
            api = "https://api.example.com",
            accounts = "https://accounts.example.com",
            webSocket = "wss://websocket.example.com",
            blackList = "https://blacklist.example.com",
            teams = "https://teams.example.com",
            website = "https://www.example.com",
            title = "Example",
            isOnPremises = false,
        )

        object OpenUrlBackend : LoginSsoBackend {
            override suspend fun initiateLogin(ssoCode: String): LoginSsoBackendResult =
                LoginSsoBackendResult.OpenUrl(
                    url = "https://accounts.example.com/sso",
                    serverLinks = serverLinks,
                )

            override suspend fun completeLogin(
                cookie: String,
                serverConfigId: String,
            ): LoginSsoBackendResult =
                LoginSsoBackendResult.Error(LoginSsoError.GenericError())
        }

        object SuccessBackend : LoginSsoBackend {
            override suspend fun initiateLogin(ssoCode: String): LoginSsoBackendResult =
                LoginSsoBackendResult.Error(LoginSsoError.GenericError())

            override suspend fun completeLogin(
                cookie: String,
                serverConfigId: String,
            ): LoginSsoBackendResult =
                LoginSsoBackendResult.Success(initialSyncCompleted = false, e2eiRequired = false)
        }
    }
}
