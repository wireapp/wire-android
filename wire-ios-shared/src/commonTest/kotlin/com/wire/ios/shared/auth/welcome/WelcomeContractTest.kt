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
package com.wire.ios.shared.auth.welcome

import com.wire.ios.shared.auth.login.model.LoginApiProxy
import com.wire.ios.shared.auth.login.model.LoginServerLinks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WelcomeContractTest {

    @Test
    fun givenServerLinksWithoutProxy_whenCheckingProxy_thenProxyIsDisabled() {
        assertFalse(defaultLinks.isProxyEnabled)
    }

    @Test
    fun givenServerLinksWithProxy_whenCheckingProxy_thenProxyIsEnabled() {
        val links = defaultLinks.copy(
            apiProxy = LoginApiProxy(
                needsAuthentication = true,
                host = "proxy.example.com",
                port = 8080,
            ),
        )

        assertTrue(links.isProxyEnabled)
    }

    @Test
    fun givenWelcomeState_whenCreatedWithDefaults_thenMatchesAndroidWelcomeDefaults() {
        val state = WelcomeState(links = defaultLinks)

        assertEquals(defaultLinks, state.links)
        assertFalse(state.isThereActiveSession)
        assertFalse(state.maxAccountsReached)
        assertFalse(state.nomadAccountBlocksLogin)
        assertTrue(state.isAccountCreationAllowed)
        assertTrue(state.useNewRegistration)
    }

    @Test
    fun givenLoginEffect_whenCreated_thenCarriesServerLinksForNextFlow() {
        val effect = WelcomeEffect.NavigateToLogin(defaultLinks)

        assertEquals(defaultLinks, effect.links)
    }

    private companion object {
        val defaultLinks = LoginServerLinks(
            api = "https://prod-nginz-https.wire.com",
            accounts = "https://account.wire.com",
            webSocket = "https://prod-nginz-ssl.wire.com",
            blackList = "https://clientblacklist.wire.com/prod",
            teams = "https://teams.wire.com",
            website = "https://wire.com",
            title = "production",
            isOnPremises = false,
            apiProxy = null,
        )
    }
}
