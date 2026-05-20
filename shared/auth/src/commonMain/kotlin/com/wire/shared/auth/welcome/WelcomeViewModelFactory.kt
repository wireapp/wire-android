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
package com.wire.shared.auth.welcome

import com.wire.shared.auth.SharedAuthConfig
import com.wire.shared.auth.SharedViewModel
import com.wire.shared.auth.login.model.LoginServerLinks
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
class WelcomeViewModelFactory(
    private val config: SharedAuthConfig,
) {
    fun create(): SharedViewModel<WelcomeState, WelcomeEffect, WelcomeIntent> {
        val state = MutableStateFlow(
            WelcomeState(
                links = config.defaultServerLinks,
                isThereActiveSession = config.isThereActiveSession,
                maxAccountsReached = config.maxAccountsReached,
                nomadAccountBlocksLogin = config.nomadAccountBlocksLogin,
                isAccountCreationAllowed = config.isAccountCreationAllowed,
                useNewRegistration = config.useNewRegistration,
            )
        )
        val effects = MutableSharedFlow<WelcomeEffect>(extraBufferCapacity = 1)

        return SharedViewModel(
            state = state.asStateFlow(),
            effects = effects.asSharedFlow(),
            onIntent = { intent ->
                when (intent) {
                    WelcomeIntent.LoginClicked ->
                        effects.tryEmit(WelcomeEffect.NavigateToLogin(state.value.links))

                    WelcomeIntent.CreatePersonalAccountClicked ->
                        effects.tryEmit(
                            createAccountEffect(
                                state = state.value,
                                proxyLimitedTarget = WelcomeProxyLimitedTarget.PersonalAccountCreation,
                                navigate = WelcomeEffect::NavigateToCreatePersonalAccount,
                            )
                        )

                    WelcomeIntent.CreateTeamAccountClicked ->
                        effects.tryEmit(
                            createAccountEffect(
                                state = state.value,
                                proxyLimitedTarget = WelcomeProxyLimitedTarget.TeamAccountCreation,
                                navigate = WelcomeEffect::NavigateToCreateTeamAccount,
                            )
                        )

                    WelcomeIntent.ProxyLimitationDismissed ->
                        Unit
                }
            }
        )
    }

    private fun createAccountEffect(
        state: WelcomeState,
        proxyLimitedTarget: WelcomeProxyLimitedTarget,
        navigate: (links: LoginServerLinks) -> WelcomeEffect,
    ): WelcomeEffect =
        if (state.links.isProxyEnabled) {
            WelcomeEffect.ShowProxyLimitation(proxyLimitedTarget)
        } else {
            navigate(state.links)
        }
}
