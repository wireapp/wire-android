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

import com.wire.ios.shared.WireIosSharedConfig
import com.wire.ios.shared.auth.login.model.LoginApiProxy
import com.wire.ios.shared.auth.login.model.LoginServerLinks
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WelcomeIosViewModelFactoryTest {
    @Test
    fun givenConfig_whenCreatingViewModel_thenStateUsesConfig() {
        val viewModel = WelcomeIosViewModelFactory(
            config = WireIosSharedConfig(
                defaultServerLinks = serverLinks,
                maxAccountsReached = true,
            )
        ).create()

        assertEquals(serverLinks, viewModel.state.value.links)
        assertEquals(true, viewModel.state.value.maxAccountsReached)
    }

    @Test
    fun givenLoginClicked_whenSendingIntent_thenNavigateToLoginEffectIsEmitted() = runTest {
        val viewModel = WelcomeIosViewModelFactory(WireIosSharedConfig(serverLinks)).create()
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        viewModel.sendIntent(WelcomeIntent.LoginClicked)

        assertEquals(WelcomeEffect.NavigateToLogin(serverLinks), effect.await())
    }

    @Test
    fun givenProxyConfigured_whenCreatingPersonalAccount_thenProxyLimitationEffectIsEmitted() = runTest {
        val links = serverLinks.copy(
            apiProxy = LoginApiProxy(
                needsAuthentication = true,
                host = "proxy.example.com",
                port = 8080,
            )
        )
        val viewModel = WelcomeIosViewModelFactory(WireIosSharedConfig(links)).create()
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        viewModel.sendIntent(WelcomeIntent.CreatePersonalAccountClicked)

        assertIs<WelcomeEffect.ShowProxyLimitation>(effect.await())
    }

    private companion object {
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
