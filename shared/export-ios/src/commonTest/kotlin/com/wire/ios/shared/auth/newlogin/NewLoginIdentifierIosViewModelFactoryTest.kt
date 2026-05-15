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
package com.wire.ios.shared.auth.newlogin

import com.wire.ios.shared.WireIosSharedConfig
import com.wire.ios.shared.auth.login.model.LoginServerLinks
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NewLoginIdentifierIosViewModelFactoryTest {
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
    }

    private companion object {
        fun newViewModel(): NewLoginIdentifierIosViewModel =
            NewLoginIdentifierIosViewModelFactory(
                config = WireIosSharedConfig(serverLinks),
                backend = LocalNewLoginIdentifierBackend(),
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
}
