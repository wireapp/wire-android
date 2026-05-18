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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class LoginSsoContractTest {

    @Test
    fun givenNoInput_whenStateIsCreated_thenItUsesIdleDefaults() {
        val state = LoginSsoState()

        assertEquals("", state.ssoCode)
        assertFalse(state.loginEnabled)
        assertEquals(LoginSsoFlowState.Default, state.flowState)
        assertNull(state.customServerDialogState)
    }

    @Test
    fun givenOpenUrlEffect_whenCreated_thenItCarriesOnlyPlatformFacingData() {
        val effect = LoginSsoEffect.OpenUrl(
            url = "wire://sso/start",
            serverLinks = serverLinks,
        )

        assertEquals("wire://sso/start", effect.url)
        assertEquals(serverLinks, effect.serverLinks)
    }

    @Test
    fun givenSsoResultFailure_whenReported_thenItKeepsPlatformErrorCode() {
        val intent = LoginSsoIntent.ReportSsoLoginFailure("access-denied")

        assertEquals("access-denied", intent.code)
    }

    @Test
    fun givenLoginError_whenStoredInFlowState_thenItCanBePatternMatched() {
        val state = LoginSsoState(
            flowState = LoginSsoFlowState.Error(LoginSsoError.InvalidSsoCode),
        )

        val errorState = assertIs<LoginSsoFlowState.Error>(state.flowState)
        assertEquals(LoginSsoError.InvalidSsoCode, errorState.reason)
    }

    private companion object {
        val serverLinks = LoginServerLinks(
            api = "https://api.example.com",
            accounts = "https://accounts.example.com",
            webSocket = "wss://websocket.example.com",
            blackList = "https://blacklist.example.com",
            teams = "https://teams.example.com",
            website = "https://example.com",
            title = "Example",
            isOnPremises = false,
        )
    }
}
