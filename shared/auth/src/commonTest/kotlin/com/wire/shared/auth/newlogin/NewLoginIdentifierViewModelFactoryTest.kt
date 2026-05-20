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
package com.wire.shared.auth.newlogin

import com.wire.shared.auth.SharedAuthConfig
import com.wire.shared.auth.login.model.LoginServerLinks
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse

class NewLoginIdentifierViewModelFactoryTest {
    @Test
    fun givenIdentifierChanged_whenSendingIntent_thenStateIsUpdated() {
        val viewModel = newViewModel()

        viewModel.sendIntent(NewLoginIdentifierIntent.UserIdentifierChanged("user@example.com"))

        assertEquals("user@example.com", viewModel.currentState.userIdentifier)
        assertEquals(true, viewModel.currentState.nextEnabled)
    }

    @Test
    fun givenInvalidIdentifier_whenSubmitting_thenTextFieldErrorIsShown() {
        val viewModel = newViewModel()
        viewModel.sendIntent(NewLoginIdentifierIntent.UserIdentifierChanged("invalid"))

        viewModel.sendIntent(NewLoginIdentifierIntent.Submit)

        assertEquals(
            NewLoginIdentifierFlowState.TextFieldError(NewLoginIdentifierTextFieldError.InvalidValue),
            viewModel.currentState.flowState,
        )
    }

    @Test
    fun givenEmailIdentifier_whenSubmitting_thenOpenEmailPasswordEffectIsEmitted() = runTest {
        val viewModel = newViewModel()
        viewModel.sendIntent(NewLoginIdentifierIntent.UserIdentifierChanged("user@example.com"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.first()
        }

        viewModel.sendIntent(NewLoginIdentifierIntent.Submit)

        val openEmailPassword = assertIs<NewLoginIdentifierEffect.OpenEmailPassword>(effect.await())
        assertEquals("user@example.com", openEmailPassword.userIdentifier)
        assertEquals(NewLoginIdentifierFlowState.Default, viewModel.currentState.flowState)
        assertFalse(viewModel.currentState.isLoading)
        assertFalse(viewModel.currentState.hasTextFieldError)
        assertFalse(viewModel.currentState.hasDialogError)
    }

    @Test
    fun givenSsoCodeIdentifier_whenSubmitting_thenOpenSsoEffectIsEmitted() = runTest {
        val ssoCode = "wire-123e4567-e89b-12d3-a456-426614174000"
        val expectedUrl = "https://sso.example.com/login"
        val viewModel = newViewModel(
            backend = FakeNewLoginIdentifierBackend(
                ssoResult = NewLoginIdentifierBackendResult.OpenSso(
                    url = expectedUrl,
                    config = NewLoginSsoUrlConfig(userIdentifier = ssoCode),
                )
            )
        )
        viewModel.sendIntent(NewLoginIdentifierIntent.UserIdentifierChanged(ssoCode))
        val effect = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.first()
        }

        viewModel.sendIntent(NewLoginIdentifierIntent.Submit)

        val openSso = assertIs<NewLoginIdentifierEffect.OpenSSO>(effect.await())
        assertEquals(expectedUrl, openSso.url)
        assertEquals(ssoCode, openSso.config.userIdentifier)
        assertEquals(NewLoginIdentifierFlowState.Default, viewModel.currentState.flowState)
    }

    @Test
    fun givenSsoFailure_whenReceived_thenDialogErrorIsShown() {
        val viewModel = newViewModel()

        viewModel.sendIntent(NewLoginIdentifierIntent.SSOResultReceived(NewLoginSsoResult.Failure(NewLoginSsoFailureCode.InvalidCode)))

        assertEquals(
            NewLoginIdentifierFlowState.DialogError(
                NewLoginIdentifierDialogError.SSOResultFailure(NewLoginSsoFailureCode.InvalidCode)
            ),
            viewModel.currentState.flowState,
        )
        assertEquals(NewLoginIdentifierDialogError.SSOResultFailure(NewLoginSsoFailureCode.InvalidCode), viewModel.currentState.dialogError)
    }

    @Test
    fun givenSsoSuccess_whenReceived_thenLoginSucceededEffectIsEmitted() = runTest {
        val viewModel = newViewModel()
        val effect = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.first()
        }

        viewModel.sendIntent(
            NewLoginIdentifierIntent.SSOResultReceived(
                NewLoginSsoResult.Success(
                    cookie = "cookie",
                    serverConfigId = "server-config-id",
                )
            )
        )

        val loginSucceeded = assertIs<NewLoginIdentifierEffect.LoginSucceeded>(effect.await())
        assertEquals(NewLoginSuccessNextStep.None, loginSucceeded.nextStep)
        assertEquals(NewLoginIdentifierFlowState.Default, viewModel.currentState.flowState)
    }

    private companion object {
        fun newViewModel(
            backend: NewLoginIdentifierBackend = LocalNewLoginIdentifierBackend(),
        ): com.wire.shared.auth.SharedViewModel<NewLoginIdentifierState, NewLoginIdentifierEffect, NewLoginIdentifierIntent> =
            NewLoginIdentifierViewModelFactory(
                config = SharedAuthConfig(serverLinks),
                backend = backend,
            ).create()

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
    }

    private class FakeNewLoginIdentifierBackend(
        private val emailResult: NewLoginIdentifierBackendResult = NewLoginIdentifierBackendResult.OpenEmailPassword(
            userIdentifier = "user@example.com",
            path = NewLoginPasswordPath(),
        ),
        private val ssoResult: NewLoginIdentifierBackendResult = NewLoginIdentifierBackendResult.OpenSso(
            url = "https://sso.example.com/login",
            config = NewLoginSsoUrlConfig(userIdentifier = "wire-123e4567-e89b-12d3-a456-426614174000"),
        ),
    ) : NewLoginIdentifierBackend {
        override suspend fun resolveEmail(userIdentifier: String): NewLoginIdentifierBackendResult = emailResult

        override suspend fun initiateSso(ssoCode: String): NewLoginIdentifierBackendResult = ssoResult
    }
}
